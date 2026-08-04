package com.logistics.order.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_CANCELED(HttpStatus.CONFLICT, "ORDER_409_01", "이미 취소된 주문입니다."),
    ORDER_STATUS_NOT_CHANGEABLE(HttpStatus.CONFLICT, "ORDER_409_02", "현재 상태에서는 주문을 변경할 수 없습니다."),
    INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_400_01", "주문 수량은 1개 이상이어야 합니다."),
    INVALID_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "ORDER_400_02", "유효하지 않은 주문 요청입니다.");

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
