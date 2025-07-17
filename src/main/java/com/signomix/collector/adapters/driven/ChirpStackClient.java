package com.signomix.collector.adapters.driven;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.signomix.collector.app.logic.chirpstack.dto.ChirpStackLoginRequest;
import com.signomix.collector.app.logic.chirpstack.dto.ChirpStackLoginResponse;
import com.signomix.collector.app.logic.chirpstack.dto.GatewayStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@RegisterRestClient(configKey = "chirpstack-api")
@Path("/api")
public interface ChirpStackClient {
    
    @POST
    @Path("/internal/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ChirpStackLoginResponse login(ChirpStackLoginRequest request);

    @GET
    @Path("/gateways")
    @Produces(MediaType.APPLICATION_JSON)
    List<GatewayStatus> getGateways(@HeaderParam("Grpc-Metadata-Authorization") String jwt);

}
