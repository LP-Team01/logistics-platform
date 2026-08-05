package com.logistics.delivery.command.application;

import com.logistics.delivery.command.dto.command.UpdateCompanyRouteRecordCommand;
import com.logistics.delivery.command.dto.response.UpdateCompanyRouteRecordResponseDto;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryRepository;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyRouteRecordCommandService {
    private final CompanyDeliveryRouteRecordRepository companyRouteRecordRepository;
    private final DeliveryRepository deliveryRepository;

    // TODO: user-service 연동 후 X-User-Role로 배송 담당자 권한 검증 추가 예정
    @Transactional
    public UpdateCompanyRouteRecordResponseDto updateStatus(UUID deliveryId, UUID recordId,
                                                            UpdateCompanyRouteRecordCommand command) {
        validateDeliveryExists(deliveryId);
        CompanyDeliveryRouteRecord routeRecord = findRouteRecord(deliveryId, recordId);
        routeRecord.update(command.status(), command.actualDistance(), command.actualDuration());
        return UpdateCompanyRouteRecordResponseDto.from(routeRecord);
    }

    private void validateDeliveryExists(UUID deliveryId) {
        deliveryRepository.findByIdAndDeletedAtIsNull(deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    private CompanyDeliveryRouteRecord findRouteRecord(UUID deliveryId, UUID recordId) {
        return companyRouteRecordRepository.findByIdAndDeliveryIdAndDeletedAtIsNull(recordId, deliveryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_ROUTE_RECORD_NOT_FOUND));
    }
}
