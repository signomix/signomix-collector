package com.signomix.collector.adapters.driven;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.jboss.logging.Logger;

import com.signomix.collector.app.ports.driven.ForAccessStatusDatabaseIface;
import com.signomix.common.db.IotDatabaseException;
import com.signomix.common.tsdb.IotDatabaseDao;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatusDatabaseAdapter extends IotDatabaseDao implements ForAccessStatusDatabaseIface {

    @Inject
    Logger logger;

    private AgroalDataSource datasource;

    @Override
    public void setDatasource(AgroalDataSource dataSource) {
        this.datasource = dataSource;
        // super.setDatasource(dataSource);
    }

    /**
     * Read status column name from 'devicechannels' table
     * 
     * @return column name
     */
    private String getChannelColumnName(String deviceEui,String channel) throws IotDatabaseException {
        String sql = "SELECT channels FROM devicechannels where eui=?";
        String channels;
        try (var conn = datasource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceEui);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                channels = rs.getString("channels");
            } else {
                throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION,
                        "No channel found for device " + deviceEui);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage());
        }
        if (channels == null) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION,
                    "No channel found for device " + deviceEui);
        }
        String[] channelArray = channels.split(",");
        int statusIndex = -1;
        for (int i = 0; i < channelArray.length; i++) {
            if (channelArray[i].equals("status")) {
                statusIndex = i;
                break;
            }
        }
        if (statusIndex == -1) {
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION,
                    "No status channel found for device " + deviceEui);
        }
        return "d" + (statusIndex + 1);
    }

    private String getUserUid() {
        return "tester1";

    }

    @Override
    public Integer getRoomReservationStatus(String roomEUI) throws IotDatabaseException {
        Integer status = null;
        String columnName = getChannelColumnName(roomEUI, "status");
        String sql = "SELECT eui," + columnName
                + " FROM analyticdata where eui=? and "+columnName+" is NOT NULL order by tstamp desc limit 1";
        try (var conn = datasource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomEUI);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                Double d = rs.getDouble(columnName);
                if (rs.wasNull()) {
                    status = null;
                } else {
                    status = d.intValue();
                }
                status = d.intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage());
        }
        return status;
    }

    @Override
    public void setRoomReservationStatus(String roomEUI, Integer status, long timestamp) throws IotDatabaseException {
        /*
         * // find device by roomId
         * Device device = getDevice(roomEUI, false);
         * device.setEUI(roomEUI);
         * 
         * // analytic data
         * ChannelData data = new ChannelData("status", (long)status ,
         * System.currentTimeMillis());
         * ArrayList<ChannelData> dataList = new ArrayList<>();
         * dataList.add(data);
         * 
         * // virtual data
         * HashMap<String, Double> values = new HashMap<>();
         * values.put("status", (double)status);
         * VirtualData virtualData = new VirtualData();
         * virtualData.eui = roomEUI;
         * virtualData.payload_fields = values;
         * virtualData.timestamp = System.currentTimeMillis();
         * 
         * putData(device, dataList);
         * putVirtualData(device, virtualData);
         */

        String sql = "INSERT INTO analyticdata (eui, userid, tstamp, " + getChannelColumnName(roomEUI, "status")
                + ") VALUES (?, ?, ?, ?)";
        try (var conn = datasource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            Timestamp ts = new Timestamp(timestamp);
            stmt.setString(1, roomEUI);
            stmt.setString(2, getUserUid());
            stmt.setTimestamp(3, ts);
            stmt.setDouble(4, status);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage());
        }
    }

    @Override
    public void createStructure() throws IotDatabaseException {
        // create table synchronization_log if not exists with columns:
        // id(big serial), url, code, message, timestamp (timestamptz) primary key (id)
        String sql = "CREATE TABLE IF NOT EXISTS synchronization_log "
                + "(id BIGSERIAL PRIMARY KEY, url VARCHAR(255), code INT, message VARCHAR(255), timestamp TIMESTAMPTZ NOT NULL DEFAULT now())";
        try (var conn = datasource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage());
        }
        // index on timestamp
        sql = "CREATE INDEX IF NOT EXISTS idx_timestamp ON synchronization_log (timestamp)";
        try (var conn = datasource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage());
        }
        //set table `synchronization_log` a TimestacleDB hyper table
        sql = "SELECT create_hypertable('synchronization_log', 'timestamp')";
        try (var conn = datasource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        // retention policy
        String query = "SELECT remove_retention_policy('synchronization_log');";
        try (Connection conn = datasource.getConnection(); PreparedStatement pst = conn.prepareStatement(query);) {
            pst.execute();
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        }
        query = "SELECT add_retention_policy('synchronization_log', INTERVAL '1 month');";
        try (Connection conn = datasource.getConnection(); PreparedStatement pst = conn.prepareStatement(query);) {
            pst.execute();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        
    }

    @Override
    public void addSyncLogEntry(String url, int code, String message) throws IotDatabaseException {
        String sql = "INSERT INTO synchronization_log (url, code, message) VALUES (?, ?, ?)";
        try (var conn = datasource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, url);
            stmt.setInt(2, code);
            stmt.setString(3, message);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IotDatabaseException(IotDatabaseException.SQL_EXCEPTION, e.getMessage());
        }
    }

}
