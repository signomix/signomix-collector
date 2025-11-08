package com.signomix.collector.app.ports.driving;

import org.jboss.logging.Logger;

import com.signomix.collector.app.logic.TwinLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TwinLogicPort {

    @Inject
    Logger logger;

    @Inject
    TwinLogic twinLogic;

    public void handleMessage(String message) {
        String[] parts = message.split(",");
        twinLogic.processData(parts);
    }

}
