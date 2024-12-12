package com.mai.db_cw.infrastructure.auth;

import com.mai.db_cw.infrastructure.auth.dto.ErrorResponseDto;
import com.mai.db_cw.infrastructure.auth.dto.LoginUserDto;
import com.mai.db_cw.infrastructure.exceptions.UserAlreadyExistException;
import com.mai.db_cw.infrastructure.user_details.CustomUserDetailsService;
import com.mai.db_cw.user.dto.UserRegistrationRequest;
import com.mai.db_cw.user.dto.UserResponse;
import com.mai.db_cw.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
    /**
     * Регистрация нового пользователя.
     *
     * Создает нового пользователя в системе. Требуются логин, пароль и подтверждение пароля.
     *
     * <p><b>HTTP Responses:</b></p>
     * <ul>
     *   <li><b>200 OK</b>: Пользователь успешно зарегистрирован.
     *       <ul>
     *           <li>Тело ответа: {@link UserResponse}</li>
     *       </ul>
     *   </li>
     *   <li><b>409 Conflict</b>: Пользователь с таким логином уже существует.
     *       <ul>
     *           <li>Тело ответа: {@link ErrorResponseDto}</li>
     *       </ul>
     *   </li>
     *   <li><b>401 Unauthorized</b>: Неверные данные пользователя.
     *       <ul>
     *           <li>Тело ответа: {@link ErrorResponseDto}</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param userRegistrationRequest данные для регистрации пользователя
     * @param request                 HTTP-запрос
     * @return Ответ с информацией о пользователе или ошибкой
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid
            @RequestBody
            UserRegistrationRequest userRegistrationRequest,
            HttpServletRequest request) {
        throwIfUserAlreadyExists(userRegistrationRequest);

        try {
            UserResponse userResponse = userService.saveUser(userRegistrationRequest);

            // нужно чтобы аутентифицировать пользователя сразу и добавить в секьюрити контекст
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userRegistrationRequest.getEmail(),
                            userRegistrationRequest.getPassword()
                    )
            );

            // добавление в сам контекст
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponseDto(e.getMessage(), HttpStatus.BAD_REQUEST.value()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Авторизация пользователя.
     * Авторизация пользователя с использованием логина и пароля.
     *
     * <p><b>HTTP Responses:</b></p>
     * <ul>
     *   <li><b>200 OK</b>: Успешная авторизация.</li>
     *   <li><b>404 Not Found</b>: Пользователь не найден.
     *       <ul>
     *           <li>Тело ответа: {@link ErrorResponseDto}</li>
     *       </ul>
     *   </li>
     *   <li><b>401 Unauthorized</b>: Неверный логин или пароль.
     *       <ul>
     *           <li>Тело ответа: Строка с сообщением об ошибке</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param loginRequest данные для авторизации пользователя
     * @return Ответ с подтверждением авторизации или ошибкой
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginUserDto loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            var user = userService.findUserByEmailReturningDto(loginRequest.getEmail());

            return ResponseEntity.ok().build();
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }
    }

    /**
     * Проверяет, существует ли пользователь с указанным email.
     *
     * @param userRegistrationRequest данные для регистрации пользователя
     * @throws UserAlreadyExistException если пользователь уже существует
     */
    private void throwIfUserAlreadyExists(@Valid UserRegistrationRequest userRegistrationRequest) {
        if (userService.existsByEmail(userRegistrationRequest.getEmail())) {
            throw new UserAlreadyExistException("User with email: " + userRegistrationRequest.getEmail());
        }
    }
}