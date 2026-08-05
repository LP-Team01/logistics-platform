package com.logistics.ai.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_AI_REQUEST(
        HttpStatus.BAD_REQUEST,
        "AI_400",
        "유효하지 않은 AI 요청입니다."
    ),

    INVALID_STATISTICS_PERIOD(
        HttpStatus.BAD_REQUEST,
        "AI_400_01",
        "조회 시작일은 종료일보다 늦을 수 없습니다."
    ),

    AI_REQUEST_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "AI_404",
        "AI 요청을 찾을 수 없습니다."
    ),

    AI_REQUEST_RETRY_NOT_ALLOWED(
        HttpStatus.CONFLICT,
        "AI_409",
        "실패한 AI 요청만 재처리할 수 있습니다."
    ),

    AI_RESPONSE_FAILED(
        HttpStatus.BAD_GATEWAY,
        "AI_502",
        "AI 응답 생성에 실패했습니다."
    ),

    RAG_SEARCH_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "RAG_500",
        "RAG 검색 중 오류가 발생했습니다."
    ),

    SLACK_NOTIFICATION_FAILED(
        HttpStatus.BAD_GATEWAY,
        "SLACK_502",
        "Slack 알림 전송에 실패했습니다."
    ),

    SLACK_MESSAGE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "SLACK_404",
        "Slack 메시지를 찾을 수 없습니다."
    ),

    SLACK_MESSAGE_ALREADY_EXISTS(
        HttpStatus.CONFLICT,
        "SLACK_409",
        "해당 AI 요청의 Slack 메시지가 이미 존재합니다."
    ),

    SLACK_MESSAGE_RETRY_NOT_ALLOWED(
        HttpStatus.CONFLICT,
        "SLACK_409",
        "실패한 Slack 메시지만 재발송할 수 있습니다."
    ),

    SLACK_MESSAGE_UPDATE_NOT_ALLOWED(
        HttpStatus.CONFLICT,
        "SLACK_409_01",
        "발송 완료된 Slack 메시지는 수정할 수 없습니다."
    ),

    SLACK_USER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "SLACK_404",
        "Slack 사용자를 찾을 수 없습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(
        HttpStatus status,
        String code,
        String message
    ) {
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
