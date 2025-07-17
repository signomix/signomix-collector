package com.signomix.collector.app.ports.driving;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import com.signomix.collector.app.logic.inteahotel.InteaReservationLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReservationLogicPort {

    @Inject
    Logger logger;

    @Inject
    InteaReservationLogic inteaReservationLogic;

    public void handleMessage(String message) {
        String[] parts = message.split(";");
        Map<String, String> parameters = getParametersMap(parts);
        String type = parameters.get("type");
        if (type == null || type.isEmpty()) {
            logger.warn("No type specified in message: " + message);
            return;
        }
        switch (type.toLowerCase()) {
            case "reservationsync":
                synchronizeReservations(parameters);
                break;
            case "reservationupdate":
                updateReservations(parameters);
                break;
            case "statuscheck":
                updateStatuses(parameters);
                break;
            default:
                logger.warn("Unknown message type: " + type);
                break;
        }
    }
    
    public void synchronizeReservations(Map<String, String> parameters) {
        String provider = parameters.get("provider");
        if (provider == null || provider.isEmpty()) {
            logger.warn("No provider specified");
            return;
        }
        switch (provider.toLowerCase()) {
            case "inteahotel":
                inteaReservationLogic.synchronizeReservations(false, parameters);
                break;
            default:
                logger.warn("Unknown provider: " + provider);
                break;
        }
        
    }

    public void updateReservations(Map<String, String> parameters) {
        String provider = parameters.get("provider");
        if (provider == null || provider.isEmpty()) {
            logger.warn("No provider specified");
            return;
        }
        switch (provider.toLowerCase()) {
            case "inteahotel":
                inteaReservationLogic.synchronizeReservations(true, parameters);
                break;
            default:
                logger.warn("Unknown provider: " + provider);
                break;
        }
        
    }

    public void updateStatuses(Map<String, String> parameters) {
        String provider = parameters.get("provider");
        if (provider == null || provider.isEmpty()) {
            logger.warn("No provider specified");
            return;
        }
        switch (provider.toLowerCase()) {
            case "inteahotel":
                inteaReservationLogic.updateStatuses(parameters);
                break;
            default:
                logger.warn("Unknown provider: " + provider);
                break;
        }
        
    }
    
    private Map<String,String> getParametersMap(String[] parts) {
        Map<String, String> parameters = new HashMap<>();
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                parameters.put(keyValue[0].trim().toLowerCase(), keyValue[1].trim());
            }
        }
        return parameters;
    }

}
