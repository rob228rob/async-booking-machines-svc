package com.mai.db_cw.machines.dto;

import java.util.UUID;

public record MachineRequest(
        UUID dormitoryId,
        String name,
        Integer type
) {

}
