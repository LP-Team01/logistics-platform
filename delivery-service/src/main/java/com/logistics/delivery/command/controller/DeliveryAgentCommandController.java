package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.dto.command.CreateDeliveryAgentCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryAgentCommand;
import com.logistics.delivery.command.application.DeliveryAgentCommandService;
import com.logistics.delivery.command.dto.request.CreateDeliveryAgentRequestDto;
import com.logistics.delivery.command.dto.request.UpdateDeliveryAgentRequestDto;
import com.logistics.delivery.command.dto.response.CreateDeliveryAgentResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryAgentResponseDto;
import com.logistics.delivery.global.common.UserRole;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-agents")
@Tag(name = "배송 담당자", description = "배송 담당자(허브/업체) 등록, 수정, 삭제 API")
public class DeliveryAgentCommandController {

    private final DeliveryAgentCommandService deliveryAgentCommandService;

    @PostMapping
    @Operation(
        summary = "배송 담당자 등록",
        description = "User 서비스에 존재하는 사용자를 배송 담당자로 등록합니다. "
            + "agentType이 COMPANY_DELIVERY이면 hubId가 필수입니다."
    )
    public ResponseEntity<CreateDeliveryAgentResponseDto> create(
            @Parameter(description = "요청자 역할", required = true)
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @RequestBody @Valid CreateDeliveryAgentRequestDto request
    ) {
        CreateDeliveryAgentCommand command = request.toCommand();
        CreateDeliveryAgentResponseDto result = deliveryAgentCommandService.create(command, userRole, requesterHubId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{agentId}")
    @Operation(
        summary = "배송 담당자 수정",
        description = "배송 담당자의 소속 허브, Slack id, 가용 여부, 담당자 유형을 수정합니다."
    )
    public ResponseEntity<UpdateDeliveryAgentResponseDto> update(
            @Parameter(description = "요청자 역할", required = true)
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @Parameter(description = "배송 담당자 id", required = true)
            @PathVariable UUID agentId,
            @RequestBody @Valid UpdateDeliveryAgentRequestDto request
    ) {
        UpdateDeliveryAgentCommand command = request.toCommand();
        UpdateDeliveryAgentResponseDto result = deliveryAgentCommandService.update(
            agentId, command, userRole, requesterHubId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{agentId}")
    @Operation(
        summary = "배송 담당자 삭제",
        description = "배송 담당자를 논리 삭제합니다."
    )
    public ResponseEntity<Void> delete(
        @Parameter(description = "요청자 역할", required = true)
        @RequestHeader("X-User-Role") UserRole userRole,
        @Parameter(description = "요청자 소속 허브 id")
        @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
        @Parameter(description = "배송 담당자 id", required = true)
        @PathVariable UUID agentId,
        @Parameter(description = "요청자 사용자 id", required = true)
        @RequestHeader("X-User-Id") UUID requesterId
    ) {
        deliveryAgentCommandService.delete(agentId, requesterId, userRole, requesterHubId);
        return ResponseEntity.noContent().build();
    }
}
