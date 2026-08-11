package com.logistics.ai.routenotification.dto.responsedto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

/**
 * 배송 서비스의 배송담당자 목록 API 응답입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryAgentPageResponseDto(

    List<DeliveryAgentSummary> content,
    Long totalElements,
    Integer totalPages,
    Integer size,
    Integer number

) {

    /**
     * 업체 배송담당자 한 명의 요약 정보입니다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeliveryAgentSummary(

        UUID agentId,
        UUID hubId,
        String agentType,
        String slackId,
        Integer deliveryOrder,
        Boolean isAvailable

    ) {
    }
}
