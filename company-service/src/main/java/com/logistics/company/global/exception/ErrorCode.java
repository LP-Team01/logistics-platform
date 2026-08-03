package com.logistics.company.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_404", "업체를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_404", "상품을 찾을 수 없습니다."),
    DUPLICATE_COMPANY(HttpStatus.CONFLICT, "COMPANY_409", "이미 등록된 업체입니다."),
    DUPLICATE_PRODUCT(HttpStatus.CONFLICT, "PRODUCT_409", "이미 등록된 상품입니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "PRODUCT_409_02", "상품 재고가 부족합니다."),
    INVALID_COMPANY_TYPE(HttpStatus.BAD_REQUEST, "COMPANY_400", "유효하지 않은 업체 유형입니다.");

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
