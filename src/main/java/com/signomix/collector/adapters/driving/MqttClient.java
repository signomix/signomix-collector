package com.signomix.collector.adapters.driving;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.collector.app.ports.driving.ReservationLogicPort;

import jakarta.inject.Inject;

public class MqttClient {

    @Inject
    Logger logger;

    @Inject
    ReservationLogicPort reservationLogicPort;

    @Incoming("commands")
    public void processReservationSyncCommand(byte[] bytes) {
        String msg = new String(bytes);
        reservationLogicPort.handleMessage(msg);
    }

}
