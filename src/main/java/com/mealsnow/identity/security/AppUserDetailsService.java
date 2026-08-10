package com.mealsnow.identity.security;

import com.mealsnow.identity.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).map(UserPrincipal::new).orElseThrow(() -> new UsernameNotFoundException(email)); // TODO: findByEmail → wrap in UserPrincipal, or throw UsernameNotFoundException
    }
}