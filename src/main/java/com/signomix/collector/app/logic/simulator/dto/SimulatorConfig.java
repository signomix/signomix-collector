package com.signomix.collector.app.logic.simulator.dto;

public class SimulatorConfig {
    public String source; // Source device EUI
    public String target; // Target device EUI
    public Long interval; // Interval for continuous simulation
    public String fromDate; // Start date for simulation
    public String toDate; // End date for simulation
    public String startDate; // Start date for simulation
    public String dataNames; // Comma-separated list of data names to simulate
    public String notModifiedNames; // Comma-separated list of data names which values should not be modified
    public Double dataVariability; // Data variability for simulation
    public boolean continuous; // Whether to run simulation continuously
    public Long counter;
    public String data; // Historical data (CSV format)
}
