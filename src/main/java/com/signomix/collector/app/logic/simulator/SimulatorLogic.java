package com.signomix.collector.app.logic.simulator;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SimulatorLogic {

    @Inject
    Logger logger;

    //@Inject
    //SimulatorConfigDao simulatorConfigDao;


    public void runContinuousSimulation(String source, String target, String apiKey, String fromDate, String toDate,
            String dataNames, String notModifiedNames, Double dataVariability, Long interval) {
        // Implementation for starting continuous simulation
    }

    public void createSimulation(String source, String target, String apiKey, String fromDate, String toDate,
            String startDate, String dataNames, String notModifiedNames, Double dataVariability) {
        // Implementation for creating a simulation
    }

}
