package com.logistics.user.auth.service;

import com.logistics.user.auth.dto.LoginRequestDto;
import com.logistics.user.auth.dto.TokenResponseDto;
import com.logistics.user.auth.util.CookieUtil;
import com.logistics.user.auth.util.JwtUtil;
import com.logistics.user.global.exception.BusinessException;
import com.logistics.user.global.exception.ErrorCode;
import com.logistics.user.global.redis.RedisService;
import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserStatus;
import com.logistics.user.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // Token 식별자
    private static final String TOKEN_TYPE = "Bearer";
    private final RedisService redisService;
    private final JwtUtil jwtUtil;



    public TokenResponseDto login(LoginRequestDto requestDto, HttpServletResponse servletResponse) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(requestDto.username())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_OR_USERNAME));

        switch (user.getStatus()){
            case REJECTED -> {
                throw new BusinessException(ErrorCode.USER_REJECTED);
            }
            case PENDING -> {
                throw new BusinessException(ErrorCode.USER_PENDING);
            }
        }

        if(!passwordEncoder.matches(requestDto.password(), user.getPassword())){
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_OR_USERNAME);
        }

        String accessToken = jwtUtil.createAccessToken(
            user.getUserId(),
            user.getHubId(),
            user.getCompanyId(),
            user.getRole()
        );
        String refreshToken = jwtUtil.createRefreshToken(
            user.getUserId(),
            user.getUsername(),
            user.getRole()
        );

        Duration refreshTokenTtl = jwtUtil.getRefreshTokenExpiration();

        redisService.setValueTtl(user.getUserId(), refreshToken, refreshTokenTtl);

        CookieUtil.addCookie(servletResponse, "refreshToken", refreshToken, (int) refreshTokenTtl.getSeconds());

        return new TokenResponseDto(
            accessToken,
            TOKEN_TYPE,
            (int) jwtUtil.getAccessTokenExpirationSeconds()
        );
    }

    public TokenResponseDto reIssue(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = CookieUtil.getCookie(request, "refreshToken").orElseThrow(
            () -> new BusinessException(ErrorCode.COOKIE_NOT_FOUND)
        );
        Claims claims = jwtUtil.parseClaims(cookie.getValue());
        if(claims == null){
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        UUID userId = UUID.fromString(claims.getSubject());

        String savedRefreshToken = redisService.getValue(userId);

        if(savedRefreshToken == null){
            throw new BusinessException(ErrorCode.REFRESHTOKEN_NOT_FOUND);
        }

        if(!savedRefreshToken.equals(cookie.getValue())){
            throw new BusinessException(ErrorCode.REFRESHTOKEN_NOT_MATCHED);
        }

        // 인증 성공 후 재발급
        User user = userRepository.findByUserIdAndDeletedAtIsNullAndStatus(userId, UserStatus.APPROVED)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String newAccessToken = jwtUtil.createAccessToken(userId, user.getHubId(), user.getCompanyId(), user.getRole());
        // RTR 기법
        redisService.delValue(userId);
        String newRefreshToken = jwtUtil.createRefreshToken(userId, user.getUsername(), user.getRole());
        Duration refreshTokenTtl = jwtUtil.getRefreshTokenExpiration();
        redisService.setValueTtl(userId, newRefreshToken, refreshTokenTtl);
        CookieUtil.addCookie(response, "refreshToken", newRefreshToken, (int)refreshTokenTtl.getSeconds());

        return new TokenResponseDto(
            newAccessToken,
            TOKEN_TYPE,
            (int)jwtUtil.getAccessTokenExpirationSeconds()
        );
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = CookieUtil.getCookie(request, "refreshToken").orElseThrow(
            () -> new BusinessException(ErrorCode.COOKIE_NOT_FOUND)
        );

        String token = cookie.getValue();
        Claims claims = jwtUtil.parseClaims(token);
        if(claims == null){
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        UUID userId = UUID.fromString(claims.getSubject());

        String savedRefreshToken = redisService.getValue(userId);

        if(savedRefreshToken == null){
            throw new BusinessException(ErrorCode.REFRESHTOKEN_NOT_FOUND);
        }

        if(!savedRefreshToken.equals(cookie.getValue())){
            throw new BusinessException(ErrorCode.REFRESHTOKEN_NOT_MATCHED);
        }

        redisService.delValue(userId);

        CookieUtil.deleteCookie(request, response, "refreshToken");
    }
}
