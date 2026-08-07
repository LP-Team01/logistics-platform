package com.logistics.delivery.infrastructure.client;

import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementRequestDto;
import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// delivery-service가 제안하는 계약 - 실제 엔드포인트는 ai-notification-service 담당자가 구현
// 구현 전까지는 호출 시 404/연결실패가 정상
@FeignClient(name = "ai-notification-service")
public interface AiNotificationServiceClient {

    @PostMapping("/api/ai-requests/visit-sequence")
    VisitSequenceRefinementResponseDto refineVisitSequence(@RequestBody VisitSequenceRefinementRequestDto request);
}
