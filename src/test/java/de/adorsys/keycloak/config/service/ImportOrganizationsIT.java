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

import de.adorsys.keycloak.config.AbstractImportIT;
import de.adorsys.keycloak.config.exception.ImportProcessingException;
import de.adorsys.keycloak.config.model.OrganizationIdentityProviderConfig;
import de.adorsys.keycloak.config.model.OrganizationRepresentation;
import de.adorsys.keycloak.config.repository.IdentityProviderRepository;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for Organization import functionality.
 * Note: Organizations are only available in Keycloak 26+
 */
@DisabledIf("isKeycloakVersionLessThan26")
@SuppressWarnings({"java:S5961", "java:S5976"})
class ImportOrganizationsIT extends AbstractImportIT {
    private static final String REALM_NAME = "realmWithOrganizations";

    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    ImportOrganizationsIT() {
        this.resourcePath = "import-files/organizations";
    }

    private static boolean isKeycloakVersionLessThan26() {
        return VersionUtil.lt(KEYCLOAK_VERSION, "26");
    }

    @Test
    @Order(0)
    void shouldCreateRealmWithOrganization() throws IOException {
        doImport("00_create_realm_with_organization.json");

        List<OrganizationRepresentation> organizations = organizationRepository.getAll(REALM_NAME);
        assertThat(organizations, hasSize(1));

        OrganizationRepresentation org = organizations.get(0);
        assertThat(org.getName(), is("Test Organization"));
        assertThat(org.getAlias(), is("test-org"));
        assertThat(org.getEnabled(), is(true));
        assertThat(org.getDescription(), is("A test organization"));
        assertThat(org.getRedirectUrl(), is("https://test-org.example.com"));

        // Check domains
        assertThat(org.getDomains(), hasSize(2));
        assertThat(org.getDomain("test-org.com"), notNullValue());
        assertThat(org.getDomain("test-org.com").isVerified(), is(Boolean.TRUE));
        assertThat(org.getDomain("test-org.org"), notNullValue());
        assertThat(org.getDomain("test-org.org").isVerified(), is(Boolean.FALSE));

        // Check attributes
        assertThat(org.getAttributes(), notNullValue());
        assertThat(org.getAttributes().get("contactEmail"), contains("admin@test-org.com"));
        assertThat(org.getAttributes().get("contactPhone"), containsInAnyOrder("+1234567890", "+0987654321"));
    }

