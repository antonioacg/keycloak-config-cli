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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Configuration for linking an identity provider to an organization.
 * Supports domain-based email matching and visibility settings.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationIdentityProviderConfig {
    
    @JsonProperty("alias")
    private String alias;
    
    @JsonProperty("domain")
    private String domain;
    
    @JsonProperty("redirectWhenEmailMatches")
    private Boolean redirectWhenEmailMatches;
    
    @JsonProperty("hideOnLogin")
    private Boolean hideOnLogin;
    
    public OrganizationIdentityProviderConfig() {
    }
    
    public OrganizationIdentityProviderConfig(String alias) {
        this.alias = alias;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    public Boolean getRedirectWhenEmailMatches() {
        return redirectWhenEmailMatches;
    }
    
    public void setRedirectWhenEmailMatches(Boolean redirectWhenEmailMatches) {
        this.redirectWhenEmailMatches = redirectWhenEmailMatches;
    }
    
    public Boolean getHideOnLogin() {
        return hideOnLogin;
    }
    
    public void setHideOnLogin(Boolean hideOnLogin) {
        this.hideOnLogin = hideOnLogin;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationIdentityProviderConfig that = (OrganizationIdentityProviderConfig) o;
        return Objects.equals(alias, that.alias);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(alias);
    }
    
    @Override
    public String toString() {
        return "OrganizationIdentityProviderConfig{"
                + "alias='" + alias + '\''
                + ", domain='" + domain + '\''
                + ", redirectWhenEmailMatches=" + redirectWhenEmailMatches
                + ", hideOnLogin=" + hideOnLogin
                + '}';
    }
}