package com.mealsnow.identity.auth.dto;

import com.mealsnow.identity.Role;

public record RegisterRequest(String email, String password, Role role) {}