    @Test
    @Order(1)
    void shouldUpdateOrganization() throws IOException {
        // Create a JSON that updates the organization
        doImport("01_update_organization_properties.json");

        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));

        OrganizationRepresentation org = maybeOrg.get();
        assertThat(org.getName(), is("Updated Test Organization"));
        assertThat(org.getDescription(), is("An updated test organization"));
        assertThat(org.getRedirectUrl(), is("https://updated.test-org.com"));
    }

    @Test
    @Order(2)
    void shouldAddDomainToOrganization() throws IOException {
        doImport("02_update_organization_add_domain.json");

        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));

        OrganizationRepresentation org = maybeOrg.get();
        assertThat(org.getDomains(), hasSize(3));
        assertThat(org.getDomain("test-org.net"), notNullValue());
        assertThat(org.getDomain("test-org.net").isVerified(), is(Boolean.TRUE));
    }

    @Test
    @Order(3)
    void shouldFailWhenOrganizationHasEmptyAlias() {
        ImportProcessingException thrown = assertThrows(
                ImportProcessingException.class,
                () -> doImport("03_update_organization_empty_alias.json")
        );
        
        assertThat(thrown.getMessage(), containsString("Organization alias cannot be null or empty"));
    }

    @Test
    @Order(10)
    void shouldFailWhenLinkingNonExistentIdentityProvider() {
        ImportProcessingException thrown = assertThrows(
                ImportProcessingException.class,
                () -> doImport("10_update_organization_link_non_existent_idp.json")
        );
        
        assertThat(thrown.getMessage(), containsString("Identity provider does not exist"));
    }

    @Test
    @Order(4)
    void shouldLinkIdentityProviderWithConfiguration() throws IOException {
        doImport("04_update_organization_idp_config.json");

        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));
        
        OrganizationRepresentation org = maybeOrg.get();
        assertThat(org.getIdentityProviders(), hasSize(1));
        
        OrganizationIdentityProviderConfig idpConfig = org.getIdentityProviders().get(0);
        assertThat(idpConfig.getAlias(), equalTo("oidc-test"));
        assertThat(idpConfig.getDomain(), equalTo("example.org"));
        assertThat(idpConfig.getRedirectWhenEmailMatches(), equalTo(true));
        assertThat(idpConfig.getHideOnLogin(), equalTo(true));
        
        // Verify the identity provider configuration was updated
        Optional<IdentityProviderRepresentation> maybeIdp = identityProviderRepository.search(REALM_NAME, "oidc-test");
        assertThat(maybeIdp.isPresent(), is(true));
        
        IdentityProviderRepresentation idp = maybeIdp.get();
        assertThat(idp.getConfig().get("kc.org.domain"), equalTo("example.org"));
        assertThat(idp.getConfig().get("kc.org.broker.redirect.mode.email-matches"), equalTo("true"));
        assertThat(Boolean.parseBoolean(idp.getConfig().get("hideOnLoginPage")), equalTo(true));
    }
    

    @Test
    @Order(6)
    void shouldHandleMultipleIdentityProviders() throws IOException {
        doImport("06_update_organization_multiple_idps.json");

        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));
        
        OrganizationRepresentation org = maybeOrg.get();
        assertThat(org.getIdentityProviders(), hasSize(2));
        
        // Check first IdP
        OrganizationIdentityProviderConfig idpConfig1 = org.getIdentityProviders().get(0);
        assertThat(idpConfig1.getAlias(), equalTo("saml-test"));
        assertThat(idpConfig1.getDomain(), equalTo("saml.example.org"));
        assertThat(idpConfig1.getRedirectWhenEmailMatches(), equalTo(true));
        assertThat(idpConfig1.getHideOnLogin(), equalTo(false));
        
        // Check second IdP
        OrganizationIdentityProviderConfig idpConfig2 = org.getIdentityProviders().get(1);
        assertThat(idpConfig2.getAlias(), equalTo("keycloak-oidc"));
        assertThat(idpConfig2.getDomain(), nullValue());
        assertThat(idpConfig2.getRedirectWhenEmailMatches(), equalTo(false));
        assertThat(idpConfig2.getHideOnLogin(), equalTo(true));
    }
    
    @Test
    @Order(7)
    void shouldNormalizeIdentityProviderDomains() throws IOException {
        doImport("07_update_organization_idp_domain_normalization.json");

        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));
        
        OrganizationRepresentation org = maybeOrg.get();
        assertThat(org.getIdentityProviders(), hasSize(1));
        
        OrganizationIdentityProviderConfig idpConfig = org.getIdentityProviders().get(0);
        assertThat(idpConfig.getDomain(), equalTo("example.org")); // Should be normalized to lowercase
        
        // Verify the identity provider configuration was updated with normalized domain
        Optional<IdentityProviderRepresentation> maybeIdp = identityProviderRepository.search(REALM_NAME, "oidc-test");
        assertThat(maybeIdp.isPresent(), is(true));
        
        IdentityProviderRepresentation idp = maybeIdp.get();
        assertThat(idp.getConfig().get("kc.org.domain"), equalTo("example.org"));
    }
    
    @Test
    @Order(8)
    void shouldRemoveAllIdentityProviderLinks() throws IOException {
        // First ensure we have IdP links from previous tests
        Optional<OrganizationRepresentation> maybeBefore = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeBefore.isPresent(), is(true));
        assertThat(maybeBefore.get().getIdentityProviders(), not(empty()));
        
        // Now remove all IdP links
        doImport("08_update_organization_remove_idp_links.json");

        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));
        
        OrganizationRepresentation org = maybeOrg.get();
        // In FULL mode, all IdP links should be removed
        assertThat(org.getIdentityProviders(), anyOf(nullValue(), empty()));
    }

    @Test
    @Order(9)
    void shouldUpdateExistingIdentityProviderConfiguration() throws IOException {
        // First, set up initial IdP configuration
        doImport("04_update_organization_idp_config.json");
        
        // Now update the configuration
        doImport("09_update_organization_modify_idp_config.json");

        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));
        
        OrganizationRepresentation org = maybeOrg.get();
        assertThat(org.getIdentityProviders(), hasSize(1));
        
        OrganizationIdentityProviderConfig idpConfig = org.getIdentityProviders().get(0);
        assertThat(idpConfig.getAlias(), equalTo("oidc-test"));
        assertThat(idpConfig.getDomain(), equalTo("updated.example.org"));
        assertThat(idpConfig.getRedirectWhenEmailMatches(), equalTo(false));
        assertThat(idpConfig.getHideOnLogin(), equalTo(false));
        
        // Verify the identity provider configuration was updated
        Optional<IdentityProviderRepresentation> maybeIdp = identityProviderRepository.search(REALM_NAME, "oidc-test");
        assertThat(maybeIdp.isPresent(), is(true));
        
        IdentityProviderRepresentation idp = maybeIdp.get();
        assertThat(idp.getConfig().get("kc.org.domain"), equalTo("updated.example.org"));
        assertThat(idp.getConfig().get("kc.org.broker.redirect.mode.email-matches"), equalTo("false"));
        assertThat(Boolean.parseBoolean(idp.getConfig().get("hideOnLoginPage")), equalTo(false));
    }

    @Test
    @Order(10)
    void shouldUpdateAlreadyLinkedIdentityProviderConfiguration() throws IOException {
        // First ensure we have an IdP linked with specific configuration
        doImport("04_update_organization_idp_config.json");
        
        // Verify initial configuration
        Optional<IdentityProviderRepresentation> maybeIdpBefore = identityProviderRepository.search(REALM_NAME, "oidc-test");
        assertThat(maybeIdpBefore.isPresent(), is(true));
        IdentityProviderRepresentation idpBefore = maybeIdpBefore.get();
        assertThat(idpBefore.getConfig().get("kc.org.domain"), equalTo("example.org"));
        assertThat(idpBefore.getConfig().get("kc.org.broker.redirect.mode.email-matches"), equalTo("true"));
        assertThat(Boolean.parseBoolean(idpBefore.getConfig().get("hideOnLoginPage")), equalTo(true));
        
        // Now update the configuration of the already-linked IdP
        doImport("10_update_linked_idp_config.json");

        // Verify the IdP configuration was updated
        Optional<IdentityProviderRepresentation> maybeIdpAfter = identityProviderRepository.search(REALM_NAME, "oidc-test");
        assertThat(maybeIdpAfter.isPresent(), is(true));
        IdentityProviderRepresentation idpAfter = maybeIdpAfter.get();
        assertThat(idpAfter.getConfig().get("kc.org.domain"), equalTo("modified.example.org"));
        assertThat(idpAfter.getConfig().get("kc.org.broker.redirect.mode.email-matches"), equalTo("false"));
        assertThat(Boolean.parseBoolean(idpAfter.getConfig().get("hideOnLoginPage")), equalTo(false));
        
        // Verify the organization still has the IdP linked
        Optional<OrganizationRepresentation> maybeOrg = organizationRepository.getByAlias(REALM_NAME, "test-org");
        assertThat(maybeOrg.isPresent(), is(true));
        assertThat(maybeOrg.get().getIdentityProviders(), hasSize(1));
    }

    @Test
    @Order(11)
    void shouldRemoveIdentityProviderConfigurationWhenNull() throws IOException {
        // First ensure we have an IdP linked with configuration
        doImport("10_update_linked_idp_config.json");
        
        // Verify configuration exists
        Optional<IdentityProviderRepresentation> maybeIdpBefore = identityProviderRepository.search(REALM_NAME, "oidc-test");
        assertThat(maybeIdpBefore.isPresent(), is(true));
        IdentityProviderRepresentation idpBefore = maybeIdpBefore.get();
        assertThat(idpBefore.getConfig().containsKey("kc.org.domain"), is(true));
        assertThat(idpBefore.getConfig().containsKey("kc.org.broker.redirect.mode.email-matches"), is(true));
        
        // Now import with no configuration values (should remove them)
        doImport("11_update_linked_idp_remove_config.json");

        // Verify the configuration was removed
        Optional<IdentityProviderRepresentation> maybeIdpAfter = identityProviderRepository.search(REALM_NAME, "oidc-test");
        assertThat(maybeIdpAfter.isPresent(), is(true));
        IdentityProviderRepresentation idpAfter = maybeIdpAfter.get();
        assertThat(idpAfter.getConfig().containsKey("kc.org.domain"), is(false));
        assertThat(idpAfter.getConfig().containsKey("kc.org.broker.redirect.mode.email-matches"), is(false));
        // hideOnLogin should remain unchanged (we didn't specify it)
        assertThat(Boolean.parseBoolean(idpAfter.getConfig().get("hideOnLoginPage")), equalTo(false));
    }

    @Test
    @Order(98)
    void shouldDeleteOrganizationInFullMode() throws IOException {
        // Assuming we're in FULL mode by default
        doImport("98_update_realm_remove_organization.json");

        List<OrganizationRepresentation> organizations = organizationRepository.getAll(REALM_NAME);
        assertThat(organizations, empty());
    }
}