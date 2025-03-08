package com.mai.db_cw.infrastructure.auth;

import com.mai.db_cw.infrastructure.auth.dto.ErrorResponseDto;
import com.mai.db_cw.infrastructure.auth.dto.LoginUserDto;
import com.mai.db_cw.infrastructure.exceptions.UserAlreadyExistException;
import com.mai.db_cw.infrastructure.user_details.CustomUserDetailsService;
import com.mai.db_cw.user.dto.UserRegistrationRequest;
import com.mai.db_cw.user.dto.UserResponse;
import com.mai.db_cw.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @GetMapping
    public String healthCheck() {
        return "OK";
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest req,
                                          HttpServletRequest request) {
        if (userService.existsByEmail(req.getEmail())) {
            throw new UserAlreadyExistException("User with email: " + req.getEmail());
        }
        try {
            UserResponse userResponse = userService.saveUser(req);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponseDto("пупупу", HttpStatus.BAD_REQUEST.value())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginUserDto loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return ResponseEntity.ok().build();
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }
    }

    @GetMapping("/redirect")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> oauth2Callback(Authentication authentication) {
        return ResponseEntity.ok("OAuth2 authentication successful for user: " + authentication.getName());
    }
}