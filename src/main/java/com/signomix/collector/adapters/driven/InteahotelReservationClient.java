package com.signomix.collector.adapters.driven;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.signomix.collector.app.logic.inteahotel.dto.CredentialsDto;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * To use it via injection.
 *
 * {@code
 *     @Inject
 *     @RestClient
 *     MyRemoteService myRemoteService;
 *
 *     public void doSomething() {
 *         Set<MyRemoteService.Extension> restClientExtensions = myRemoteService.getExtensionsById("io.quarkus:quarkus-hibernate-validator");
 *     }
 * }
 */
@RegisterRestClient(configKey = "reservation-service")
public interface InteahotelReservationClient {

    @POST
    @Path("/api/login")
    Response login(CredentialsDto credentials);

    @GET
    @Path("/api/heaters/rooms")
    Response getRoomStatuses(@HeaderParam("Authorization") String token);

    @GET
    @Path("/api/heaters/rooms/recently-changed")
    Response getUpdatedRoomStatuses(@HeaderParam("Authorization") String token);
}
