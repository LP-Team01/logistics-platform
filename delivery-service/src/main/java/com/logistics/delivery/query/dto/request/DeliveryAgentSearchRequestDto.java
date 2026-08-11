package com.logistics.delivery.query.dto.request;

import com.logistics.delivery.domain.entity.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "배송 담당자 목록 검색 조건")
public record DeliveryAgentSearchRequestDto(
    @Schema(description = "소속 허브 id 필터")
    UUID hubId,
    @Schema(description = "배송 담당자 유형 필터")
    AgentType agentType,
    @Schema(description = "가용 여부 필터")
    Boolean  isAvailable
) {
}
