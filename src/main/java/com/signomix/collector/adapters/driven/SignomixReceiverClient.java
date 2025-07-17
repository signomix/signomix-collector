package com.signomix.collector.adapters.driven;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;


@RegisterRestClient(configKey = "signomix-receiver")
public interface SignomixReceiverClient {

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void sendSynchronizationStatus(@HeaderParam("Authorization") String key, 
    @HeaderParam("X-device-eui") String deviceEui,
    @FormParam("sync_status") double syncStatus,
    @FormParam("error_message") String errorMessage);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void sendRoomStatusData(@HeaderParam("Authorization") String key,
    @HeaderParam("X-device-eui") String deviceEui,
    @FormParam("status") double status);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void sendRoomStatusCheck(@HeaderParam("Authorization") String key,
    @HeaderParam("X-device-eui") String deviceEui,
    @FormParam("status_check") double status);

}
