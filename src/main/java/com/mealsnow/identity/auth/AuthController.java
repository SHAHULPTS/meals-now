package com.mealsnow.identity.auth;

import com.mealsnow.identity.User;
import com.mealsnow.identity.auth.dto.AuthResponse;
import com.mealsnow.identity.auth.dto.LoginRequest;
import com.mealsnow.identity.auth.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody RegisterRequest request) {
        authService.register(request);
    }


    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

}
