package com.logistics.delivery.infrastructure.kafka;

import com.logistics.delivery.command.dto.command.CreateDeliveryCommand;
import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.infrastructure.client.HubQueryService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


 //배송 생성 직후 DELIVERY_CREATED Kafka 이벤트를 조립
 //업체 경로 예상치(지오코딩)나 허브 주소 조회가 아직 준비되지 않았으면 이벤트를 만들지 않고 건너뜀
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryAiNotificationEventFactory {

    private static final String EVENT_VERSION = "v1";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final HubQueryService hubQueryService;

    @Value("${ai-notification.preparation-buffer-minutes:30}")
    private int preparationBufferMinutes;

    public Optional<DeliveryAiNotificationEvent> create(Delivery delivery, CreateDeliveryCommand command,
                                                          List<DeliveryRouteRecord> routeRecords,
                                                          CompanyDeliveryRouteRecord companyRouteRecord) {
        if (companyRouteRecord.getEstimatedDistance() == null || companyRouteRecord.getEstimatedDuration() == null) {
            log.warn("업체 경로 예상치가 아직 없어 DELIVERY_CREATED 이벤트 발행을 건너뜁니다. deliveryId={}", delivery.getId());
            return Optional.empty();
        }

        Map<UUID, String> hubAddresses;
        try {
            hubAddresses = fetchHubAddresses(command, routeRecords);
        } catch (Exception exception) {
            log.warn("허브 주소 조회 실패로 DELIVERY_CREATED 이벤트 발행을 건너뜁니다. deliveryId={}", delivery.getId(), exception);
            return Optional.empty();
        }

        int estimatedDurationMinutes = routeRecords.stream()
            .mapToInt(DeliveryRouteRecord::getEstimatedDuration)
            .sum() + companyRouteRecord.getEstimatedDuration();

        DeliveryAiNotificationEvent event = new DeliveryAiNotificationEvent(
            UUID.randomUUID(),
            EVENT_VERSION,
            DeliveryAiNotificationEventType.DELIVERY_CREATED,
            Instant.now(),
            command.orderId(),
            delivery.getId(),
            command.recipientUserId(),
            command.receiverSlackId(),
            List.of(new DeliveryAiNotificationEvent.ProductItem(command.productName(), command.quantity())),
            command.requestText(),
            hubAddresses.get(command.departureHubId()),
            routeRecords.stream().map(record -> hubAddresses.get(record.getArrivalHubId())).toList(),
            command.deliveryAddress(),
            LocalDateTime.ofInstant(command.requestedArrivalAt(), ZONE),
            estimatedDurationMinutes,
            preparationBufferMinutes
        );

        return Optional.of(event);
    }

    // 출발 허브 + 구간별 도착 허브 주소를 한 번씩만 조회 (같은 허브가 반복돼도 중복 호출하지 않도록 Map으로 캐시)
    private Map<UUID, String> fetchHubAddresses(CreateDeliveryCommand command, List<DeliveryRouteRecord> routeRecords) {
        Map<UUID, String> addresses = new LinkedHashMap<>();
        addresses.put(command.departureHubId(), null);
        routeRecords.forEach(record -> addresses.put(record.getArrivalHubId(), null));

        for (UUID hubId : addresses.keySet()) {
            addresses.put(hubId, hubQueryService.getHub(hubId).address());
        }
        return addresses;
    }
}
