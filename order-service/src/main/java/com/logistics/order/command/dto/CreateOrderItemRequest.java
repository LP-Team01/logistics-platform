package com.logistics.order.command.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문에 포함할 상품 한 개의 요청 정보입니다.
 *
 * 상품명, 단가, 공급업체 ID는 사용자가 직접 보내지 않습니다.
 * productId를 이용해 Company Service에서 조회합니다.
 */
public record CreateOrderItemRequest(
    @NotBlank(message = "상품 ID는 필수입니다.")
    UUID productId,

    @NotBlank(message = "주문 수량은 필수입니다.")
    @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
    Integer quantity,

    @NotNull(message = "납품 기한은 필수입니다.")
    @Future(message = "납품 기한은 현재 시간 이후여야 합니다.")
    Instant requestedDeadline
) {
}
