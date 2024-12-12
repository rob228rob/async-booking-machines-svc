package com.mai.db_cw.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mai.db_cw.infrastructure.exceptions.InvalidUserInfoException;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRegistrationRequest {

    @JsonProperty(value = "first_name", required = true)
    @NotBlank(message = "First name is mandatory")
    private String firstName;

    @JsonProperty(value = "last_name", required = true)
    @NotBlank(message = "Last name is mandatory")
    private String lastName;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is mandatory")
    @JsonProperty(value = "email", required = true)
    private String email;

    @JsonProperty(value = "password", required = true)
    @NotBlank(message = "Password is mandatory")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @JsonProperty(value = "confirm_password", required = true)
    @NotBlank(message = "Confirm Password is mandatory")
    private String confirmPassword;

    @JsonProperty(value = "role", required = true)
    @NotBlank(message = "Role is mandatory")
    @Pattern(regexp = "ROLE_[A-Z]+", message = "Role must follow the pattern ROLE_<ROLE_NAME>")
    private String role;

    public UserRegistrationRequest(UserRegistrationRequest userRegistrationRequest) {
        this.firstName = userRegistrationRequest.getFirstName();
        this.lastName = userRegistrationRequest.getLastName();
        this.email = userRegistrationRequest.getEmail();
        this.password = userRegistrationRequest.getPassword();
        this.confirmPassword = userRegistrationRequest.getConfirmPassword();
        this.role = userRegistrationRequest.getRole();
    }

    public void validate() {
        this
                .throwIfNameAndSurnameInvalid()
                .throwIfEmailInvalid()
                .throwIfPasswordInvalid()
                .throwIfRoleInvalid();
    }

    private UserRegistrationRequest throwIfRoleInvalid() {
        if (this.role == null || this.role.trim().isEmpty()) {
            this.role = "ROLE_USER";
        }

        return this;
    }

    private UserRegistrationRequest throwIfNameAndSurnameInvalid() {
        if (this.getFirstName() == null || this.getFirstName().length() < 3) {
            throw new InvalidUserInfoException("Имя должно содержать минимум 3 символа");
        }
        if (this.getLastName() == null || this.getLastName().length() < 2) {
            throw new InvalidUserInfoException("Фамилия должна содержать минимум 2 символа");
        }
        return this;
    }

    private UserRegistrationRequest throwIfEmailInvalid() {
        String email = this.email;

        // Простая проверка email на корректность
        if (email == null || !email.matches("^[\\w-.]+@[a-zA-Z]+\\.[a-zA-Z]{2,}$")) {
            throw new InvalidUserInfoException("Некорректный email");
        }

        return this;
    }

    private UserRegistrationRequest throwIfPasswordInvalid() {
        if (this.getPassword() == null || this.getConfirmPassword() == null) {
            throw new InvalidUserInfoException("Пароль и его подтверждение обязательны для заполнения");
        }

        if (this.getPassword().length() < 8) {
            throw new InvalidUserInfoException("Пароль должен содержать минимум 8 символов");
        }

        if (!this.getPassword().equals(this.getConfirmPassword())) {
            throw new InvalidUserInfoException("Пароль и подтвержденный пароль не совпадают");
        }

        return this;
    }
}

