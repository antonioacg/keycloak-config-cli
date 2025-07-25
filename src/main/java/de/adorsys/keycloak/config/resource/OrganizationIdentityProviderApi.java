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

package de.adorsys.keycloak.config.resource;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

import java.util.List;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Custom API interface for managing identity providers within organizations.
 * This is needed because the Keycloak admin client 26.0.4 doesn't include Organizations API.
 */
@Path("/admin/realms/{realm}/organizations/{orgId}/identity-providers")
public interface OrganizationIdentityProviderApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<IdentityProviderRepresentation> getIdentityProviders(@PathParam("realm") String realm,
                                                              @PathParam("orgId") String orgId);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response addIdentityProvider(@PathParam("realm") String realm,
                                @PathParam("orgId") String orgId,
                                String identityProviderAlias);

    @GET
    @Path("/{alias}")
    @Produces(MediaType.APPLICATION_JSON)
    IdentityProviderRepresentation getIdentityProvider(@PathParam("realm") String realm,
                                                       @PathParam("orgId") String orgId,
                                                       @PathParam("alias") String alias);

    @DELETE
    @Path("/{alias}")
    void removeIdentityProvider(@PathParam("realm") String realm,
                               @PathParam("orgId") String orgId,
                               @PathParam("alias") String alias);
}