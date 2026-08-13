package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.infrastructure.client.AiNotificationServiceClient;
import com.logistics.delivery.infrastructure.client.HubQueryService;
import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementResponseDto;
import com.logistics.delivery.infrastructure.client.external.naver.NaverDirectionsClient;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverDirectionsResponseDto;
import com.logistics.delivery.global.config.InternalServiceProperties;
import feign.FeignException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// 이 서비스는 외부 API(Gemini/Naver) 계산만 담당하고 DB 반영은 CompanyRouteResequenceWriter(짧은 트랜잭션)에 위임하므로,
// 여기서는 writer.apply()에 전달되는 인자(최종 순서/총 동선/기대 recordId 집합)가 올바른지 검증한다.
// writer 자체의 advisory lock/재검증/실제 DB 반영 로직은 CompanyRouteResequenceWriterTest에서 별도로 검증한다.
@ExtendWith(MockitoExtension.class)
class CompanyRouteSequencingServiceTest {

    @Mock CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    @Mock HubQueryService hubQueryService;
    @Mock NaverDirectionsClient naverDirectionsClient;
    @Mock AiNotificationServiceClient aiNotificationServiceClient;
    @Mock InternalServiceProperties internalServiceProperties;
    @Mock CompanyRouteResequenceWriter resequenceWriter;
    @InjectMocks CompanyRouteSequencingService companyRouteSequencingService;

    private static final UUID HUB_ID = UUID.randomUUID();
    private static final UUID AGENT_ID = UUID.randomUUID();

    // CompanyDeliveryRouteRecord.id는 @GeneratedValue라 build()만으로는 null이라, 리플렉션으로 직접 주입한다
    private static void setId(CompanyDeliveryRouteRecord record, UUID id) {
        try {
            Field field = CompanyDeliveryRouteRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(record, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private CompanyDeliveryRouteRecord waitingRecord(UUID id, double latitude, double longitude) {
        CompanyDeliveryRouteRecord record = CompanyDeliveryRouteRecord.builder()
            .deliveryId(UUID.randomUUID())
            .departureHubId(HUB_ID)
            .receiverCompanyId(UUID.randomUUID())
            .latitude(latitude)
            .longitude(longitude)
            .estimatedDistance(5)
            .estimatedDuration(10)
            .agentId(AGENT_ID)
            .build();
        setId(record, id);
        return record;
    }

    private HubServiceHubResponseDto hub() {
        return new HubServiceHubResponseDto(HUB_ID, "hub addr", 37.50, 127.00);
    }

    // 구간(leg)마다 동일한 요약값(1km, 1분)을 반환하도록 스텁 - 총합 검증을 단순하게 만들기 위함
    private void stubEveryLegAs1KmAnd1Min() {
        when(naverDirectionsClient.getDirections(anyString(), anyString(), isNull(), eq("trafast")))
            .thenReturn(new NaverDirectionsResponseDto(0, "OK",
                Map.of("trafast", List.of(new NaverDirectionsResponseDto.Route(
                    new NaverDirectionsResponseDto.Summary(1_000L, 60_000L))))));
    }

    private ArgumentCaptor<List<UUID>> finalOrderCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @Test
    @DisplayName("대기 중인 업체 배송 경로가 없으면 아무것도 하지 않고 종료한다")
    void returnsEarlyWhenNoWaitingRecords() {
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of());

        companyRouteSequencingService.resequence(AGENT_ID, HUB_ID);

        verifyNoInteractions(hubQueryService, naverDirectionsClient, aiNotificationServiceClient, resequenceWriter);
    }

    @Test
    @DisplayName("허브 좌표를 확인할 수 없으면 방문 순서 재계산을 건너뛴다")
    void returnsEarlyWhenHubCoordinatesMissing() {
        CompanyDeliveryRouteRecord record = waitingRecord(UUID.randomUUID(), 37.52, 127.00);
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(record));
        when(hubQueryService.getHub(HUB_ID)).thenReturn(new HubServiceHubResponseDto(HUB_ID, "addr", null, null));

        companyRouteSequencingService.resequence(AGENT_ID, HUB_ID);

        assertNull(record.getDeliverySequence());
        verifyNoInteractions(naverDirectionsClient, aiNotificationServiceClient, resequenceWriter);
    }

    @Test
    @DisplayName("AI 미세조정 호출이 실패하면 1차 최근접 이웃(NN) 순서와 총 동선을 그대로 writer에 전달한다")
    void appliesNearestNeighborOrderAndTotalRouteWhenAiRefinementUnavailable() {
        UUID nearId = UUID.randomUUID();
        UUID farId = UUID.randomUUID();
        CompanyDeliveryRouteRecord near = waitingRecord(nearId, 37.51, 127.00);
        CompanyDeliveryRouteRecord far = waitingRecord(farId, 37.60, 127.00);
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(far, near));
        when(hubQueryService.getHub(HUB_ID)).thenReturn(hub());
        when(aiNotificationServiceClient.refineVisitSequence(any(), any(), any()))
            .thenThrow(mock(FeignException.class));
        stubEveryLegAs1KmAnd1Min();

        companyRouteSequencingService.resequence(AGENT_ID, HUB_ID);

        ArgumentCaptor<Set<UUID>> expectedIdsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<List<UUID>> finalOrderCaptor = finalOrderCaptor();
        ArgumentCaptor<CompanyRouteResequenceWriter.RouteTotal> routeTotalCaptor =
            ArgumentCaptor.forClass(CompanyRouteResequenceWriter.RouteTotal.class);
        verify(resequenceWriter).apply(eq(AGENT_ID), expectedIdsCaptor.capture(), finalOrderCaptor.capture(),
            routeTotalCaptor.capture());

        assertEquals(Set.of(nearId, farId), expectedIdsCaptor.getValue());
        // 허브(37.50)에서 near(37.51)가 far(37.60)보다 가까우므로 NN 순서는 near -> far
        assertEquals(List.of(nearId, farId), finalOrderCaptor.getValue());
        // 구간 2개(허브->near, near->far) x 1km/1분 = 총 2km/2분
        assertEquals(2, routeTotalCaptor.getValue().totalDistanceKm());
        assertEquals(2, routeTotalCaptor.getValue().totalDurationMinutes());
    }

