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

import de.adorsys.keycloak.config.model.OrganizationRepresentation;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Custom API interface for Keycloak Organizations management.
 * This is needed because the Keycloak admin client 26.0.4 doesn't include Organizations API.
 */
@Path("/admin/realms/{realm}/organizations")
public interface OrganizationApi {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(@PathParam("realm") String realm, OrganizationRepresentation organization);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> search(@PathParam("realm") String realm,
                                           @QueryParam("search") String search,
                                           @QueryParam("searchQuery") String searchQuery,
                                           @QueryParam("exact") Boolean exact,
                                           @QueryParam("first") Integer first,
                                           @QueryParam("max") Integer max,
                                           @QueryParam("briefRepresentation") Boolean briefRepresentation);

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    Integer count(@PathParam("realm") String realm,
                  @QueryParam("search") String search,
                  @QueryParam("searchQuery") String searchQuery);

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    OrganizationRepresentation get(@PathParam("realm") String realm, @PathParam("id") String id);

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void update(@PathParam("realm") String realm, @PathParam("id") String id, OrganizationRepresentation organization);

    @DELETE
    @Path("/{id}")
    void delete(@PathParam("realm") String realm, @PathParam("id") String id);
}