/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2025 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.OrganizationRepresentation;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.properties.ImportConfigProperties;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.service.normalize.OrganizationNormalizationService;
import de.adorsys.keycloak.config.util.CloneUtil;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static de.adorsys.keycloak.config.properties.ImportConfigProperties.ImportManagedProperties.ImportManagedPropertiesValues;

/**
 * Service responsible for importing Keycloak Organizations.
 * Handles creation, update, and deletion of organizations based on managed mode.
 * Organizations must be imported after identity providers due to dependencies.
 */
@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "IMPORT", matchIfMissing = true)
public class OrganizationImportService {
    private static final Logger logger = LoggerFactory.getLogger(OrganizationImportService.class);

    private final OrganizationRepository organizationRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final ImportConfigProperties importConfigProperties;
    private final OrganizationNormalizationService organizationNormalizationService;

    @Autowired
    public OrganizationImportService(
            OrganizationRepository organizationRepository,
            IdentityProviderRepository identityProviderRepository,
            ImportConfigProperties importConfigProperties,
            OrganizationNormalizationService organizationNormalizationService
    ) {
        this.organizationRepository = organizationRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.importConfigProperties = importConfigProperties;
        this.organizationNormalizationService = organizationNormalizationService;
    }

    public void doImport(RealmImport realmImport) {
        List<OrganizationRepresentation> organizations = realmImport.getOrganizationList();
        if (organizations == null) {
            return;
        }

        String realmName = realmImport.getRealm();
        
        // Normalize organizations before processing
        organizations = organizationNormalizationService.normalizeOrganizations(organizations);
        
        // First phase: Create/update organizations (and delete if managed mode is FULL)
        manageOrganizations(realmName, organizations);
        
        // Second phase: Link identity providers
        // This is done separately to ensure all organizations exist before linking
        for (OrganizationRepresentation organization : organizations) {
            updateIdentityProviderLinks(realmName, organization);
        }
    }

    private void manageOrganizations(String realmName, List<OrganizationRepresentation> organizations) {
        List<OrganizationRepresentation> existingOrganizations = organizationRepository.getAll(realmName);

        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            deleteOrganizationsMissingInImport(realmName, organizations, existingOrganizations);
        }

        for (OrganizationRepresentation organization : organizations) {
            createOrUpdateOrganization(realmName, organization);
        }
    }

    private void deleteOrganizationsMissingInImport(
            String realmName,
            List<OrganizationRepresentation> importedOrganizations,
            List<OrganizationRepresentation> existingOrganizations
    ) {
        Set<String> importedAliases = importedOrganizations.stream()
                .map(OrganizationRepresentation::getAlias)
                .collect(Collectors.toSet());

        for (OrganizationRepresentation existingOrg : existingOrganizations) {
            if (!importedAliases.contains(existingOrg.getAlias())) {
                logger.debug("Delete organization '{}' in realm '{}'", existingOrg.getAlias(), realmName);
                organizationRepository.delete(realmName, existingOrg.getId());
            }
        }
    }

    private void createOrUpdateOrganization(String realmName, OrganizationRepresentation organization) {
        String organizationAlias = organization.getAlias();
        
        if (!StringUtils.hasText(organizationAlias)) {
            throw new ImportProcessingException("Organization alias cannot be null or empty");
        }
        
        Optional<OrganizationRepresentation> maybeOrganization = organizationRepository.getByAlias(realmName, organizationAlias);

        if (maybeOrganization.isPresent()) {
            updateOrganizationIfNeeded(realmName, organization, maybeOrganization.get());
        } else {
            logger.debug("Create organization '{}' in realm '{}'", organizationAlias, realmName);
            
            // Don't send identity providers during creation
            OrganizationRepresentation orgToCreate = CloneUtil.deepClone(organization);
            orgToCreate.setIdentityProviders(null);
            
            organizationRepository.create(realmName, orgToCreate);
            
            // Update the original organization with the generated ID
            organization.setId(orgToCreate.getId());
        }
    }

    private void updateOrganizationIfNeeded(String realmName, OrganizationRepresentation importedOrg, 
                                           OrganizationRepresentation existingOrg) {
        String organizationAlias = importedOrg.getAlias();
        
        // Set the ID from existing organization
        importedOrg.setId(existingOrg.getId());
        
        // Create a copy without identity providers for comparison
        OrganizationRepresentation importedWithoutIdps = CloneUtil.deepClone(importedOrg);
        importedWithoutIdps.setIdentityProviders(null);
        
        OrganizationRepresentation existingWithoutIdps = CloneUtil.deepClone(existingOrg);
        existingWithoutIdps.setIdentityProviders(null);
        
        if (!CloneUtil.deepEquals(importedWithoutIdps, existingWithoutIdps)) {
            logger.debug("Update organization '{}' in realm '{}'", organizationAlias, realmName);
            organizationRepository.update(realmName, importedWithoutIdps);
        } else {
            logger.debug("No need to update organization '{}' in realm '{}'", organizationAlias, realmName);
        }
    }

    private void updateIdentityProviderLinks(String realmName, OrganizationRepresentation organization) {
        if (organization.getId() == null) {
            // If we don't have an ID, try to get it from the repository
            Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(realmName, organization.getAlias());
            if (maybeOrg.isPresent()) {
                organization.setId(maybeOrg.get().getId());
            } else {
                logger.warn("Cannot update identity provider links for organization '{}': Organization not found", 
                            organization.getAlias());
                return;
            }
        }

        String organizationId = organization.getId();
        List<String> desiredIdpAliases = organization.getIdentityProviders();
        
        if (desiredIdpAliases == null) {
            desiredIdpAliases = Collections.emptyList();
        }
        
        // Get current linked IdPs
        List<IdentityProviderRepresentation> currentIdps = organizationRepository.getLinkedIdentityProviders(realmName, organizationId);
        Set<String> currentIdpAliases = currentIdps.stream()
                .map(IdentityProviderRepresentation::getAlias)
                .collect(Collectors.toSet());
        
        Set<String> desiredIdpAliasesSet = new HashSet<>(desiredIdpAliases);
        
        // Link new IdPs
        for (String idpAlias : desiredIdpAliases) {
            if (!currentIdpAliases.contains(idpAlias)) {
                validateIdentityProviderExists(realmName, idpAlias);
                logger.debug("Link identity provider '{}' to organization '{}' in realm '{}'", 
                            idpAlias, organization.getAlias(), realmName);
                organizationRepository.linkIdentityProvider(realmName, organizationId, idpAlias);
            }
        }
        
        // Unlink removed IdPs (only in FULL managed mode)
        if (importConfigProperties.getManaged().getOrganization() == ImportManagedPropertiesValues.FULL) {
            for (String currentIdpAlias : currentIdpAliases) {
                if (!desiredIdpAliasesSet.contains(currentIdpAlias)) {
                    logger.debug("Unlink identity provider '{}' from organization '{}' in realm '{}'", 
                                currentIdpAlias, organization.getAlias(), realmName);
                    organizationRepository.unlinkIdentityProvider(realmName, organizationId, currentIdpAlias);
                }
            }
        }
    }

    private void validateIdentityProviderExists(String realmName, String idpAlias) {
        Optional<IdentityProviderRepresentation> maybeIdp = identityProviderRepository.search(realmName, idpAlias);
        if (maybeIdp.isEmpty()) {
            throw new ImportProcessingException(
                    String.format("Identity provider '%s' does not exist in realm '%s'",
                            idpAlias, realmName));
        }
    }
}