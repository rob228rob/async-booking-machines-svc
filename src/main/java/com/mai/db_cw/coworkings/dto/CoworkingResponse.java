package com.mai.db_cw.coworkings.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CoworkingResponse {
    private UUID id;
    private String name;
    private String locationName;
    private String coworkingType;
}
