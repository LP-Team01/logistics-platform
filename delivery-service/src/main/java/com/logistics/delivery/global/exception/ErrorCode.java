package com.logistics.delivery.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    //DELIVERY
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_01", "배송 정보를 찾을 수 없습니다."),
    DELIVERY_STATUS_NOT_CHANGEABLE(HttpStatus.BAD_REQUEST, "DELIVERY_400_01", "유효하지 않은 상태 전이입니다."),
    DELIVERY_ORDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "DELIVERY_409_04", "주문아이템 1개에 배송 1개 생성 가능합니다."),
    DELIVERY_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_05", "배송 수정/삭제 권한이 없습니다."),
    DELIVERY_QUERY_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_06", "본인이 배정된 배송만 조회할 수 있습니다."),
    DELIVERY_INTERNAL_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_07", "내부 서비스 호출만 허용됩니다."),
    DELIVERY_HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_08", "배송 생성에 필요한 허브를 찾을 수 없습니다."),

    //DELIVERY_ROUTE & COMPANY_ROUTE
    INVALID_DELIVERY_ROUTE(HttpStatus.BAD_REQUEST, "DELIVERY_400", "유효하지 않은 배송 경로입니다."),
    DELIVERY_ROUTE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_03", "배송 경로 기록을 찾을 수 없습니다."),
    DELIVERY_ROUTE_RECORD_STATUS_NOT_CHANGEABLE(HttpStatus.BAD_REQUEST, "DELIVERY_400_03", "유효하지 않은 배송 경로 상태 전이입니다."),
    DELIVERY_ROUTE_RECORD_ACTUAL_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "DELIVERY_400_04", "허브 도착 처리 시 실제 거리와 실제 소요 시간은 필수입니다."),
    COMPANY_ROUTE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_04", "업체배송경로 기록을 찾을 수 없습니다."),
    COMPANY_ROUTE_RECORD_STATUS_NOT_CHANGEABLE(HttpStatus.BAD_REQUEST, "DELIVERY_400_05", "유효하지 않은 업체배송경로 상태 전이입니다."),
    COMPANY_ROUTE_RECORD_ACTUAL_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "DELIVERY_400_06", "배송 완료 처리 시 실제 거리와 실제 소요 시간은 필수입니다."),
    DELIVERY_ROUTE_RECORD_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_03", "본인이 배정된 경로만 조회/수정할 수 있습니다."),
    COMPANY_ROUTE_RECORD_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_04", "본인이 배정된 업체배송경로만 조회/수정할 수 있습니다."),

    //DELIVERY_AGENT
    DELIVERY_PERSON_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_02", "배송 담당자를 찾을 수 없습니다."),
    DELIVERY_PERSON_UNAVAILABLE(HttpStatus.NOT_FOUND, "DELIVERY_404_05", "배정 가능한 배송 담당자가 없습니다."),
    HUB_ID_REQUIRED(HttpStatus.BAD_REQUEST, "DELIVERY_400_02", "업체배송담당자는 소속 허브(hubId)가 필수입니다."),
    DELIVERY_AGENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "DELIVERY_409_03", "배송 담당자 총 인원은 최대 10명입니다."),
    DELIVERY_AGENT_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_01", "배송담당자 생성/수정 권한이 없습니다."),
    DELIVERY_AGENT_QUERY_FORBIDDEN(HttpStatus.FORBIDDEN, "DELIVERY_403_02", "배송담당자 조회 권한이 없습니다."),
    DELIVERY_AGENT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_06", "배송담당자로 등록할 사용자를 찾을 수 없습니다."),
    DELIVERY_AGENT_HUB_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_404_07", "배송담당자로 등록할 허브를 찾을 수 없습니다."),
    DELIVERY_AGENT_INVALID_USER_ROLE(HttpStatus.BAD_REQUEST, "DELIVERY_400_07", "해당 사용자는 배송담당자(DELIVERY_MANAGER) 역할이 아닙니다."),
    DELIVERY_AGENT_USER_NOT_APPROVED(HttpStatus.BAD_REQUEST, "DELIVERY_400_08", "승인(APPROVED)되지 않은 사용자는 배송담당자로 등록할 수 없습니다."),
    DELIVERY_AGENT_GROUP_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DELIVERY_400_09", "배송담당자의 소속 허브/타입은 수정할 수 없습니다. 그룹을 변경하려면 삭제 후 다시 등록하세요."),
    DELIVERY_AGENT_ORDER_CONFLICT(HttpStatus.CONFLICT, "DELIVERY_409_05", "배송담당자 순번 처리 중 충돌이 발생했습니다. 다시 시도해주세요.");

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
