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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class OrganizationIdentityProviderConfigTest {

    @Test
    void shouldCreateConfigWithAlias() {
        OrganizationIdentityProviderConfig config = new OrganizationIdentityProviderConfig("test-alias");
        
        assertThat(config.getAlias(), equalTo("test-alias"));
        assertThat(config.getDomain(), nullValue());
        assertThat(config.getRedirectWhenEmailMatches(), nullValue());
        assertThat(config.getHideOnLogin(), nullValue());
    }

    @Test
    void shouldSetAndGetAllProperties() {
        OrganizationIdentityProviderConfig config = new OrganizationIdentityProviderConfig();
        config.setAlias("corp-saml");
        config.setDomain("corp.example.com");
        config.setRedirectWhenEmailMatches(true);
        config.setHideOnLogin(false);
        
        assertThat(config.getAlias(), equalTo("corp-saml"));
        assertThat(config.getDomain(), equalTo("corp.example.com"));
        assertThat(config.getRedirectWhenEmailMatches(), equalTo(true));
        assertThat(config.getHideOnLogin(), equalTo(false));
    }

    @Test
    void shouldBeEqualBasedOnAlias() {
        OrganizationIdentityProviderConfig config1 = new OrganizationIdentityProviderConfig("test-alias");
        config1.setDomain("domain1.com");
        
        OrganizationIdentityProviderConfig config2 = new OrganizationIdentityProviderConfig("test-alias");
        config2.setDomain("domain2.com");
        
        OrganizationIdentityProviderConfig config3 = new OrganizationIdentityProviderConfig("different-alias");
        
        assertThat(config1, equalTo(config2));
        assertThat(config1, not(equalTo(config3)));
        assertThat(config1.hashCode(), equalTo(config2.hashCode()));
        assertThat(config1.hashCode(), not(equalTo(config3.hashCode())));
    }

    @Test
    void shouldHandleNullsInEquals() {
        OrganizationIdentityProviderConfig config = new OrganizationIdentityProviderConfig("test");
        
        assertThat(config.equals(null), is(false));
        assertThat(config.equals(config), is(true));
        assertThat(config.equals(new Object()), is(false));
    }

    @Test
    void shouldGenerateReadableToString() {
        OrganizationIdentityProviderConfig config = new OrganizationIdentityProviderConfig("corp-saml");
        config.setDomain("corp.example.com");
        config.setRedirectWhenEmailMatches(true);
        config.setHideOnLogin(false);
        
        String toString = config.toString();
        assertThat(toString, containsString("corp-saml"));
        assertThat(toString, containsString("corp.example.com"));
        assertThat(toString, containsString("true"));
        assertThat(toString, containsString("false"));
    }
}