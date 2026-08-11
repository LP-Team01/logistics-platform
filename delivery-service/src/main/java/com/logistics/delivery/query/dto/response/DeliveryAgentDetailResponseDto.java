package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "배송 담당자 단건 조회 결과")
public record DeliveryAgentDetailResponseDto(
    @Schema(description = "배송 담당자 id")
    UUID agentId,
    @Schema(description = "소속 허브 id")
    UUID hubId,
    @Schema(description = "배송 담당자 유형")
    AgentType agentType,
    @Schema(description = "Slack id")
    String slackId,
    @Schema(description = "배정 순번(라운드로빈 기준)")
    Integer deliveryOrder,
    @Schema(description = "가용 여부")
    Boolean isAvailable,
    @Schema(description = "생성 일시")
    Instant createdAt,
    @Schema(description = "수정 일시")
    Instant updatedAt
) {
    public static DeliveryAgentDetailResponseDto from(DeliveryAgent deliveryAgent) {
        return DeliveryAgentDetailResponseDto.builder()
            .agentId(deliveryAgent.getId())
            .hubId(deliveryAgent.getHubId())
            .agentType(deliveryAgent.getAgentType())
            .slackId(deliveryAgent.getSlackId())
            .deliveryOrder(deliveryAgent.getDeliveryOrder())
            .isAvailable(deliveryAgent.isAvailable())
            .createdAt(deliveryAgent.getCreatedAt())
            .updatedAt(deliveryAgent.getUpdatedAt())
            .build();
    }
}
