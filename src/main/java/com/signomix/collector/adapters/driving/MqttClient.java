package com.signomix.collector.adapters.driving;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import com.signomix.collector.app.ports.driving.ReservationLogicPort;
import com.signomix.collector.app.ports.driving.SimulationLogicPort;
import com.signomix.collector.app.ports.driving.TwinLogicPort;

import jakarta.inject.Inject;

public class MqttClient {

    @Inject
    Logger logger;

    @Inject
    ReservationLogicPort reservationLogicPort;

    @Inject
    SimulationLogicPort simulationLogicPort;

    @Inject
    TwinLogicPort twinLogicPort;

    @Incoming("commands")
    public void processReservationSyncCommand(byte[] bytes) {
        try {
            String msg = new String(bytes);
            if (msg.startsWith("simulation;")) {
                simulationLogicPort.handleMessage(msg);
            } else {
                reservationLogicPort.handleMessage(msg);
            }
        } catch (Exception e) {
            logger.error("Error processing MQTT message", e);
        }
    }

    @Incoming("data-received")
    public void receiveData(byte[] message) {
        twinLogicPort.handleMessage(new String(message));
    }

}
