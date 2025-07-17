package com.signomix.collector.app.logic.inteahotel.dto;

public class ReservationDto {
    public Integer roomId;
    public Integer status;

    public ReservationDto() {
    }

    public ReservationDto(Integer roomId, Integer status) {
        this.roomId = roomId;
        this.status = status;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}
