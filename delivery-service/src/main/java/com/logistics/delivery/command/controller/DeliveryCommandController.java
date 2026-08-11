package com.logistics.delivery.command.controller;

import com.logistics.delivery.command.application.DeliveryCommandService;
import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
import com.logistics.delivery.command.dto.request.CreateDeliveryRequestDto;
import com.logistics.delivery.command.dto.request.UpdateDeliveryRequestDto;
import com.logistics.delivery.command.dto.response.BatchCreateDeliveryResponseDto;
import com.logistics.delivery.command.dto.response.CreateDeliveryResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryResponseDto;
import com.logistics.delivery.global.common.InternalServiceValidator;
import com.logistics.delivery.global.common.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
@RequestMapping("/api/deliveries")
@Tag(name = "배송", description = "배송 생성, 상태 변경, 삭제 API")
public class DeliveryCommandController {
    private final DeliveryCommandService deliveryCommandService;
    private final InternalServiceValidator internalServiceValidator;

    @PostMapping
    @Operation(
        summary = "배송 생성",
        description = "Order 서비스가 주문상품 단위로 배송을 생성하는 내부 전용 API입니다. "
            + "허브 간 전체 경로 구간(DeliveryRouteRecord)과 업체 배송 경로(CompanyDeliveryRouteRecord), "
            + "허브 배송 담당자 배정까지 한 트랜잭션으로 함께 처리합니다."
    )
    public ResponseEntity<CreateDeliveryResponseDto> create(
            @Parameter(description = "호출 서비스명(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service") String serviceName,
            @Parameter(description = "호출 서비스 인증 키(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody @Valid CreateDeliveryRequestDto request) {
        internalServiceValidator.validateInternalService(serviceName, serviceKey);
        CreateDeliveryCommand command = request.toCommand();
        CreateDeliveryResponseDto result = deliveryCommandService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // Order 서비스가 주문 1건의 상품별 배송을 반복 호출 없이 한 번에 생성하기 위한 내부 전용 API
    @PostMapping("/batch")
    @Operation(
        summary = "배송 일괄 생성",
        description = "Order 서비스가 주문 1건에 속한 여러 주문상품의 배송을 반복 호출 없이 한 번에 생성하는 내부 전용 API입니다."
    )
    public ResponseEntity<List<BatchCreateDeliveryResponseDto>> createBatch(
            @Parameter(description = "호출 서비스명(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service") String serviceName,
            @Parameter(description = "호출 서비스 인증 키(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody @Valid List<@NotNull @Valid CreateDeliveryRequestDto> requests) {
        internalServiceValidator.validateInternalService(serviceName, serviceKey);
        List<CreateDeliveryCommand> commands = requests.stream().map(CreateDeliveryRequestDto::toCommand).toList();
        List<BatchCreateDeliveryResponseDto> result = deliveryCommandService.createBatch(commands);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{deliveryId}/status")
    @Operation(
        summary = "배송 상태 변경",
        description = "배송 상태를 다음 단계로 전이합니다("
            + "HUB_WAITING → HUB_MOVING → DESTINATION_ARRIVED → DELIVERING → COMPANY_MOVING → DELIVERED). "
            + "역행/스킵 전이는 400으로 거부됩니다."
    )
    public ResponseEntity<UpdateDeliveryResponseDto> update(
            @Parameter(description = "요청자 역할", required = true)
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "요청자 사용자 id", required = true)
            @RequestHeader("X-User-Id") UUID requesterId,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @Parameter(description = "배송 id", required = true)
            @PathVariable UUID deliveryId,
            @RequestBody @Valid UpdateDeliveryRequestDto request) {
        UpdateDeliveryCommand command = request.toCommand();
        UpdateDeliveryResponseDto result = deliveryCommandService.update(
            userRole, requesterId, requesterHubId, deliveryId, command);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{deliveryId}")
    @Operation(
        summary = "배송 삭제",
        description = "배송과 하위 허브 경로 기록/업체 배송 경로 기록을 함께 논리 삭제합니다."
    )
    public ResponseEntity<Void> delete(
            @Parameter(description = "요청자 역할", required = true)
            @RequestHeader("X-User-Role") UserRole userRole,
            @Parameter(description = "배송 id", required = true)
            @PathVariable UUID deliveryId,
            @Parameter(description = "요청자 사용자 id", required = true)
            @RequestHeader("X-User-Id") UUID requesterId,
            @Parameter(description = "요청자 소속 허브 id")
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId) {
        deliveryCommandService.delete(userRole, requesterId, requesterHubId, deliveryId);
        return ResponseEntity.noContent().build();
    }

    // Order 서비스의 주문상품 취소 콜백 전용
    @DeleteMapping("/order-items/{orderItemId}")
    @Operation(
        summary = "주문상품 취소 콜백",
        description = "Order 서비스에서 주문상품이 취소되었을 때 대응하는 배송을 논리 삭제하는 내부 전용 API입니다."
    )
    public ResponseEntity<Void> deleteByOrderItem(
            @Parameter(description = "호출 서비스명(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service") String serviceName,
            @Parameter(description = "호출 서비스 인증 키(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @Parameter(description = "주문상품 id", required = true)
            @PathVariable UUID orderItemId) {
        internalServiceValidator.validateInternalService(serviceName, serviceKey);
        deliveryCommandService.deleteByOrderItem(orderItemId);
        return ResponseEntity.noContent().build();
    }

    // Order 서비스의 주문 생성 실패 보상 처리 전용
    @DeleteMapping("/orders/{orderId}")
    @Operation(
        summary = "주문 생성 실패 보상 삭제",
        description = "Order 서비스의 주문 생성 실패 보상 처리를 위해 해당 주문에 속한 배송을 모두 논리 삭제하는 내부 전용 API입니다."
    )
    public ResponseEntity<Void> cancelByOrderId(
            @Parameter(description = "호출 서비스명(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service") String serviceName,
            @Parameter(description = "호출 서비스 인증 키(내부 전용 검증)", required = true)
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @Parameter(description = "주문 id", required = true)
            @PathVariable UUID orderId) {
        internalServiceValidator.validateInternalService(serviceName, serviceKey);
        deliveryCommandService.cancelByOrderId(orderId);
        return ResponseEntity.noContent().build();
    }
}
