package com.signomix.collector.adapters.driving;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.media.PatternProperty;
import org.jboss.logging.Logger;

import com.signomix.collector.app.exception.ServiceException;
import com.signomix.collector.app.logic.inteahotel.InteaReservationLogic;
import com.signomix.collector.app.logic.inteahotel.dto.ReservationDto;
import com.signomix.collector.app.ports.driving.AuthPort;
import com.signomix.common.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/api/reservations")
public class ForReservationsAdapter {

    @Inject
    Logger logger;

    @Inject
    InteaReservationLogic reservationLogic;

    @Inject
    AuthPort authPort;

    @ConfigProperty(name = "signomix.exception.api.unauthorized")
    String unauthorizedException;

    @Path("/hello")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from reservations REST API";
    }

    @Path("/status/{roomPrefix}/{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String status(@HeaderParam("Authentication") String token,
            @PathParam(value = "roomPrefix") String roomPrefix,
            @PathParam("id") Integer id) {
        User user = authPort.getUser(token);
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        Integer status = reservationLogic.getRoomReservationStatus(id, roomPrefix);
        return "" + status;
    }

    /**
     * Zmiana statusu rezerwacji dla pokoju
     * 
     * @param reservation
     * @return
     * 
     *         Example CURL request:
     *         curl -i -H "Content-Type: application/json" -d
     *         '{"roomId":2,"status":5}'
     *         "http://localhost:9999/api/reservations/status"
     */
    
    @POST
    @Path("/{pathParam}/status")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String setStatus(@HeaderParam("Authentication") String token, @PathParam("pathParam") String roomPrefix, ReservationDto reservation) {
        User user = authPort.getUser(token);
        if (null == user) {
            throw new ServiceException(unauthorizedException);
        }
        try {
            long timestamp = System.currentTimeMillis();
            reservationLogic.saveRoomReservationStatus(roomPrefix, reservation.roomId, reservation.status, timestamp);
            reservationLogic.createRoomDataEvent(roomPrefix, reservation.roomId, reservation.status, reservation.arrivalDate, timestamp);
            return "OK";
        } catch (Exception e) {
            logger.error("Error setting status for room " + reservation.roomId + " to " + reservation.status);
            e.printStackTrace();
            return "ERROR";
        }
    }
}
