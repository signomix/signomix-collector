package com.signomix.collector.app.ports.driven;

import com.signomix.common.db.IotDatabaseException;

import io.agroal.api.AgroalDataSource;

public interface ForAccessStatusDatabaseIface {

    public void setDatasource(AgroalDataSource dataSource);
    public void createStructure() throws IotDatabaseException;
    public Integer getRoomReservationStatus(String roomEUI) throws IotDatabaseException;
    public void setRoomReservationStatus(String roomEUI, Integer status, long timestamp) throws IotDatabaseException;
    public void addSyncLogEntry(String url, int code, String message) throws IotDatabaseException;

}
