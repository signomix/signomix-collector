package com.signomix.collector.app.logic;

import java.util.HashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import redis.clients.jedis.Jedis;

@ApplicationScoped
public class TwinLogic {

     static final int HEADER_SIZE=8;

    @Inject
    Logger logger;

    @ConfigProperty(name = "signomix.twin.organization.id") 
    Long twinOrganizationId;

    @ConfigProperty(name = "signomix.twin.device.eui.prefix") 
    String twinDeviceEuiPrefix;

    private static Jedis jedis;

    public void processData(String[] dataParts) {
        try {
            if (dataParts.length < 2) {
                logger.warn("Insufficient data parts received.");
                return;
            }
            String deviceEUI = dataParts[0];
            String organizationStr= dataParts[1];
            String name = dataParts[2];
            String statusStr = dataParts[3];
            String alertStatusStr = dataParts[4];
            String latitudeStr = dataParts[5];
            String longitudeStr = dataParts[6];
            String altitudeStr = dataParts[7];
            String timestampStr= dataParts[8];
            long organization;
            long timestamp;
            try{
                organization = Long.parseLong(organizationStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format: " + e.getMessage());
                return;
            }
            try{
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format: " + e.getMessage());
                return;
            }
            if(organization!=twinOrganizationId || !deviceEUI.startsWith(twinDeviceEuiPrefix)){
                logger.warn("Data key not supported: " + organization + ":" + deviceEUI  );
                return;
            }

            HashMap<String, String> dataMap = new HashMap<>();
            dataMap.put("eui", deviceEUI);
            dataMap.put("name", name);
            dataMap.put("organization", organizationStr);
            dataMap.put("timestamp", timestampStr);
            dataMap.put("d_alertStatus", alertStatusStr);
            dataMap.put("d_latitude", latitudeStr);
            dataMap.put("d_longitude", longitudeStr);
            dataMap.put("d_altitude", altitudeStr);
            dataMap.put("d_status", statusStr);

            for (int i = HEADER_SIZE; i < dataParts.length; i++) {
                String[] keyValue = dataParts[i].split("=");
                if (keyValue.length == 2) {
                    try {
                        dataMap.put(keyValue[0], keyValue[1]);
                    } catch (NumberFormatException nfe) {
                        logger.warn("Invalid number format for key: " + keyValue[0]);
                    }
                }
            }
            logger.info("Handling twin data: " + dataMap.toString());
            saveToRedis(deviceEUI, organization, timestamp  , dataMap);
        } catch (Exception e) {
            logger.error("Error processing twin data: " + e.getMessage());
        }
    }

    private Jedis getJedis() {
        if (jedis == null) {
            jedis = new Jedis("redis", 6379);
        }
        return jedis;
    }
    private void saveToRedis(String deviceEUI, long organization, long timestamp, HashMap<String, String> dataMap) {
        try {
            Jedis jedis = getJedis();
            String redisKey=""+organization+":"+deviceEUI;
            jedis.hmset(redisKey, dataMap);
        } catch (Exception e) {
            logger.error("Error saving to Redis: " + e.getMessage());
        }
    }

}
