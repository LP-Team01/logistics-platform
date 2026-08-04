package com.logistics.delivery.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    //DELIVERY
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_01", "배송 정보를 찾을 수 없습니다."),
    DELIVERY_STATUS_NOT_CHANGEABLE(HttpStatus.CONFLICT, "DELIVERY_409_02", "현재 상태에서는 배송 상태를 변경할 수 없습니다."),

    //DELIVERY_ROUTE & COMPANY_ROUTE
    INVALID_DELIVERY_ROUTE(HttpStatus.BAD_REQUEST, "DELIVERY_400", "유효하지 않은 배송 경로입니다."),

    //DELIVERY_AGENT
    DELIVERY_PERSON_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_02", "배송 담당자를 찾을 수 없습니다."),
    DELIVERY_PERSON_UNAVAILABLE(HttpStatus.CONFLICT, "DELIVERY_409_01", "배정 가능한 배송 담당자가 없습니다."),
    HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "DELIVERY_400_02", "업체배송담당자는 소속 허브(hubId)가 필수입니다."),
    DELIVERY_AGENT_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_01", "배송담당자 생성/수정 권한이 없습니다.");

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
