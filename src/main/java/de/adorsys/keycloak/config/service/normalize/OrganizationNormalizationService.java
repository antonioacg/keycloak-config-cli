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

package de.adorsys.keycloak.config.service.normalize;

import de.adorsys.keycloak.config.model.OrganizationRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@ConditionalOnProperty(prefix = "run", name = "operation", havingValue = "NORMALIZE", matchIfMissing = true)
public class OrganizationNormalizationService {
    private static final Logger logger = LoggerFactory.getLogger(OrganizationNormalizationService.class);
    
    private static final String WHITESPACE_PATTERN = "\\s+";
    private static final String INVALID_ALIAS_CHARS_PATTERN = "[^a-z0-9-]";
    private static final String HYPHEN_REPLACEMENT = "-";

    public List<OrganizationRepresentation> normalizeOrganizations(List<OrganizationRepresentation> organizations) {
        if (organizations == null) {
            return null;
        }

        for (OrganizationRepresentation organization : organizations) {
            normalizeOrganization(organization);
        }

        return organizations;
    }

    private void normalizeOrganization(OrganizationRepresentation organization) {
        // Set default enabled value if not specified
        if (organization.getEnabled() == null) {
            organization.setEnabled(true);
        }

        // Normalize alias - convert to lowercase and replace spaces with hyphens
        if (organization.getAlias() != null) {
            String normalizedAlias = organization.getAlias()
                    .toLowerCase()
                    .replaceAll(WHITESPACE_PATTERN, HYPHEN_REPLACEMENT)
                    .replaceAll(INVALID_ALIAS_CHARS_PATTERN, "");
            organization.setAlias(normalizedAlias);
        }

        // Normalize attributes - ensure all values are lists
        if (organization.getAttributes() != null) {
            Map<String, List<String>> normalizedAttributes = new HashMap<>();
            
            for (Map.Entry<String, List<String>> entry : organization.getAttributes().entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                
                if (values != null && !values.isEmpty()) {
                    // Remove null and empty values
                    List<String> cleanedValues = new ArrayList<>();
                    for (String value : values) {
                        if (StringUtils.hasText(value)) {
                            cleanedValues.add(value.trim());
                        }
                    }
                    
                    if (!cleanedValues.isEmpty()) {
                        normalizedAttributes.put(key, cleanedValues);
                    }
                }
            }
            
            organization.setAttributes(normalizedAttributes.isEmpty() ? null : normalizedAttributes);
        }

        // Normalize domains
        if (organization.getDomains() != null && !organization.getDomains().isEmpty()) {
            organization.getDomains().forEach(domain -> {
                if (domain != null && domain.getName() != null) {
                    // Normalize domain name to lowercase
                    domain.setName(domain.getName().toLowerCase().trim());
                }
            });
            
            // Remove any domains with null or empty names
            organization.getDomains().removeIf(domain -> 
                domain == null || !StringUtils.hasText(domain.getName())
            );
        }

        // Normalize identity provider list
        if (organization.getIdentityProviders() != null) {
            // Remove duplicates and nulls
            List<String> uniqueIdps = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            
            for (String idp : organization.getIdentityProviders()) {
                if (StringUtils.hasText(idp) && seen.add(idp.trim())) {
                    uniqueIdps.add(idp.trim());
                }
            }
            
            organization.setIdentityProviders(uniqueIdps.isEmpty() ? null : uniqueIdps);
        }

        // Log normalization actions
        logger.debug("Normalized organization '{}' with alias '{}'", 
                    organization.getName(), organization.getAlias());
    }
}