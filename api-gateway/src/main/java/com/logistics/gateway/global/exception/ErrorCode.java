package com.logistics.gateway.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "GATEWAY_401_01", "유효하지 않은 인증 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "GATEWAY_401_02", "만료된 인증 토큰입니다."),
    EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "GATEWAY_401_03" , "JWT claims is empty, 잘못된 JWT 토큰 입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "GATEWAY_401_04", "Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "GATEWAY_403", "접근 권한이 없습니다."),
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "GATEWAY_404", "요청한 서비스를 찾을 수 없습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "GATEWAY_503", "서비스에 일시적으로 연결할 수 없습니다."),
    GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "GATEWAY_502", "서비스 요청 처리 중 오류가 발생했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
