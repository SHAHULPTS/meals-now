package com.mealsnow.identity.security;

import com.mealsnow.identity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class UserPrincipal implements UserDetails {
    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    public User getUser() {
        return user;
    }

    // --- your turn below ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public boolean isAccountNonExpired() {

        return true; // TODO
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // TODO
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // TODO
    }

    @Override
    public boolean isEnabled() {
        return true; // TODO
    }

}
