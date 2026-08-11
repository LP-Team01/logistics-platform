package com.logistics.user.auth.controller;

import com.logistics.user.auth.dto.LoginRequestDto;
import com.logistics.user.auth.dto.TokenResponseDto;
import com.logistics.user.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody @Valid LoginRequestDto requestDto, HttpServletResponse servletResponse){
        TokenResponseDto responseDto = authService.login(requestDto, servletResponse);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/re-issue")
    public ResponseEntity<TokenResponseDto> reIssue(HttpServletRequest request, HttpServletResponse response){
        TokenResponseDto responseDto = authService.reIssue(request, response);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);
        return ResponseEntity.ok().build();
    }
}
