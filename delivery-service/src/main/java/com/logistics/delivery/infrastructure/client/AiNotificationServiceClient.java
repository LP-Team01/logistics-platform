package com.logistics.delivery.infrastructure.client;

import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementRequestDto;
import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

// ai-notification-service 내부 전용 API - X-Internal-Service(-Key) 헤더 필요
@FeignClient(name = "ai-notification-service")
public interface AiNotificationServiceClient {

    @PostMapping("/api/ai-requests/visit-sequence")
    VisitSequenceRefinementResponseDto refineVisitSequence(
        @RequestHeader("X-Internal-Service") String serviceName,
        @RequestHeader("X-Internal-Service-Key") String serviceKey,
        @RequestBody VisitSequenceRefinementRequestDto request
    );
}
