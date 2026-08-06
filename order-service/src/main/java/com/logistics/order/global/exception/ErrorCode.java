package com.logistics.order.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_CANCELED(HttpStatus.CONFLICT, "ORDER_409_01", "이미 취소된 주문입니다."),
    ORDER_STATUS_NOT_CHANGEABLE(HttpStatus.CONFLICT, "ORDER_409_02", "현재 상태에서는 주문을 변경할 수 없습니다."),
    ORDER_STATUS_NOT_DELETE(HttpStatus.CONFLICT, "ORDER_409_03", "주문 취소 후 삭제 해주세요"),
    INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_400_01", "주문 수량은 1개 이상이어야 합니다."),
    INVALID_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "ORDER_400_02", "유효하지 않은 주문 요청입니다."),
    INVALID_ORDER_UNIT_PRICE(HttpStatus.BAD_REQUEST, "ORDER_400_03", "단가는 0 이상이어야 합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER_403","해당 요청에 대한 권한이 없습니다."),

    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_ITEM_404", "주문 상품을 찾을 수 없습니다."),
    ORDER_ITEM_ALREADY_CANCELED(HttpStatus.CONFLICT, "ORDER_ITEM_409_01", "이미 취소된 주문 상품입니다."),
    ORDER_ITEM_STATUS_NOT_CANCELED(HttpStatus.CONFLICT, "ORDER_ITEM_409_02", "현재 상태에서는 주문 상품을 취소할 수 없습니다."),
    ORDER_ITEM_STATUS_NOT_CHANGEABLE(HttpStatus.CONFLICT, "ORDER_ITEM_409_03", "현재 상태에서는 주문 상품 확정을 할 수 없습니다."),
    ORDER_ITEM_NO_CONFIRMABLE(HttpStatus.CONFLICT, "ORDER_ITEM_409_04", "주문을 확정할 상품 이 없습니다."),
    ORDER_ALREADY_DELETED(HttpStatus.CONFLICT, "ORDER_409_04", "이미 삭제된 주문입니다."),

    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404_02", "업체 정보를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404_03", "상품 정보를 찾을 수 없습니다."),
    INVALID_COMPANY_RESPONSE(HttpStatus.BAD_GATEWAY, "ORDER_502_01", "업체 서비스의 응답이 올바르지 않습니다."),
    INVALID_PRODUCT_RESPONSE(HttpStatus.BAD_GATEWAY, "ORDER_502_02", "상품 서비스의 응답이 올바르지 않습니다."),
    EXTERNAL_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_503_01", "외부 서비스에 일시적으로 연결할 수 없습니다."),
    DUPLICATE_ORDER_PRODUCT(HttpStatus.BAD_REQUEST, "ORDER_400_05", "같은 상품을 중복해서 주문할 수 없습니다."),

    INVALID_DELIVERY_ID(HttpStatus.BAD_REQUEST, "ORDER_400_06", "배송 ID가 올바르지 않습니다."),
    DELIVERY_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "ORDER_409_03", "이미 다른 배송이 연결된 주문 상품입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_404_04", "사용자 정보를 찾을 수 없습니다."),
    INVALID_USER_RESPONSE(HttpStatus.BAD_GATEWAY, "ORDER_502_03", "사용자 서비스의 응답이 올바르지 않습니다."),
    INVALID_DELIVERY_RESPONSE(HttpStatus.BAD_GATEWAY, "ORDER_502_04", "배송 서비스의 응답이 올바르지 않습니다."),
    DELIVERY_CREATION_FAILED(HttpStatus.BAD_GATEWAY, "ORDER_502_05", "배송 생성에 실패했습니다."),
    DELIVERY_CANCELLATION_FAILED(HttpStatus.BAD_GATEWAY, "ORDER_502_06", "배송 취소에 실패했습니다.");

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
