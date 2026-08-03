package com.logistics.ai.airequest.controller;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.requestdto.AiSearchCondition;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.dto.responsedto.AiStatisticsResponseDto;
import com.logistics.ai.airequest.service.AiRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

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
     * API에서 허용하는 페이지 크기입니다.
     */
    private static final Set<Integer> ALLOWED_PAGE_SIZES =
        Set.of(10, 30, 50);

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

    /**
     * 특정 AI 요청의 처리 결과를 단건 조회합니다.
     *
     * @param aiRequestId AI 요청 식별자
     * @return AI 요청 처리 결과
     */
    @GetMapping("/{aiRequestId}")
    @Operation(
        summary = "AI 요청 단건 조회",
        description = "AI 요청 식별자로 처리 상태와 계산 결과를 조회합니다."
    )
    public ResponseEntity<AiResponseDto> getAiRequest(
        @Parameter(
            description = "AI 요청 식별자",
            required = true
        )
        @PathVariable("aiRequestId") UUID aiRequestId
    ) {
        AiResponseDto response =
            aiRequestService.getAiRequest(aiRequestId);

        return ResponseEntity.ok(response);
    }

    /**
     * 검색조건과 페이지 정보를 이용하여 AI 요청 목록을 조회합니다.
     *
     * @param condition 주문 ID, 배송 ID, 처리 상태 검색조건
     * @param pageable 페이지 번호, 크기 및 정렬 정보
     * @return 페이징된 AI 요청 목록
     */
    @GetMapping
    @Operation(
        summary = "AI 요청 목록 조회/검색",
        description = "주문 ID, 배송 ID, 처리 상태로 AI 요청 이력을 검색합니다."
    )
    public ResponseEntity<Page<AiResponseDto>> searchAiRequests(
        @ParameterObject AiSearchCondition condition,

        @ParameterObject
        @PageableDefault(
            size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        )
        Pageable pageable
    ) {
        validatePageSize(pageable.getPageSize());

        Page<AiResponseDto> response =
            aiRequestService.searchAiRequests(
                condition,
                pageable
            );

        return ResponseEntity.ok(response);
    }

    /**
     * 페이지 크기가 요구사항에서 허용한 값인지 검증합니다.
     */
    private void validatePageSize(int pageSize) {
        if (!ALLOWED_PAGE_SIZES.contains(pageSize)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "페이지 크기는 10, 30, 50 중 하나여야 합니다."
            );
        }
    }

    /**
     * 실패한 AI 요청을 다시 처리합니다.
     *
     * @param aiRequestId 재처리할 AI 요청 식별자
     * @return 재처리 결과
     */
    @PostMapping("/{aiRequestId}/retry")
    @Operation(
        summary = "AI 요청 재처리",
        description = "FAILED 상태인 AI 요청을 Gemini에 다시 전달하여 처리합니다."
    )
    public ResponseEntity<AiResponseDto> retryAiRequest(
        @Parameter(
            description = "재처리할 AI 요청 식별자",
            required = true
        )
        @PathVariable("aiRequestId") UUID aiRequestId
    ) {
        AiResponseDto response =
            aiRequestService.retryAiRequest(aiRequestId);

        return ResponseEntity.ok(response);
    }

    /**
     * 지정된 기간의 AI 요청 처리 통계를 조회합니다.
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return AI 요청 처리 통계
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "AI 처리 통계 조회",
        description = "기간별 성공·실패 건수와 평균 AI 처리시간을 조회합니다."
    )
    public ResponseEntity<AiStatisticsResponseDto> getAiStatistics(
        @Parameter(
            description = "조회 시작일",
            example = "2026-08-01"
        )
        @RequestParam(
            name = "startDate",
            required = false
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @Parameter(
            description = "조회 종료일",
            example = "2026-08-03"
        )
        @RequestParam(
            name = "endDate",
            required = false
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate
    ) {
        AiStatisticsResponseDto response =
            aiRequestService.getAiStatistics(
                startDate,
                endDate
            );

        return ResponseEntity.ok(response);
    }

    /**
     * AI 요청 이력을 논리 삭제합니다.
     *
     * @param aiRequestId 삭제할 AI 요청 식별자
     * @param deletedBy   삭제를 수행한 사용자 식별자
     * @return 응답 본문 없는 204 응답
     */
    @DeleteMapping("/{aiRequestId}")
    @Operation(
        summary = "AI 요청 이력 삭제",
        description = "AI 요청 이력을 물리적으로 삭제하지 않고 논리 삭제합니다."
    )
    public ResponseEntity<Void> deleteAiRequest(
        @Parameter(description = "삭제할 AI 요청 식별자")
        @PathVariable UUID aiRequestId,

        @Parameter(description = "삭제를 수행한 사용자 식별자")
        @RequestHeader("X-User-Id") UUID deletedBy
    ) {
        aiRequestService.deleteAiRequest(aiRequestId, deletedBy);

        return ResponseEntity.noContent().build();
    }
}
