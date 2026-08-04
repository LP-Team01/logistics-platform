package com.logistics.user.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, OffsetDateTime.now(), request.getRequestURI()));
    }

    public record ErrorResponse(String code, String message, OffsetDateTime timestamp, String path) {}
}
