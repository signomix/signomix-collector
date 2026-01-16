package com.signomix.collector.app.logic;

import java.util.HashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

@ApplicationScoped
public class TwinLogic {

    static final int HEADER_SIZE = 9;

    @Inject
    Logger logger;

    @ConfigProperty(name = "signomix.twin.organization.id")
    Long twinOrganizationId;

    @ConfigProperty(name = "signomix.twin.device.eui.prefix")
    String twinDeviceEuiPrefix;

    private static Jedis jedis;

    public void processData(String[] dataParts) {
        try {
            if (dataParts.length < HEADER_SIZE) {
                logger.warn("Insufficient data parts received.");
                return;
            }
            String deviceEUI = dataParts[0];
            String organizationStr = dataParts[1];
            String name = dataParts[2];
            String statusStr = dataParts[3];
            String alertStatusStr = dataParts[4];
            String latitudeStr = dataParts[5];
            String longitudeStr = dataParts[6];
            String altitudeStr = dataParts[7];
            String timestampStr = dataParts[8];
            long organization;
            long timestamp;
            try {
                organization = Long.parseLong(organizationStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format: " + e.getMessage());
                return;
            }
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format: " + e.getMessage());
                return;
            }
            if (organization != twinOrganizationId || !deviceEUI.startsWith(twinDeviceEuiPrefix)) {
                logger.warn("Data key not supported: " + organization + ":" + deviceEUI);
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
                if (keyValue.length == 2 && !keyValue[1].equalsIgnoreCase("null")) {
                    dataMap.put(keyValue[0], keyValue[1]);
                }
            }
            logger.info("Handling twin data: " + dataMap.toString());
            saveToRedisTransaction(deviceEUI, organization, dataMap);
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

    // save data to redis
    private void saveToRedis(String deviceEUI, long organization, long timestamp, HashMap<String, String> dataMap) {
        long actualTimestamp = getActualTimestamp(deviceEUI, organization);
        long newTimestamp = 0;
        long delta = 0;
        try {
            newTimestamp = Long.parseLong(dataMap.get("timestamp"));
        } catch (Exception e) {
            logger.error("Error parsing new timestamp: " + e.getMessage());
        }
        if (actualTimestamp > 0 && newTimestamp > actualTimestamp) {
            delta = (newTimestamp - actualTimestamp) / 1000; // in seconds
        }
        dataMap.put("transmission_interval", Long.toString(delta));
        try {
            Jedis jedis = getJedis();
            String redisKey = "" + organization + ":" + deviceEUI;
            jedis.hmset(redisKey, dataMap);
        } catch (Exception e) {
            logger.error("Error saving to Redis: " + e.getMessage());
        }
    }

    // save data to redis with transaction
    private void saveToRedisTransaction(String deviceEUI, long organization, HashMap<String, String> dataMap) {
        try {
            Jedis jedis = getJedis();
            String redisKey = "" + organization + ":" + deviceEUI;
            // Use WATCH/MULTI/EXEC pattern: read current timestamp first, compute delta,
            // then start transaction to update values. Calling Response.get() on a
            // Transaction before exec causes "Please close pipeline or multi block"
            // error, so avoid reading via Transaction.
            jedis.watch(redisKey);
            String timestampStr = jedis.hget(redisKey, "timestamp");
            long actualTimestamp = 0;
            try {
                if (timestampStr != null) {
                    actualTimestamp = Long.parseLong(timestampStr);
                }
            } catch (Exception e) {
                logger.warn("Error parsing actual timestamp: " + e.getMessage());
            }
            long newTimestamp = 0;
            long delta = 0;
            try {
                String newTsStr = dataMap.get("timestamp");
                if (newTsStr != null) {
                    newTimestamp = Long.parseLong(newTsStr);
                }
            } catch (Exception e) {
                logger.warn("Error parsing new timestamp: " + e.getMessage());
            }
            if (actualTimestamp > 0 && newTimestamp > actualTimestamp) {
                delta = (newTimestamp - actualTimestamp) / 1000; // in seconds
            }
            dataMap.put("transmission_interval", Long.toString(delta));

            Transaction t = null;
            try {
                t = jedis.multi();
                t.hmset(redisKey, dataMap);
                java.util.List<Object> execResult = t.exec();
                if (execResult == null) {
                    // Transaction was aborted because watched key changed
                    logger.warn("Redis transaction aborted for key " + redisKey + " (watched key changed)");
                }
            } finally {
                if (t != null) {
                    try {
                        t.close();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                try {
                    jedis.unwatch();
                } catch (Exception ex) {
                    // ignore
                }
            }
        } catch (Exception e) {
            logger.error("Error saving to Redis with transaction: " + e.getMessage());
        }
    }

    // get actual timestamp from selected key
    public long getActualTimestamp(String deviceEUI, long organization) {
        long timestamp = 0;
        try {
            Jedis jedis = getJedis();
            String redisKey = "" + organization + ":" + deviceEUI;
            String timestampStr = jedis.hget(redisKey, "timestamp");
            if (timestampStr != null) {
                timestamp = Long.parseLong(timestampStr);
            }
        } catch (Exception e) {
            logger.error("Error retrieving timestamp from Redis: " + e.getMessage());
        }
        return timestamp;
    }

    // get actual timestamp within transaction
    public long getActualTimestampInTransaction(Transaction t, String deviceEUI, long organization) {
        long timestamp = 0;
        try {
            String redisKey = "" + organization + ":" + deviceEUI;
            String timestampStr = t.hget(redisKey, "timestamp").get();
            if (timestampStr != null) {
                timestamp = Long.parseLong(timestampStr);
            }
        } catch (Exception e) {
            logger.error("Error retrieving timestamp from Redis within transaction: " + e.getMessage());
        }
        return timestamp;
    }

}
