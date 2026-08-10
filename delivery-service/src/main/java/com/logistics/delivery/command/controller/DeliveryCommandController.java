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
public class DeliveryCommandController {
    private final DeliveryCommandService deliveryCommandService;
    private final InternalServiceValidator internalServiceValidator;

    @PostMapping
    public ResponseEntity<CreateDeliveryResponseDto> create(
            @RequestHeader("X-Internal-Service") String serviceName,
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody @Valid CreateDeliveryRequestDto request) {
        internalServiceValidator.validateOrderService(serviceName, serviceKey);
        CreateDeliveryCommand command = request.toCommand();
        CreateDeliveryResponseDto result = deliveryCommandService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // Order 서비스가 주문 1건의 상품별 배송을 반복 호출 없이 한 번에 생성하기 위한 내부 전용 API
    @PostMapping("/batch")
    public ResponseEntity<List<BatchCreateDeliveryResponseDto>> createBatch(
            @RequestHeader("X-Internal-Service") String serviceName,
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @RequestBody @Valid List<@NotNull @Valid CreateDeliveryRequestDto> requests) {
        internalServiceValidator.validateOrderService(serviceName, serviceKey);
        List<CreateDeliveryCommand> commands = requests.stream().map(CreateDeliveryRequestDto::toCommand).toList();
        List<BatchCreateDeliveryResponseDto> result = deliveryCommandService.createBatch(commands);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{deliveryId}/status")
    public ResponseEntity<UpdateDeliveryResponseDto> update(
            @RequestHeader("X-User-Role") UserRole userRole,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId,
            @PathVariable UUID deliveryId,
            @RequestBody @Valid UpdateDeliveryRequestDto request) {
        UpdateDeliveryCommand command = request.toCommand();
        UpdateDeliveryResponseDto result = deliveryCommandService.update(
            userRole, requesterId, requesterHubId, deliveryId, command);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{deliveryId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Role") UserRole userRole,
            @PathVariable UUID deliveryId,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader(value = "X-Hub-Id", required = false) UUID requesterHubId) {
        deliveryCommandService.delete(userRole, requesterId, requesterHubId, deliveryId);
        return ResponseEntity.noContent().build();
    }

    // Order 서비스의 주문상품 취소 콜백 전용
    @DeleteMapping("/order-items/{orderItemId}")
    public ResponseEntity<Void> deleteByOrderItem(
            @RequestHeader("X-Internal-Service") String serviceName,
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @PathVariable UUID orderItemId) {
        internalServiceValidator.validateOrderService(serviceName, serviceKey);
        deliveryCommandService.deleteByOrderItem(orderItemId);
        return ResponseEntity.noContent().build();
    }

    // Order 서비스의 주문 생성 실패 보상 처리 전용
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> cancelByOrderId(
            @RequestHeader("X-Internal-Service") String serviceName,
            @RequestHeader("X-Internal-Service-Key") String serviceKey,
            @PathVariable UUID orderId) {
        internalServiceValidator.validateOrderService(serviceName, serviceKey);
        deliveryCommandService.cancelByOrderId(orderId);
        return ResponseEntity.noContent().build();
    }
}
