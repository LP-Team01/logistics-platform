package com.logistics.hub.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_404_01", "허브를 찾을 수 없습니다."),
    HUB_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "HUB_404_02", "허브 이동 경로를 찾을 수 없습니다."),
    DUPLICATE_HUB(HttpStatus.CONFLICT, "HUB_409_01", "이미 등록된 허브입니다."),
    DUPLICATE_HUB_ROUTE(HttpStatus.CONFLICT, "HUB_409_02", "이미 등록된 허브 이동 경로입니다."),
    SAME_HUB_ROUTE(HttpStatus.BAD_REQUEST, "HUB_400_01", "출발 허브와 도착 허브는 같을 수 없습니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "HUB_400_02", "페이지 크기는 10, 30, 50 중 하나여야 합니다."),
    INVALID_UPDATE_REQUEST(HttpStatus.BAD_REQUEST, "HUB_400_03", "수정할 값이 없습니다. distance 또는 duration 중 하나는 입력해야 합니다."),
    PATH_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "HUB_422_01", "출발 허브에서 도착 허브까지 도달 가능한 경로가 없습니다."),
    INTERNAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "HUB_403_01", "내부 서비스 인증에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
