package com.logistics.delivery.query.dto.response;

import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
@Schema(description = "배송 담당자 목록 조회 결과(페이징)")
public record DeliveryAgentResponseDto(
    @Schema(description = "배송 담당자 요약 목록")
    List<DeliverAgentSummary> content,
    @Schema(description = "전체 요소 수")
    Long totalElements,
    @Schema(description = "전체 페이지 수")
    Integer totalPages,
    @Schema(description = "페이지 크기")
    Integer size,
    @Schema(description = "현재 페이지 번호(0부터 시작)")
    Integer number
) {

    @Schema(description = "배송 담당자 요약")
    public record DeliverAgentSummary(
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
        Boolean isAvailable
    ) {
        public static DeliverAgentSummary from(DeliveryAgent deliveryAgent) {
            return new DeliverAgentSummary(
                deliveryAgent.getId(),
                deliveryAgent.getHubId(),
                deliveryAgent.getAgentType(),
                deliveryAgent.getSlackId(),
                deliveryAgent.getDeliveryOrder(),
                deliveryAgent.isAvailable()
            );
        }
    }

    public static DeliveryAgentResponseDto from(Page<DeliveryAgent> page) {
        return DeliveryAgentResponseDto.builder()
            .content(page.getContent().stream().map(DeliverAgentSummary::from).toList())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .size(page.getSize())
            .number(page.getNumber())
            .build();
    }
}
