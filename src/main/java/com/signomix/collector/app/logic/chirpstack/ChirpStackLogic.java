package com.signomix.collector.app.logic.chirpstack;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import com.signomix.collector.adapters.driven.ChirpStackClient;
import com.signomix.collector.app.logic.chirpstack.dto.ChirpStackLoginRequest;
import com.signomix.collector.app.logic.chirpstack.dto.ChirpStackLoginResponse;
import com.signomix.collector.app.logic.chirpstack.dto.GatewayStatus;

import jakarta.inject.Inject;

public class ChirpStackLogic {

    @Inject
    Logger logger;

    /* @RestClient
    ChirpStackClient chirpStackClient; */

    public void getGateways(Map<String, String> parameters) {
        String user = parameters.get("user");
        String password = parameters.get("password");
        String url = parameters.get("url");
        if (url == null || url.isEmpty()) {
            logger.error("ChirpStack URL not provided");
            return;
        }
        if (user == null || password == null || user.isEmpty() || password.isEmpty() || url.isEmpty()) {
            logger.error("Credentials for ChirpStack service not provided");
            return;
        }
        ChirpStackClient chirpStackClient = RestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .build(ChirpStackClient.class);
        // Logic to get gateways using email and password
        ChirpStackLoginRequest request = new ChirpStackLoginRequest();
        request.email = user;
        request.password = password;
        ChirpStackLoginResponse response = chirpStackClient.login(request);
        if (response != null && response.jwt != null) {
            String jwt = response.jwt;
            // Use the JWT to fetch gateways
            List<GatewayStatus> gateways = chirpStackClient.getGateways(jwt);
            for (GatewayStatus gateway : gateways) {
                logger.info("Gateway ID: " + gateway.gatewayId);
                logger.info("Gateway Name: " + gateway.name);
                logger.info("Last Seen At: " + gateway.lastSeenAt);
                logger.info("Online: " + gateway.online);
            }
        } else {
            // Handle login failure
            logger.error("Failed to login to ChirpStack");
        }
    }

}
