package com.logistics.order.command.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 주문 수정 요청
 */
public record UpdateOrderRequest(

        @Schema(
                description = "배송 요청사항",
                example = "도착 전에 연락해주세요."
        )
        @Size(
                max = 500,
                message = "배송 요청사항은 500자 이하여야 합니다."
        )
        String deliveryRequest
) {
}
