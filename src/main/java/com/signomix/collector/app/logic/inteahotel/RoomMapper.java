package com.signomix.collector.app.logic.inteahotel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoomMapper {

    @Inject
    Logger logger;

    private final Map<Integer, Integer> roomMapping = new HashMap<>();

    public Map<Integer, Integer> getRoomMapping() {
        return roomMapping;
    }

    @PostConstruct
    public void loadRoomMapping() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("/roommapping.csv"), StandardCharsets.UTF_8))) {
            String line;
            // Skip the first line (header)
            reader.readLine();
            // Read the rest of the lines
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines or comments
                }
                String[] columns = line.split(",");
                if (columns.length == 2) {
                    Integer inteahotel = Integer.parseInt(columns[0].trim());
                    Integer kombornia = Integer.parseInt(columns[1].trim());
                    roomMapping.put(inteahotel, kombornia);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load room mapping from pokoje.csv", e);
        }
        if (logger.isDebugEnabled()) {
            roomMapping.forEach(
                    (inteahotel, kombornia) -> logger.debug("Inteahotel: " + inteahotel + ", Kombornia: " + kombornia));
        }
    }
}