package com.mai.db_cw.config.infrastructure.exceptions;

public class UserRoleNotFoundException extends RuntimeException {
    public UserRoleNotFoundException(String roleNotFound) {
        super(roleNotFound);
    }
}
