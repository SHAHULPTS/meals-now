package com.mealsnow.identity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/me")
    public String me(@AuthenticationPrincipal String userId) {
        return "You are authenticated as user: " + userId;
    }

    @GetMapping("/admin/hello")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin() {
        return "Hello ADMIN";
    }

    @GetMapping("/customer/hello")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customer() {
        return "Hello CUSTOMER";
    }
}