package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.UpdateDeliveryRouteRecordCommand;
import com.logistics.delivery.command.dto.response.UpdateDeliveryRouteRecordResponseDto;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.repository.DeliveryRouteRecordRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryRouteRecordCommandService {
    private final DeliveryRouteRecordRepository routeRecordRepository;

    // TODO: user-service 연동 후 X-User-Role로 배송 담당자 권한 검증 추가 예정
    @Transactional
    public UpdateDeliveryRouteRecordResponseDto updateStatus(UUID deliveryId, Integer sequence,
                                                             UpdateDeliveryRouteRecordCommand command) {
        DeliveryRouteRecord routeRecord = findRouteRecord(deliveryId, sequence);
        routeRecord.update(command.status(), command.actualDistance(), command.actualDuration());
        return UpdateDeliveryRouteRecordResponseDto.from(routeRecord);
    }

    private DeliveryRouteRecord findRouteRecord(UUID deliveryId, Integer sequence) {
        return routeRecordRepository.findByDeliveryIdAndSequenceAndDeletedAtIsNull(deliveryId, sequence)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ROUTE_RECORD_NOT_FOUND));
    }
}
