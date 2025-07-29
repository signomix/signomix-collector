package com.signomix.collector.app.ports.driving;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import com.signomix.collector.app.logic.simulator.SimulatorLogic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SimulationLogicPort {

    @Inject
    Logger logger;

    @Inject
    SimulatorLogic simulatorLogic;

    public void handleMessage(String message) {
        Map<String, String> parameters = getParametersMap(message.split(";"));
        String source = parameters.get("source"); // Source device EUI
        if (source == null || source.isEmpty()) {
            logger.warn("No source specified for simulation command");
            return;
        }
        String target = parameters.get("target"); // Target device EUI
        if (target == null || target.isEmpty()) {
            logger.warn("No target specified for simulation command");
            return;
        }
        String apiKey = parameters.get("apikey"); // API key for authentication
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("No API key specified for simulation command");
            return;
        }
        String dataNames = parameters.get("datanames"); // Comma-separated list of data names to simulate
        String notModifiedNames = parameters.get("notmodifiednames"); // Comma-separated list of data names which values
                                                                      // should not be modified
        String fromDate = parameters.get("fromdate"); // Start date for simulation
        String toDate = parameters.get("todate"); // End date for simulation
        String startDate = parameters.get("startdate"); // Start date for simulation
        String sContinous = parameters.get("continuous"); // Whether to run simulation continuously
        if (sContinous == null || sContinous.isEmpty() || (!sContinous.equals("true") && !sContinous.equals("false"))) {
            logger.warn("No continuous flag specified for simulation command. Ignoring command.");
            return;
        }
        boolean continuous = Boolean.parseBoolean(sContinous);
        String sInterval = parameters.get("interval"); // Interval for continuous simulation
        Long interval = null;
        if (sInterval != null && !sInterval.isEmpty()) {
            try {
                interval = Long.parseLong(sInterval);
            } catch (NumberFormatException e) {
                logger.warn("Invalid interval specified for simulation command: " + sInterval);
            }
        }
        String sDataVariability = parameters.get("datavariability"); // Data variability for simulation
        Double dataVariability = 0.0;
        try {
            dataVariability = Double.parseDouble(sDataVariability);
        } catch (Exception e) {
            logger.warn("Invalid data variability specified for simulation command: " + sDataVariability);
            return;
        }
        if (continuous) {
            if (fromDate == null || fromDate.isEmpty()) {
                logger.warn("No fromDate specified for continuous simulation command");
                return;
            }
            if (toDate == null || toDate.isEmpty()) {
                logger.warn("No toDate specified for continuous simulation command");
                return;
            }
            if (dataNames == null || dataNames.isEmpty()) {
                logger.warn("No dataNames specified for continuous simulation command");
                return;
            }
            simulatorLogic.runContinuousSimulation(source, target, apiKey, fromDate, toDate, dataNames,
                    notModifiedNames, dataVariability, interval);

        } else {
            if (dataNames == null || dataNames.isEmpty()) {
                logger.warn("No dataNames specified for continuous simulation command");
                return;
            }
            if (fromDate == null || fromDate.isEmpty()) {
                logger.warn("No fromDate specified for simulation command");
                return;
            }
            if (toDate == null || toDate.isEmpty()) {
                logger.warn("No toDate specified for simulation command");
                return;
            }
            if (startDate == null || startDate.isEmpty()) {
                logger.warn("No startDate specified for simulation command");
                return;
            }
            simulatorLogic.createSimulation(source, target, apiKey, fromDate, toDate, startDate, dataNames,
                    notModifiedNames, dataVariability);
        }
    }

    private Map<String, String> getParametersMap(String[] parts) {
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
