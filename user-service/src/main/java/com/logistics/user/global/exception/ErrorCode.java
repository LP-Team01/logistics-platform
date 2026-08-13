package com.logistics.user.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "USER_409_01", "이미 사용 중인 아이디입니다."),
    DUPLICATE_SLACK_ID(HttpStatus.CONFLICT, "USER_409_02", "이미 등록된 Slack ID입니다."),
    USER_REJECTED(HttpStatus.UNAUTHORIZED, "USER_401_01", "거절된 사용자입니다."),
    USER_PENDING(HttpStatus.UNAUTHORIZED, "USER_401_02", "아직 가입 승인 대기중입니다."),
    INVALID_PASSWORD_OR_USERNAME(HttpStatus.UNAUTHORIZED, "AUTH_401_01", "아이디 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401_02", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_403", "접근 권한이 없습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST,"USER_400" , "권한에 맞는 입력이 아닙니다."),
    ALREADY_PROCESSED_SIGNUP(HttpStatus.CONFLICT, "USER_409_03", "이미 처리된 가입 요청입니다." ),
    COOKIE_NOT_FOUND(HttpStatus.NOT_FOUND,"AUTH_404_01" , "쿠키가 없습니다."),
    REFRESHTOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,"AUTH_401_03" , "현재 가진 토큰은 권한이 없습니다."),
    REFRESHTOKEN_NOT_MATCHED(HttpStatus.UNAUTHORIZED,"AUTH_401_04" , "토큰이 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,"AUTH_401_05" , "유효한 토큰이 아닙니다." ),
    HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_01", "존재하지 않는 허브입니다."),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_02", "존재하지 않는 업체입니다."),
    HUB_COMPANY_MISMATCH(HttpStatus.BAD_REQUEST, "USER_400_01", "요청한 허브와 업체가 실제로 속한 허브가 일치하지 않습니다."),
    DELIVERY_AGENT_NOT_APPROVED(HttpStatus.CONFLICT, "USER_409_04", "배송담당자 등록 처리 중 승인 상태 확인에 실패했습니다."),
    DELIVERY_AGENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "USER_409_05", "허브의 배송담당자 정원이 초과되었습니다."),
    DELIVERY_AGENT_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "USER_502_01", "배송담당자 등록에 실패했습니다.");

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
