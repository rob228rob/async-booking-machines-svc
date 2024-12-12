package com.mai.db_cw.reservation.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class ReservationUserResponse implements Serializable {
    private UUID machineId;
    private UUID reservationId;
    private String machineName;
    private String dormitoryName;
    private String reservationDate;
    //private String dayOfWeek;
    private String reservationTime;
    private String status;
}
