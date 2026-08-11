package com.logistics.order.command.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 주문 취소 요청
 */
public record CancelOrderRequest(

        @Schema(
                description = "주문 취소 사유",
                example = "고객 요청으로 주문을 취소합니다."
        )
        @NotBlank(message = "취소 사유는 필수입니다.")
        @Size(max = 300, message = "취소 사유는 300자 이하여야 합니다.")
        String cancelReason
) {
}
