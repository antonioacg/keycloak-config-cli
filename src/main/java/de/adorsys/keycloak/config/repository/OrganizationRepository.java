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
import de.adorsys.keycloak.config.model.OrganizationRepresentation;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import de.adorsys.keycloak.config.resource.OrganizationApi;
import de.adorsys.keycloak.config.resource.OrganizationIdentityProviderApi;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrganizationRepository {
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NO_CONTENT = 204;

    private final KeycloakProvider keycloakProvider;

    @Autowired
    public OrganizationRepository(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }

    public void create(String realm, OrganizationRepresentation organization) {
        OrganizationApi api = keycloakProvider.getCustomApiProxy(OrganizationApi.class);
        
        try (Response response = api.create(realm, organization)) {
            if (response.getStatus() != HTTP_CREATED) {
                throw new KeycloakRepositoryException(
                        String.format("Failed to create organization '%s' in realm '%s'. Status: %d",
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

    public void linkIdentityProvider(String realm, String organizationId, String identityProviderAlias) {
        OrganizationIdentityProviderApi api = keycloakProvider.getCustomApiProxy(OrganizationIdentityProviderApi.class);
        
        // Create a minimal representation with just the alias
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(identityProviderAlias);
        
        try (Response response = api.addIdentityProvider(realm, organizationId, idp)) {
            // API may return either 201 (created) or 204 (no content) on success
            if (response.getStatus() != HTTP_NO_CONTENT && response.getStatus() != HTTP_CREATED) {
                throw new KeycloakRepositoryException(
                        String.format("Failed to link identity provider '%s' to organization '%s' in realm '%s'. Status: %d",
                                identityProviderAlias, organizationId, realm, response.getStatus()));
            }
        } catch (ClientErrorException e) {
            throw new KeycloakRepositoryException(
                    String.format("Cannot link identity provider '%s' to organization '%s' in realm '%s': %s",
                            identityProviderAlias, organizationId, realm, e.getMessage()), e);
        }
    }

    public void unlinkIdentityProvider(String realm, String organizationId, String identityProviderAlias) {
        OrganizationIdentityProviderApi api = keycloakProvider.getCustomApiProxy(OrganizationIdentityProviderApi.class);
        
        try {
            api.removeIdentityProvider(realm, organizationId, identityProviderAlias);
        } catch (NotFoundException e) {
            // Ignore if already unlinked
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