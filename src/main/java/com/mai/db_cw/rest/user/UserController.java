package com.mai.db_cw.rest.user;

import java.security.Principal;
import java.util.UUID;

import com.mai.db_cw.rest.user.dto.UpdateUserDto;
import com.mai.db_cw.rest.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole(\"ADMIN\")")
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        var userById = userService.findUserByIdReturningDto(id);

        return new ResponseEntity<>(userById, HttpStatus.OK);
    }

    @GetMapping("/get/current")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        String email = principal.getName();

        return ResponseEntity.ok(userService.findUserByEmailReturningDto(email));
    }

    @PreAuthorize("hasRole(\"ADMIN\")")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUserByUserId(@PathVariable UUID id) {
        userService.deleteUserByUserId(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/current")
    public ResponseEntity<?> deleteCurrentUser(Principal principal) {
        userService.deleteCurrentUser(principal.getName());

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole(\"ROLE_ADMIN\", \"ROLE_STAFF\")")
    @PatchMapping("/update/{id}")
    public ResponseEntity<?> updateUserByUserId(@RequestBody @Valid UpdateUserDto updateUserDto, @PathVariable UUID id) {
        userService.updateUserPartly(updateUserDto, id);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/update/current")
    public ResponseEntity<UserResponse> updateCurrentUser(@RequestBody @Valid UpdateUserDto userDto, Principal principal) {
        String email = principal.getName();
        UserResponse userResponse = userService.updateUserPartly(userDto, email);
        return ResponseEntity.ok().body(userResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all")
    public ResponseEntity<?> getAllUsers(@RequestParam long limit){
        return ResponseEntity.ok(userService.findAllWithLimit(limit));
    }
}
