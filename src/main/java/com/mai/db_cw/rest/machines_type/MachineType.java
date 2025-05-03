package com.mai.db_cw.rest.machines_type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineType {
    private Integer id;
    private String name;
}
