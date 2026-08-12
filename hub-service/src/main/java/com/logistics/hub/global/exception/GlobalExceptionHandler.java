package com.logistics.hub.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        return response(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, "COMMON_400", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500",
                "서버 내부 오류가 발생했습니다.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException exception, HttpServletRequest request) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uq_hub_route_pair")) {
            ErrorCode errorCode = ErrorCode.DUPLICATE_HUB_ROUTE;
            return response(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), request);
        }
        if (message != null && message.contains("uq_hub_name")) {
            ErrorCode errorCode = ErrorCode.DUPLICATE_HUB;
            return response(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), request);
        }
        log.error("Unhandled data integrity violation", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500",
            "서버 내부 오류가 발생했습니다.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
        MethodArgumentTypeMismatchException exception, HttpServletRequest request
    ) {
        String message = String.format("'$s', 파라미터의 값이 올바른 형식이 아닙니다.", exception.getName());
        return response(HttpStatus.BAD_REQUEST, "COMMON_400", message, request);
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, OffsetDateTime.now(), request.getRequestURI()));
    }

    public record ErrorResponse(String code, String message, OffsetDateTime timestamp, String path) {}
}
