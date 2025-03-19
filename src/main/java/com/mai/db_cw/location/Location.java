package com.mai.db_cw.location;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Location {
    private UUID id;
    private String name;
    private String address;
    private LocalDateTime creationTime;
    private LocalDateTime modifiedTime;
}