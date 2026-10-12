package com.hxy.hxailife.controller;

import com.hxy.hxailife.auth.AuthService;
import com.hxy.hxailife.auth.dto.LoginDTO;
import com.hxy.hxailife.auth.dto.RegisterDTO;
import com.hxy.hxailife.auth.vo.UserAuthVO;
import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserAuthVO register(@Valid @RequestBody RegisterDTO request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserAuthVO login(@Valid @RequestBody LoginDTO request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @SaCheckLogin
    public void logout() {
        authService.logout();
    }

    @GetMapping("/me")
    @SaCheckLogin
    public UserAuthVO currentUser() {
        return authService.currentUser();
    }
}
