package com.mealsnow.identity.auth;


import com.mealsnow.identity.User;
import com.mealsnow.identity.UserRepository;
import com.mealsnow.identity.auth.dto.AuthResponse;
import com.mealsnow.identity.auth.dto.LoginRequest;
import com.mealsnow.identity.auth.dto.RegisterRequest;
import com.mealsnow.identity.security.JwtService;
import com.mealsnow.identity.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()){
            throw  new IllegalArgumentException("Email already registered");
        }
        String hash = passwordEncoder.encode(request.password());
        String email = request.email();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hash);
        user.setRole(request.role());
        userRepository.save(user);
    }


    public AuthResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        Authentication auth = authenticationManager.authenticate(token);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();
        String jwt = jwtService.generateToken(
                user.getId().toString(),
                user.getRole().name()
        );
        return new AuthResponse(jwt);



        // 1. build a UsernamePasswordAuthenticationToken(email, password)
        // 2. authenticationManager.authenticate(...)  → throws if bad
        // 3. pull the authenticated UserPrincipal from the result
        // 4. jwtService.generateToken(userId, role)
        // 5. return new AuthResponse(token)
    }




}
