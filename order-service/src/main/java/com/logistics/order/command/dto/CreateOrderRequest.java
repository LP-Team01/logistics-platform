package com.logistics.order.command.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 주문 생성 API의 요청 본문입니다.
 *
 * createdBy는 요청 Body로 받지 않습니다.
 * 인증된 사용자의 UUID를 Gateway 헤더 또는 JWT에서 가져옵니다.
 */
public record CreateOrderRequest(

        @NotNull(message = "수령 업체 ID는 필수입니다.")
        UUID receiverCompanyId,

        @Size(
                max = 500,
                message = "배송 요청사항은 500자 이하로 입력해야 합니다."
        )
        String deliveryRequest,

        @Valid
        @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
        List<CreateOrderItemRequest> items
) {
}
