package com.logistics.ai.slackmessage.controller;

import com.logistics.ai.slackmessage.dto.requestdto.*;
import com.logistics.ai.slackmessage.dto.responsedto.SlackBulkRetryResponseDto;
import com.logistics.ai.slackmessage.dto.responsedto.SlackMessageResponseDto;
import com.logistics.ai.slackmessage.dto.responsedto.SlackTestMessageResponseDto;
import com.logistics.ai.slackmessage.service.SlackMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Slack 메시지 발송 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slack-messages")
@Tag(
    name = "Slack 메시지",
    description = "Slack 메시지 발송 및 발송 이력 관리 API"
)
public class SlackMessageController {

    private final SlackMessageService slackMessageService;

    /**
     * Slack 메시지 발송 이력을 생성하고 실제 Slack 메시지를 발송합니다.
     *
     * @param requestDto Slack 메시지 발송 요청
     * @return 생성된 Slack 메시지 이력
     */
    @PostMapping
    @Operation(
        summary = "Slack 메시지 발송",
        description = "Slack 메시지 이력을 생성하고 배송담당자에게 메시지를 발송합니다."
    )
    public ResponseEntity<SlackMessageResponseDto> createSlackMessage(
        @Valid @RequestBody SlackMessageRequestDto requestDto
    ) {
        SlackMessageResponseDto response =
            slackMessageService.createSlackMessage(requestDto);

        URI location = URI.create(
            "/api/slack-messages/" + response.slackMessageId()
        );

        return ResponseEntity
            .created(location)
            .body(response);
    }

    /**
     * Slack 메시지 발송 이력을 단건 조회합니다.
     *
     * @param slackMessageId 조회할 Slack 메시지 식별자
     * @return Slack 메시지 발송 이력
     */
    @GetMapping("/{slackMessageId}")
    @Operation(
        summary = "Slack 메시지 단건 조회",
        description = "Slack 메시지 내용과 발송 처리 결과를 조회합니다."
    )
    public ResponseEntity<SlackMessageResponseDto> getSlackMessage(
        @Parameter(
            description = "조회할 Slack 메시지 식별자",
            required = true
        )
        @PathVariable UUID slackMessageId
    ) {
        SlackMessageResponseDto response =
            slackMessageService.getSlackMessage(slackMessageId);

        return ResponseEntity.ok(response);
    }

    /**
     * Slack 메시지 목록을 검색하고 페이징하여 조회합니다.
     *
     * @param condition Slack 메시지 검색 조건
     * @param pageable 페이지 번호, 크기 및 정렬 조건
     * @return 페이징된 Slack 메시지 목록
     */
    @GetMapping
    @Operation(
        summary = "Slack 메시지 목록 조회·검색",
        description = "수신자와 발송 상태를 기준으로 Slack 메시지를 검색합니다."
    )
    public ResponseEntity<Page<SlackMessageResponseDto>> getSlackMessages(
        @ParameterObject
        SlackMessageSearchCondition condition,

        @ParameterObject
        @PageableDefault(
            page = 0,
            size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        )
        Pageable pageable
    ) {
        Page<SlackMessageResponseDto> response =
            slackMessageService.getSlackMessages(
                condition,
                pageable
            );

        return ResponseEntity.ok(response);
    }

    /**
     * 실패한 Slack 메시지를 단건 재발송합니다.
     *
     * @param slackMessageId 재발송할 Slack 메시지 식별자
     * @return 재발송 처리 결과
     */
    @PostMapping("/{slackMessageId}/retry")
    @Operation(
        summary = "Slack 메시지 재발송",
        description = "FAILED 상태인 Slack 메시지 한 건을 다시 발송합니다."
    )
    public ResponseEntity<SlackMessageResponseDto> retrySlackMessage(
        @Parameter(
            description = "재발송할 Slack 메시지 식별자",
            required = true
        )
        @PathVariable UUID slackMessageId
    ) {
        SlackMessageResponseDto response =
            slackMessageService.retrySlackMessage(slackMessageId);

        return ResponseEntity.ok(response);
    }

