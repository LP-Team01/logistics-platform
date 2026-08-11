package com.logistics.user.auth.controller;

import com.logistics.user.auth.dto.LoginRequestDto;
import com.logistics.user.auth.dto.TokenResponseDto;
import com.logistics.user.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증", description = "로그인, 토큰 재발급, 로그아웃 API")
public class AuthController {

    private final AuthService authService;


    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하고 액세스/리프레시 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody @Valid LoginRequestDto requestDto, HttpServletResponse servletResponse){
        TokenResponseDto responseDto = authService.login(requestDto, servletResponse);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다.")
    @PostMapping("/re-issue")
    public ResponseEntity<TokenResponseDto> reIssue(HttpServletRequest request, HttpServletResponse response){
        TokenResponseDto responseDto = authService.reIssue(request, response);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "로그아웃", description = "발급된 토큰을 무효화하고 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);
        return ResponseEntity.ok().build();
    }
}
