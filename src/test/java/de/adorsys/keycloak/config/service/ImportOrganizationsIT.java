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
import de.adorsys.keycloak.config.model.OrganizationRepresentation;
import de.adorsys.keycloak.config.repository.OrganizationRepository;
import de.adorsys.keycloak.config.util.VersionUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
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
        assertThat(org.getDomain("test-org.com").getVerified(), is(Boolean.TRUE));
        assertThat(org.getDomain("test-org.org"), notNullValue());
        assertThat(org.getDomain("test-org.org").getVerified(), is(Boolean.FALSE));

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
        assertThat(org.getDomain("test-org.net").getVerified(), is(Boolean.TRUE));
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
    @Order(98)
    void shouldDeleteOrganizationInFullMode() throws IOException {
        // Assuming we're in FULL mode by default
        doImport("98_update_realm_remove_organization.json");

        List<OrganizationRepresentation> organizations = organizationRepository.getAll(REALM_NAME);
        assertThat(organizations, empty());
    }
}