    @Test
    @DisplayName("AI가 유효한 순서를 응답하면 NN 순서 대신 AI가 다듬은 순서를 writer에 전달한다")
    void appliesAiRefinedOrderWhenValid() {
        UUID nearId = UUID.randomUUID();
        UUID farId = UUID.randomUUID();
        CompanyDeliveryRouteRecord near = waitingRecord(nearId, 37.51, 127.00);
        CompanyDeliveryRouteRecord far = waitingRecord(farId, 37.60, 127.00);
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(near, far));
        when(hubQueryService.getHub(HUB_ID)).thenReturn(hub());
        // AI가 NN과 반대 순서(far 먼저)로 응답
        when(aiNotificationServiceClient.refineVisitSequence(any(), any(), any()))
            .thenReturn(new VisitSequenceRefinementResponseDto(AGENT_ID, List.of(farId, nearId)));
        stubEveryLegAs1KmAnd1Min();

        companyRouteSequencingService.resequence(AGENT_ID, HUB_ID);

        ArgumentCaptor<List<UUID>> finalOrderCaptor = finalOrderCaptor();
        verify(resequenceWriter).apply(eq(AGENT_ID), any(), finalOrderCaptor.capture(), any());
        assertEquals(List.of(farId, nearId), finalOrderCaptor.getValue());
    }

    @Test
    @DisplayName("AI 응답에 전체 정류지가 포함되지 않으면(유효하지 않으면) NN 순서를 그대로 writer에 전달한다")
    void keepsNnOrderWhenAiResponseInvalid() {
        UUID nearId = UUID.randomUUID();
        UUID farId = UUID.randomUUID();
        CompanyDeliveryRouteRecord near = waitingRecord(nearId, 37.51, 127.00);
        CompanyDeliveryRouteRecord far = waitingRecord(farId, 37.60, 127.00);
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(near, far));
        when(hubQueryService.getHub(HUB_ID)).thenReturn(hub());
        // far 하나만 포함된 불완전한 응답
        when(aiNotificationServiceClient.refineVisitSequence(any(), any(), any()))
            .thenReturn(new VisitSequenceRefinementResponseDto(AGENT_ID, List.of(farId)));
        stubEveryLegAs1KmAnd1Min();

        companyRouteSequencingService.resequence(AGENT_ID, HUB_ID);

        ArgumentCaptor<List<UUID>> finalOrderCaptor = finalOrderCaptor();
        verify(resequenceWriter).apply(eq(AGENT_ID), any(), finalOrderCaptor.capture(), any());
        assertEquals(List.of(nearId, farId), finalOrderCaptor.getValue());
    }

    @Test
    @DisplayName("구간 경로 조회가 재시도 후에도 계속 실패하면 순서는 writer에 전달하되 총 동선은 null로 전달한다")
    void passesNullRouteTotalWhenLegFetchFails() {
        UUID nearId = UUID.randomUUID();
        CompanyDeliveryRouteRecord near = waitingRecord(nearId, 37.51, 127.00);
        when(companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(AGENT_ID, CompanyRouteRecordStatus.WAITING))
            .thenReturn(List.of(near));
        when(hubQueryService.getHub(HUB_ID)).thenReturn(hub());
        when(aiNotificationServiceClient.refineVisitSequence(any(), any(), any()))
            .thenThrow(mock(FeignException.class));
        when(naverDirectionsClient.getDirections(anyString(), anyString(), isNull(), eq("trafast")))
            .thenThrow(mock(FeignException.class));

        companyRouteSequencingService.resequence(AGENT_ID, HUB_ID);

        ArgumentCaptor<List<UUID>> finalOrderCaptor = finalOrderCaptor();
        verify(resequenceWriter, times(1)).apply(eq(AGENT_ID), any(), finalOrderCaptor.capture(), eq(null));
        assertEquals(List.of(nearId), finalOrderCaptor.getValue());
    }
}