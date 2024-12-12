package com.mai.db_cw.machines.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MachineResponse {
    private UUID id;
    private String name;
    private String dormitoryName;
    private String machineType;
}
