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

package de.adorsys.keycloak.config.repository;

import de.adorsys.keycloak.config.exception.KeycloakRepositoryException;
import de.adorsys.keycloak.config.model.OrganizationIdentityProviderConfig;
import de.adorsys.keycloak.config.model.OrganizationRepresentation;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.resource.OrganizationApi;
import de.adorsys.keycloak.config.resource.OrganizationIdentityProviderApi;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

@Service
@ConditionalOnBean(KeycloakProvider.class)
public class OrganizationRepository {
    private static final Logger logger = LoggerFactory.getLogger(OrganizationRepository.class);
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NO_CONTENT = 204;
    private static final String KC_ORG_DOMAIN = "kc.org.domain";
    private static final String KC_ORG_REDIRECT_EMAIL_MATCHES = "kc.org.broker.redirect.mode.email-matches";

    private final KeycloakProvider keycloakProvider;
    private final IdentityProviderRepository identityProviderRepository;

    @Autowired
    public OrganizationRepository(KeycloakProvider keycloakProvider, IdentityProviderRepository identityProviderRepository) {
        this.keycloakProvider = keycloakProvider;
        this.identityProviderRepository = identityProviderRepository;
    }

    public void create(String realm, OrganizationRepresentation organization) {
        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("Realm cannot be null or empty");
        }
        if (organization == null) {
            throw new IllegalArgumentException("Organization cannot be null");
        }
        if (!StringUtils.hasText(organization.getAlias())) {
            throw new IllegalArgumentException("Organization alias cannot be null or empty");
        }
        
        logger.debug("Creating organization '{}' in realm '{}'", organization.getAlias(), realm);
        OrganizationApi api = keycloakProvider.getCustomApiProxy(OrganizationApi.class);
        
