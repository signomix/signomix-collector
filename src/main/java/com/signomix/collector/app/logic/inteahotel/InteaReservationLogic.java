package com.signomix.collector.app.logic.inteahotel;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.signomix.collector.adapters.driven.InteahotelReservationClient;
import com.signomix.collector.adapters.driven.SignomixReceiverClient;
import com.signomix.collector.adapters.driven.StatusDatabaseAdapter;
import com.signomix.collector.app.logic.inteahotel.dto.CredentialsDto;
import com.signomix.collector.app.logic.inteahotel.dto.ErrorMessageDto;
import com.signomix.collector.app.logic.inteahotel.dto.RoomDataDto;
import com.signomix.collector.app.logic.inteahotel.dto.RoomStatusResponse;
import com.signomix.collector.app.logic.inteahotel.dto.TokenDto;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.iot.Device;
import com.signomix.common.tsdb.IotDatabaseDao;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class InteaReservationLogic {

    @Inject
    Logger logger;

    @Inject
    RoomMapper roomMapper;

    @Inject
    @DataSource("olap")
    AgroalDataSource olapDs;

    @Inject
    @Channel("data")
    Emitter<String> dataEmitter;

    StatusDatabaseAdapter statusDatabaseAdapter = null;

    IotDatabaseDao iotDatabaseDao = null;

    @RestClient
    SignomixReceiverClient signomixReceiverClient;

    /* @RestClient
    InteahotelReservationClient inteahotelReservationClient; */

    /* @ConfigProperty(name = "room.prefix")
    String roomPrefix;
    @ConfigProperty(name = "room.key")
    String roomKey;
    @ConfigProperty(name = "sync.device.eui")
    String syncDeviceEui;
    @ConfigProperty(name = "sync.device.key")
    String syncDeviceAuthKey;
    @ConfigProperty(name = "room.group")
    String roomGroup; */

    StatusDatabaseAdapter getStatusDatabaseAdapter() {
        if (null == statusDatabaseAdapter) {
            statusDatabaseAdapter = new StatusDatabaseAdapter();
            statusDatabaseAdapter.setDatasource(olapDs);
        }
        return statusDatabaseAdapter;
    }

    IotDatabaseDao getIotDatabaseDao() {
        if (null == iotDatabaseDao) {
            iotDatabaseDao = new IotDatabaseDao();
            iotDatabaseDao.setDatasource(olapDs);
        }
        return iotDatabaseDao;
    }

    public Integer getRoomReservationStatus(Integer roomId, String roomPrefix) {
        if (logger.isDebugEnabled()) {
            logger.debug("Getting status for room " + roomId);
        }
        String eui = getRoomEuiFromNumber(roomId, roomPrefix);
        try {
            return getStatusDatabaseAdapter().getRoomReservationStatus(eui);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error getting status for room " + roomId);
        }
        return null;
    }

    /**
     * Set room reservation status
     * 
     * @param roomId
     * @param status
     * @param timestamp
     */
    public void saveRoomReservationStatus(String roomPrefix, Integer roomId, Integer status, long timestamp) {
        String eui = getRoomEuiFromNumber(roomId, roomPrefix);
        try {
            getStatusDatabaseAdapter().setRoomReservationStatus(eui, status, timestamp);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error setting status for room " + roomId + " to " + status);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Setting status for room " + roomId + " to " + status);
        }
    }

    /**
     * Create room status event
     * 
     * @param roomId
     * @param status
     * @param timestamp
     */
    public void createRoomDataEvent(String roomPrefix, Integer roomId, Integer status, long timestamp) {
        String eui = getRoomEuiFromNumber(roomId, roomPrefix);
        // send data to MQTT
        String timestampStr = Long.toString(timestamp);
        StringBuffer sb = new StringBuffer();
        sb.append("eui=");
        sb.append(eui);
        sb.append(";");
        sb.append("status=");
        sb.append(status);
        sb.append(";");
        sb.append("timestamp=");
        sb.append(timestampStr);
        dataEmitter.send(sb.toString());
    }

    /**
     * Create EUI from room ID
     * 
     * @param roomId
     * @return
     */
    public String getRoomEuiFromNumber(Integer roomId, String roomPrefix) {
        // transform roomId to String containing number prepended by 0s to have 3 digits
        String roomIdStr = String.format("%03d", roomId);
        String eui = roomPrefix + roomIdStr;
        return eui;
    }

    /**
     * Synchronize reservations
     */
    public void synchronizeReservations(boolean updatedOnly, Map<String, String> parameters) {
        logger.debug("Synchronizing reservations");
        String token;
        String user = parameters.get("user");
        String password = parameters.get("password");
        String url = parameters.get("url");
        if (url == null || url.isEmpty()) {
            logger.error("Reservation service URL not provided");
            return;
        }
        if (user == null || password == null || user.isEmpty() || password.isEmpty()) {
            logger.error("Credentials for reservation service not provided");
            return;
        }
        String syncDeviceEui = parameters.get("sync.device.eui");
        String syncDeviceAuthKey = parameters.get("sync.device.key");
        if (syncDeviceEui == null || syncDeviceAuthKey == null || syncDeviceEui.isEmpty() || syncDeviceAuthKey.isEmpty()) {
            logger.error("Sync device EUI or auth key not provided");
            return;
        }
        String roomPrefix = parameters.get("room.prefix");
        String roomKey = parameters.get("room.key");
        if (roomPrefix == null || roomKey == null || roomPrefix.isEmpty() || roomKey.isEmpty()) {
            logger.error("Room prefix or key not provided");
            return;
        }

        InteahotelReservationClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(url))
                .build(InteahotelReservationClient.class);
        Response response;
        try {
            response = client.login(new CredentialsDto(user, password));
            if (response.getStatus() != 200) {
                ErrorMessageDto errorMessage = response.readEntity(ErrorMessageDto.class);
                logger.error("Error logging in to reservation service: " + errorMessage.code + " - "
                        + errorMessage.message);
                signomixReceiverClient.sendSynchronizationStatus(syncDeviceAuthKey,
                        syncDeviceEui, 1, errorMessage.code + " - "
                                + errorMessage.message);
                return;
            } else {
                TokenDto tokenDto = response.readEntity(TokenDto.class);
                token = tokenDto.token;
                logger.debug("Logged in to reservation service");
            }
        } catch (Exception e) {
            logger.error("Error logging in to reservation service: " + e.getMessage());
            signomixReceiverClient.sendSynchronizationStatus(syncDeviceAuthKey,
                    syncDeviceEui, 1, e.getMessage());
            return;
        }

        RoomStatusResponse roomStatusResponse = null;
        try {
            if (updatedOnly) {
                response = client.getUpdatedRoomStatuses("Bearer " + token);
            } else {
                response = client.getRoomStatuses("Bearer " + token);
            }
            if (response.getStatus() != 200) {
                ErrorMessageDto errorMessage = response.readEntity(ErrorMessageDto.class);
                logger.error("Error getting room statuses: " + errorMessage.code + " - " + errorMessage.message);
                return;
            } else {
                roomStatusResponse = response.readEntity(RoomStatusResponse.class);
                if (logger.isDebugEnabled()) {
                    logger.debug("Got room statuses: " + roomStatusResponse.created_at);
                    logger.debug("Last status check: " + roomStatusResponse.last_status_check);
                    logger.debug("Room statuses: " + roomStatusResponse.data.size() + " rooms");
                }
            }
        } catch (Exception e) {
            logger.error("Error getting room statuses: " + e.getMessage());
            signomixReceiverClient.sendSynchronizationStatus(syncDeviceAuthKey,
                    syncDeviceEui, 1, e.getMessage());
            return;
        }
        if (roomStatusResponse == null || roomStatusResponse.data == null) {
            logger.error("Error getting room statuses: data list null");
            signomixReceiverClient.sendSynchronizationStatus(syncDeviceAuthKey,
                    syncDeviceEui, 1, "brak listy danych");
            return;
        } else if (roomStatusResponse.data.size() == 0 && !updatedOnly) {
            logger.error("Error getting room statuses: data list empty");
            signomixReceiverClient.sendSynchronizationStatus(syncDeviceAuthKey,
                    syncDeviceEui, 1, "brak danych");
            return;
        }
        RoomDataDto roomDataDto = null;
        int roomId;
        long timestamp = System.currentTimeMillis();
        logger.info("Got room statuses: " + roomStatusResponse.data.size() + " rooms");
        for (int i = 0; i < roomStatusResponse.data.size(); i++) {
            roomDataDto = roomStatusResponse.data.get(i);
            //roomId = transformRoomId(roomDataDto.room);
            try{
                roomId = Integer.parseInt(roomDataDto.number);
            } catch (Exception e) {
                logger.error("Error transforming room ID: " + roomDataDto.room + " - " + e.getMessage());
                continue; // skip this room if transformation fails
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Room " + roomId + " status: " + roomDataDto.status);
            }
            // save status to database
            saveRoomReservationStatus(roomPrefix, roomId, roomDataDto.status, timestamp);
            // create event - DISABLED
            createRoomDataEvent(roomPrefix, roomId, roomDataDto.status, timestamp);
        }
        signomixReceiverClient.sendSynchronizationStatus(syncDeviceAuthKey,
                syncDeviceEui, 0, "");
    }

    /**
     * Update room statuses
     */
    public void updateStatuses(Map<String, String> parameters) {
        logger.info("Updating room statuses");
        String roomGroup = parameters.get("room.group");
        String roomKey = parameters.get("room.key");
        if (roomGroup == null || roomKey == null || roomGroup.isEmpty() || roomKey.isEmpty()) {
            logger.error("Room group or key not provided");
            return;
        }
        ArrayList<Device> roomDevices = new ArrayList<>();
        try {
            roomDevices = (ArrayList) getIotDatabaseDao().getGroupDevices(roomGroup);
        } catch (IotDatabaseException e) {
            logger.error("Error getting devices from database");
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < roomDevices.size(); i++) {
            String roomEui = roomDevices.get(i).getEUI();
            signomixReceiverClient.sendRoomStatusCheck(roomKey, roomEui, 0);
        }
    }

    /**
     * Transform Inteahotel room ID to the hotel room ID
     * 
     * @param roomId room Id used in the Inteahotel system
     * @return room Id used in the hotel system or -1 if not found
     */
    private int transformRoomId(int roomId) {
        return roomMapper.getRoomMapping().getOrDefault(roomId, -1);
    }

}
