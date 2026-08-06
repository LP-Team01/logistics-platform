package com.logistics.delivery.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "COMMON_400",
                exception.getParameterName() + "은(는) 필수 파라미터입니다.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "COMMON_400",
                exception.getName() + "의 값이 올바르지 않습니다.", request);
    }

    // 동시 요청으로 사전 체크(existsBy...)를 통과한 뒤 유니크 제약을 위반한 경우의 최종 방어선.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        if (exception.getMostSpecificCause() instanceof ConstraintViolationException constraintViolationException
                && "ux_deliveries_order_item_id_active".equals(constraintViolationException.getConstraintName())) {
            ErrorCode errorCode = ErrorCode.DELIVERY_ORDER_ALREADY_EXISTS;
            return response(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage(), request);
        }
        log.error("Data integrity violation", exception);
        return response(HttpStatus.CONFLICT, "COMMON_409", "데이터 정합성 제약을 위반했습니다.", request);
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
