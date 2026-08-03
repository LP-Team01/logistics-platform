package com.logistics.ai.airequest.controller;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.service.AiRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 요청 관련 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-requests")
@Tag(
    name = "AI 요청",
    description = "배송 최종 발송 시한 계산 및 AI 요청 이력 관리 API"
)
public class AiRequestController {

    private final AiRequestService aiRequestService;

    /**
     * 주문 및 배송 정보를 Gemini에 전달하여 최종 발송 시한을 계산합니다.
     *
     * @param requestDto AI 계산에 필요한 주문 및 배송 정보
     * @return AI 처리 결과
     */
    @PostMapping
    @Operation(
        summary = "최종 발송 시한 생성",
        description = "주문 및 배송 정보를 AI에 전달하여 최종 발송 시한을 계산합니다."
    )
    public ResponseEntity<AiResponseDto> createAiRequest(
        @Valid @RequestBody AiRequestDto requestDto
    ) {
        AiResponseDto response =
            aiRequestService.createAiRequest(requestDto);

        return ResponseEntity.ok(response);
    }
}