        try (Response response = api.create(realm, organization)) {
            if (response.getStatus() != HTTP_CREATED) {
                throw new KeycloakRepositoryException(
                        String.format("Failed to create organization '%s' in realm '%s'. Status: %d. "
                                + "Ensure the realm has organizationsEnabled=true and the organization alias is unique.",
                                organization.getAlias(), realm, response.getStatus()));
            }
            
            if (response.getLocation() != null) {
                String location = response.getLocation().toString();
                String id = location.substring(location.lastIndexOf('/') + 1);
                organization.setId(id);
            }
        }
    }

    public void update(String realm, OrganizationRepresentation organization) {
        if (!StringUtils.hasText(realm)) {
            throw new IllegalArgumentException("Realm cannot be null or empty");
        }
        if (organization == null) {
            throw new IllegalArgumentException("Organization cannot be null");
        }
        if (!StringUtils.hasText(organization.getId())) {
            throw new IllegalArgumentException("Organization ID cannot be null or empty");
        }
        
        OrganizationApi api = keycloakProvider.getCustomApiProxy(OrganizationApi.class);
        
        try {
            api.update(realm, organization.getId(), organization);
        } catch (NotFoundException e) {
            throw new KeycloakRepositoryException(
                    String.format("Cannot update organization '%s' in realm '%s': Not found",
                            organization.getAlias(), realm), e);
        }
    }

    public void delete(String realm, String organizationId) {
        OrganizationApi api = keycloakProvider.getCustomApiProxy(OrganizationApi.class);
        
        try {
            api.delete(realm, organizationId);
        } catch (NotFoundException e) {
            throw new KeycloakRepositoryException(
                    String.format("Cannot delete organization with id '%s' in realm '%s': Not found",
                            organizationId, realm), e);
        }
    }

    public OrganizationRepresentation get(String realm, String organizationId) {
        OrganizationApi api = keycloakProvider.getCustomApiProxy(OrganizationApi.class);
        
        try {
            return api.get(realm, organizationId);
        } catch (NotFoundException e) {
            throw new KeycloakRepositoryException(
                    String.format("Cannot get organization with id '%s' in realm '%s': Not found",
                            organizationId, realm), e);
        }
    }

    public List<OrganizationRepresentation> getAll(String realm) {
        OrganizationApi api = keycloakProvider.getCustomApiProxy(OrganizationApi.class);
        List<OrganizationRepresentation> organizations = api.search(realm, null, null, null, null, null, false);
        return organizations != null ? organizations : new ArrayList<>();
    }

    public Optional<OrganizationRepresentation> search(String realm, String searchValue) {
        OrganizationApi api = keycloakProvider.getCustomApiProxy(OrganizationApi.class);
        
        // First try exact match
        List<OrganizationRepresentation> exactResults = api.search(realm, searchValue, null, true, null, null, false);
        if (!exactResults.isEmpty()) {
            return Optional.of(exactResults.get(0));
        }
        
        // If no exact match, try partial match
        List<OrganizationRepresentation> partialResults = api.search(realm, searchValue, null, false, null, null, false);
        return partialResults.stream()
                .filter(org -> searchValue.equals(org.getName()) || searchValue.equals(org.getAlias()))
                .findFirst();
    }

    public Optional<OrganizationRepresentation> getByAlias(String realm, String alias) {
        return search(realm, alias);
    }

    // Identity Provider management methods

    public void updateIdentityProviderConfiguration(String realm, OrganizationIdentityProviderConfig config) {
        // Get the current identity provider
        Optional<IdentityProviderRepresentation> maybeIdp = identityProviderRepository.search(realm, config.getAlias());
        if (maybeIdp.isEmpty()) {
            throw new KeycloakRepositoryException(
                    String.format("Identity provider '%s' not found in realm '%s'", config.getAlias(), realm));
        }
        
        IdentityProviderRepresentation idp = maybeIdp.get();
        
        // Update the identity provider configuration with organization-specific settings
        if (idp.getConfig() == null) {
            idp.setConfig(new HashMap<>());
        }
        
        // Update organization-specific configuration
        // Set or remove domain
        if (config.getDomain() != null) {
            idp.getConfig().put(KC_ORG_DOMAIN, config.getDomain());
        } else {
            idp.getConfig().remove(KC_ORG_DOMAIN);
        }
        
        // Set or remove redirect email matches
        if (config.getRedirectWhenEmailMatches() != null) {
            idp.getConfig().put(KC_ORG_REDIRECT_EMAIL_MATCHES, config.getRedirectWhenEmailMatches().toString());
        } else {
            idp.getConfig().remove(KC_ORG_REDIRECT_EMAIL_MATCHES);
        }
        
        // Update hide on login
        if (config.getHideOnLogin() != null) {
            // Store in config map for compatibility with older Keycloak versions
            idp.getConfig().put("hideOnLoginPage", config.getHideOnLogin().toString());
        } else {
            idp.getConfig().remove("hideOnLoginPage");
        }
        
        // Update the identity provider with the new configuration
        identityProviderRepository.update(realm, idp);
    }

    public void linkIdentityProvider(String realm, String organizationId, OrganizationIdentityProviderConfig config) {
        OrganizationIdentityProviderApi api = keycloakProvider.getCustomApiProxy(OrganizationIdentityProviderApi.class);
        
        // Update the identity provider configuration
        updateIdentityProviderConfiguration(realm, config);
        
        // Now link it to the organization
        try (Response response = api.addIdentityProvider(realm, organizationId, config.getAlias())) {
            // API may return either 201 (created) or 204 (no content) on success
            if (response.getStatus() != HTTP_NO_CONTENT && response.getStatus() != HTTP_CREATED) {
                throw new KeycloakRepositoryException(
                        String.format("Failed to link identity provider '%s' to organization '%s' in realm '%s'. Status: %d",
                                config.getAlias(), organizationId, realm, response.getStatus()));
            }
        } catch (ClientErrorException e) {
            throw new KeycloakRepositoryException(
                    String.format("Cannot link identity provider '%s' to organization '%s' in realm '%s': %s",
                            config.getAlias(), organizationId, realm, e.getMessage()), e);
        }
    }

    public void unlinkIdentityProvider(String realm, String organizationId, String identityProviderAlias) {
        OrganizationIdentityProviderApi api = keycloakProvider.getCustomApiProxy(OrganizationIdentityProviderApi.class);
        
        try {
            api.removeIdentityProvider(realm, organizationId, identityProviderAlias);
        } catch (NotFoundException e) {
            // Ignore if already unlinked - this is expected behavior when the identity provider
            // is not linked to the organization or has been previously removed
            logger.debug("Identity provider '{}' was not linked to organization '{}' in realm '{}' - ignoring unlink request", 
                    identityProviderAlias, organizationId, realm);
        } catch (ClientErrorException e) {
            throw new KeycloakRepositoryException(
                    String.format("Cannot unlink identity provider '%s' from organization '%s' in realm '%s': %s",
                            identityProviderAlias, organizationId, realm, e.getMessage()), e);
        }
    }

    public List<IdentityProviderRepresentation> getLinkedIdentityProviders(String realm, String organizationId) {
        OrganizationIdentityProviderApi api = keycloakProvider.getCustomApiProxy(OrganizationIdentityProviderApi.class);
        
        try {
            List<IdentityProviderRepresentation> providers = api.getIdentityProviders(realm, organizationId);
            return providers != null ? providers : new ArrayList<>();
        } catch (NotFoundException e) {
            throw new KeycloakRepositoryException(
                    String.format("Cannot get identity providers for organization '%s' in realm '%s': Organization not found",
                            organizationId, realm), e);
        }
    }
}