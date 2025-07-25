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

import de.adorsys.keycloak.config.model.OrganizationRepresentation;
import de.adorsys.keycloak.config.provider.KeycloakProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for OrganizationRepository input validation.
 * These tests focus on the defensive programming improvements added to ensure
 * proper input validation and fail-fast behavior.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationRepositoryTest {

    @Mock
    private KeycloakProvider keycloakProvider;

    @Mock
    private IdentityProviderRepository identityProviderRepository;

    private OrganizationRepository organizationRepository;

    @BeforeEach
    void setUp() {
        organizationRepository = new OrganizationRepository(keycloakProvider, identityProviderRepository);
        // No API stubbing needed - these tests only validate input parameters
    }

    @Test
    void create_shouldThrowException_whenRealmIsNull() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.create(null, org)
        );

        assert exception.getMessage().equals("Realm cannot be null or empty");
    }

    @Test
    void create_shouldThrowException_whenRealmIsEmpty() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("test-org");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.create("", org)
        );

        assert exception.getMessage().equals("Realm cannot be null or empty");
    }

    @Test
    void create_shouldThrowException_whenOrganizationIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.create("test-realm", null)
        );

        assert exception.getMessage().equals("Organization cannot be null");
    }

    @Test
    void create_shouldThrowException_whenOrganizationAliasIsNull() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        // alias is null by default

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.create("test-realm", org)
        );

        assert exception.getMessage().equals("Organization alias cannot be null or empty");
    }

    @Test
    void create_shouldThrowException_whenOrganizationAliasIsEmpty() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setAlias("");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.create("test-realm", org)
        );

        assert exception.getMessage().equals("Organization alias cannot be null or empty");
    }

    @Test
    void update_shouldThrowException_whenRealmIsNull() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setId("test-id");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.update(null, org)
        );

        assert exception.getMessage().equals("Realm cannot be null or empty");
    }

    @Test
    void update_shouldThrowException_whenOrganizationIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.update("test-realm", null)
        );

        assert exception.getMessage().equals("Organization cannot be null");
    }

    @Test
    void update_shouldThrowException_whenOrganizationIdIsNull() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        // id is null by default

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.update("test-realm", org)
        );

        assert exception.getMessage().equals("Organization ID cannot be null or empty");
    }

    @Test
    void update_shouldThrowException_whenOrganizationIdIsEmpty() {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setId("");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organizationRepository.update("test-realm", org)
        );

        assert exception.getMessage().equals("Organization ID cannot be null or empty");
    }
}