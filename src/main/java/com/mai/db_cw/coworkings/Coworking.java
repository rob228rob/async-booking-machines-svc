package com.mai.db_cw.coworkings;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
public class Coworking {
    private UUID id;
    private UUID dormitoryId;
    private Integer machineTypeId;
    private String name;
    private LocalDateTime creationTime;
    private LocalDateTime modifiedTime;
}
