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

package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

/**
 * Represents a Keycloak Organization entity for multi-tenancy support.
 * Organizations group users and identity providers under a common domain.
 * Available in Keycloak 26+.
 */
public class OrganizationRepresentation {
    private String id;
    private String name;
    private String alias;
    private Boolean enabled;
    private String description;
    private String redirectUrl;
    private Map<String, List<String>> attributes;
    private Set<OrganizationDomainRepresentation> domains;
    private List<OrganizationIdentityProviderConfig> identityProviders;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public void singleAttribute(String name, String value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(name, Collections.singletonList(value));
    }

    public Set<OrganizationDomainRepresentation> getDomains() {
        return domains;
    }

    public void setDomains(Set<OrganizationDomainRepresentation> domains) {
        this.domains = domains;
    }

    public OrganizationDomainRepresentation getDomain(String name) {
        if (domains == null || name == null) {
            return null;
        }
        return domains.stream()
                .filter(domain -> domain != null && name.equals(domain.getName()))
                .findFirst()
                .orElse(null);
    }

    public void addDomain(OrganizationDomainRepresentation domain) {
        if (domain == null) {
            return;
        }
        if (domains == null) {
            domains = new HashSet<>();
        }
        domains.add(domain);
    }

    public void removeDomain(OrganizationDomainRepresentation domain) {
        if (domains != null) {
            domains.remove(domain);
        }
    }

    public List<OrganizationIdentityProviderConfig> getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(List<OrganizationIdentityProviderConfig> identityProviders) {
        this.identityProviders = identityProviders;
    }
    
    @JsonIgnore
    public List<String> getIdentityProviderAliases() {
        if (identityProviders == null) {
            return null;
        }
        return identityProviders.stream()
                .map(OrganizationIdentityProviderConfig::getAlias)
                .toList();
    }

    public void addIdentityProvider(OrganizationIdentityProviderConfig identityProviderConfig) {
        if (identityProviders == null) {
            identityProviders = new ArrayList<>();
        }
        // Check if already exists based on alias
        boolean exists = identityProviders.stream()
                .anyMatch(config -> Objects.equals(config.getAlias(), identityProviderConfig.getAlias()));
        if (!exists) {
            identityProviders.add(identityProviderConfig);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationRepresentation that = (OrganizationRepresentation) o;
        // Use ID if available, otherwise fall back to alias
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        // Use ID if available, otherwise fall back to alias
        return Objects.hash(id != null ? id : alias);
    }
}