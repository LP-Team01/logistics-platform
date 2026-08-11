package com.logistics.gateway.util;

import com.logistics.gateway.global.exception.BusinessException;
import com.logistics.gateway.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private final Key signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil, "secretKey", Base64.getEncoder().encodeToString(signingKey.getEncoded())
        );
        jwtUtil.init();
    }

    // Authorization 헤더에서 Bearer 접두사를 제거한 토큰을 추출하는지 검증
    @Test
    void shouldExtractBearerTokenFromAuthorizationHeader() {
        String token = jwtUtil.getJwtFromHeader(
                MockServerHttpRequest.get("/")
                        .header("Authorization", "Bearer access-token")
                        .build()
        );

        assertThat(token).isEqualTo("access-token");
    }

    // 만료된 JWT를 EXPIRED_TOKEN 오류로 처리하는지 검증
    @Test
    void shouldRejectExpiredJwtWithExpiredTokenError() {
        String token = token(signingKey, new Date(System.currentTimeMillis() - 1_000));

        assertThatThrownBy(() -> jwtUtil.parseClaim(token))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN)
                );
    }

    // 다른 키로 서명된 JWT를 INVALID_TOKEN 오류로 처리하는지 검증
    @Test
    void shouldRejectJwtSignedWithDifferentKey() {
        Key otherKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String token = token(otherKey, new Date(System.currentTimeMillis() + 60_000));

        assertThatThrownBy(() -> jwtUtil.parseClaim(token))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN)
                );
    }

    // JWT가 없을 때 EMPTY_TOKEN 오류로 처리하는지 검증
    @Test
    void shouldRejectMissingTokenWithEmptyTokenError() {
        assertThatThrownBy(() -> jwtUtil.parseClaim(null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPTY_TOKEN)
                );
    }

    private String token(Key key, Date expiration) {
        return Jwts.builder()
                .setSubject("user-1")
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
