package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.command.dto.command.UpdateDeliveryCommand;
import com.logistics.delivery.command.dto.response.CreateDeliveryResponseDto;
import com.logistics.delivery.command.dto.response.UpdateDeliveryResponseDto;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryCommandService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRecordRepository deliveryRouteRecordRepository;

    @Transactional
    public CreateDeliveryResponseDto create(CreateDeliveryCommand command) {
        validateOrder(command.orderId());
        Delivery delivery = Delivery.builder()
            .orderId(command.orderId())
            .departureHubId(command.departureHubId())
            .destinationHubId(command.destinationHubId())
            .deliveryAddress(command.deliveryAddress())
            .receiver(command.receiver())
            .receiverSlackId(command.receiverSlackId())
            .build();

        Delivery saved = deliveryRepository.save(delivery);

        // TODO: Hub 연동(거리/소요시간 조회, 허브 간 다구간 분할) 붙기 전까지의 임시 구현.
        DeliveryRouteRecord routeRecord = DeliveryRouteRecord.builder()
            .deliveryId(saved.getId())
            .sequence(1)
            .departureHubId(command.departureHubId())
            .arrivalHubId(command.destinationHubId())
            .estimatedDistance(0)
            .estimatedDuration(0)
            .build();
        List<DeliveryRouteRecord> savedRouteRecords = deliveryRouteRecordRepository.saveAll(List.of(routeRecord));

        return CreateDeliveryResponseDto.from(saved, savedRouteRecords);
    }

    @Transactional
    public UpdateDeliveryResponseDto update(UUID deliveryId, UpdateDeliveryCommand command) {
        Delivery delivery = findDelivery(deliveryId);
        delivery.update(command.status());
        return UpdateDeliveryResponseDto.from(delivery);
    }

    @Transactional
    public void delete(UUID requesterId, UUID deliveryId) {
        Delivery delivery = findDelivery(deliveryId);
        delivery.softDelete(requesterId);
    }

    private Delivery findDelivery(UUID deliveryId) {
        return deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    private void validateOrder(UUID orderId) {
        boolean exists = deliveryRepository.existsByOrderIdAndDeletedAtIsNull(orderId);
        if (exists) {
            throw new BusinessException(ErrorCode.DELIVERY_ORDER_ALREADY_EXISTS);
        }
    }
}
