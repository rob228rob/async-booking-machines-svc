package com.mai.db_cw.rest.user;

import java.time.LocalDateTime;
import java.util.*;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private boolean enabled;

    private boolean tokenExpired;

    private LocalDateTime creationTime;

    private LocalDateTime modifiedTime;
}
