package com.mai.db_cw.infrastructure.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserDto {

    @NotNull
    @JsonProperty(value = "email", required = true)
    private String email;

    @NotNull
    @JsonProperty(value = "password", required = true)
    private String password;
}
