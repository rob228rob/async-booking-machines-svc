package com.mai.db_cw.dormitory;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Dormitory {
    private UUID id;
    private String name;
    private String address;
    private LocalDateTime creationTime;
    private LocalDateTime modifiedTime;
}