    /**
     * Slack 메시지를 부분 수정합니다.
     *
     * <p>PENDING 또는 FAILED 상태의 메시지만 수정할 수 있으며,
     * 요청에 포함된 필드만 변경합니다.</p>
     *
     * @param slackMessageId 수정할 Slack 메시지 식별자
     * @param requestDto Slack 메시지 수정 요청
     * @return 수정된 Slack 메시지
     */
    @PatchMapping("/{slackMessageId}")
    @Operation(
        summary = "Slack 메시지 수정",
        description = "발송 전 또는 발송에 실패한 Slack 메시지를 부분 수정합니다."
    )
    public ResponseEntity<SlackMessageResponseDto> updateSlackMessage(
        @Parameter(
            description = "수정할 Slack 메시지 식별자",
            required = true
        )
        @PathVariable UUID slackMessageId,

        @Valid
        @RequestBody
        SlackMessageUpdateRequestDto requestDto
    ) {
        SlackMessageResponseDto response =
            slackMessageService.updateSlackMessage(
                slackMessageId,
                requestDto
            );

        return ResponseEntity.ok(response);
    }

    /**
     * Slack 메시지 발송 이력을 논리 삭제합니다.
     *
     * @param slackMessageId 삭제할 Slack 메시지 식별자
     * @param deletedBy 삭제를 수행한 사용자 식별자
     * @return 응답 본문 없는 204 응답
     */
    @DeleteMapping("/{slackMessageId}")
    @Operation(
        summary = "Slack 메시지 이력 삭제",
        description = "Slack 메시지 발송 이력을 논리 삭제합니다."
    )
    public ResponseEntity<Void> deleteSlackMessage(
        @Parameter(
            description = "삭제할 Slack 메시지 식별자",
            required = true
        )
        @PathVariable UUID slackMessageId,

        @Parameter(
            description = "삭제를 수행한 사용자 식별자",
            required = true
        )
        @RequestHeader("X-User-Id")
        UUID deletedBy
    ) {
        slackMessageService.deleteSlackMessage(
            slackMessageId,
            deletedBy
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Slack 연동 상태를 확인하기 위한 테스트 메시지를 발송합니다.
     *
     * <p>실제 Slack API를 호출하지만
     * Slack 메시지 이력은 DB에 저장하지 않습니다.</p>
     *
     * @param requestDto Slack 테스트 메시지 발송 요청
     * @return Slack 테스트 메시지 발송 결과
     */
    @PostMapping("/test")
    @Operation(
        summary = "테스트 Slack 메시지 발송",
        description = "Slack 토큰과 수신자 ID가 정상적으로 동작하는지 확인합니다."
    )
    public ResponseEntity<SlackTestMessageResponseDto> sendTestMessage(
        @Valid @RequestBody SlackTestMessageRequestDto requestDto
    ) {
        SlackTestMessageResponseDto response =
            slackMessageService.sendTestMessage(requestDto);

        return ResponseEntity.ok(response);
    }

    /**
     * 실패한 Slack 메시지를 여러 건 일괄 재발송합니다.
     *
     * <p>일부 메시지가 실패하더라도 나머지 메시지는 계속 처리하며,
     * 메시지별 성공 및 실패 결과를 반환합니다.</p>
     *
     * @param requestDto 일괄 재발송 요청
     * @return 메시지별 일괄 재발송 결과
     */
    @PostMapping("/retry")
    @Operation(
        summary = "Slack 메시지 일괄 재발송",
        description = """
        FAILED 상태의 Slack 메시지를 여러 건 다시 발송합니다.
        일부 메시지가 실패해도 나머지 메시지는 계속 처리합니다.
        """
    )
    public ResponseEntity<SlackBulkRetryResponseDto> retrySlackMessages(
        @Valid @RequestBody SlackBulkRetryRequestDto requestDto
    ) {
        SlackBulkRetryResponseDto response =
            slackMessageService.retrySlackMessages(requestDto);

        return ResponseEntity.ok(response);
    }
}
