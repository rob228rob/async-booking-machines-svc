package com.mai.db_cw.coworkings.dto;

import java.util.UUID;

public record CoworkingRequest(
        UUID dormitoryId,
        String name,
        Integer type
) {

}
