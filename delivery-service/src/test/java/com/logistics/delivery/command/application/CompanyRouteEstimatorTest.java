package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.delivery.command.application.CompanyRouteEstimator.CompanyRouteEstimate;
import com.logistics.delivery.infrastructure.client.HubQueryService;
import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import com.logistics.delivery.infrastructure.client.external.naver.NaverDirectionsClient;
import com.logistics.delivery.infrastructure.client.external.naver.NaverGeocodingClient;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverDirectionsResponseDto;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverGeocodingResponseDto;
import feign.FeignException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyRouteEstimatorTest {

    @Mock HubQueryService hubQueryService;
    @Mock NaverGeocodingClient naverGeocodingClient;
    @Mock NaverDirectionsClient naverDirectionsClient;
    @InjectMocks CompanyRouteEstimator companyRouteEstimator;

    private NaverGeocodingResponseDto geocodingSuccess() {
        return new NaverGeocodingResponseDto("OK",
            List.of(new NaverGeocodingResponseDto.Address("도로명", "지번", "127.10", "37.55")), null);
    }

    private NaverDirectionsResponseDto directionsSuccess() {
        return new NaverDirectionsResponseDto(0, "OK",
            Map.of("trafast", List.of(new NaverDirectionsResponseDto.Route(
                new NaverDirectionsResponseDto.Summary(12_500L, 1_800_000L)))));
    }

    @Test
    @DisplayName("지오코딩과 경로 조회가 모두 성공하면 좌표·예상거리·예상소요시간이 모두 채워진다")
    void estimatesFullyWhenBothCallsSucceed() {
        UUID destinationHubId = UUID.randomUUID();
        when(naverGeocodingClient.geocode("서울시 강남구")).thenReturn(geocodingSuccess());
        when(hubQueryService.getHub(destinationHubId))
            .thenReturn(new HubServiceHubResponseDto(destinationHubId, "hub addr", 37.50, 127.00));
        when(naverDirectionsClient.getDirections("127.0,37.5", "127.1,37.55", null, "trafast"))
            .thenReturn(directionsSuccess());

        CompanyRouteEstimate result = companyRouteEstimator.estimate(destinationHubId, "서울시 강남구");

        assertEquals(37.55, result.latitude());
        assertEquals(127.10, result.longitude());
        assertEquals(13, result.distanceKm());
        assertEquals(30, result.durationMin());
    }

    @Test
    @DisplayName("지오코딩 결과에 주소가 없으면 좌표·거리 모두 null이고 경로 조회는 시도하지 않는다")
    void returnsAllNullWhenAddressNotFound() {
        UUID destinationHubId = UUID.randomUUID();
        when(naverGeocodingClient.geocode("존재하지 않는 주소"))
            .thenReturn(new NaverGeocodingResponseDto("OK", List.of(), null));

        CompanyRouteEstimate result = companyRouteEstimator.estimate(destinationHubId, "존재하지 않는 주소");

        assertNull(result.latitude());
        assertNull(result.longitude());
        assertNull(result.distanceKm());
        assertNull(result.durationMin());
    }

    @Test
    @DisplayName("지오코딩이 계속 실패하면 최대 3회까지 재시도한 뒤 모두 null로 반환한다")
    void retriesGeocodingUpToThreeTimesThenGivesUp() {
        UUID destinationHubId = UUID.randomUUID();
        when(naverGeocodingClient.geocode("주소"))
            .thenThrow(mock(FeignException.class));

        CompanyRouteEstimate result = companyRouteEstimator.estimate(destinationHubId, "주소");

        assertNull(result.latitude());
        verify(naverGeocodingClient, times(3)).geocode("주소");
    }

    @Test
    @DisplayName("지오코딩은 성공했지만 허브 조회가 실패하면 좌표는 채워지고 거리 정보는 null이다")
    void keepsCoordinateButSkipsRouteWhenHubLookupFails() {
        UUID destinationHubId = UUID.randomUUID();
        when(naverGeocodingClient.geocode("서울시 강남구")).thenReturn(geocodingSuccess());
        when(hubQueryService.getHub(destinationHubId)).thenThrow(mock(FeignException.class));

        CompanyRouteEstimate result = companyRouteEstimator.estimate(destinationHubId, "서울시 강남구");

        assertEquals(37.55, result.latitude());
        assertEquals(127.10, result.longitude());
        assertNull(result.distanceKm());
        assertNull(result.durationMin());
    }

    @Test
    @DisplayName("Directions 호출이 3회 모두 실패하면 좌표는 유지되고 거리 정보만 null이다")
    void keepsCoordinateButSkipsRouteWhenDirectionsFailsAfterRetries() {
        UUID destinationHubId = UUID.randomUUID();
        when(naverGeocodingClient.geocode("서울시 강남구")).thenReturn(geocodingSuccess());
        when(hubQueryService.getHub(destinationHubId))
            .thenReturn(new HubServiceHubResponseDto(destinationHubId, "hub addr", 37.50, 127.00));
        when(naverDirectionsClient.getDirections("127.0,37.5", "127.1,37.55", null, "trafast"))
            .thenThrow(mock(FeignException.class));

        CompanyRouteEstimate result = companyRouteEstimator.estimate(destinationHubId, "서울시 강남구");

        assertEquals(37.55, result.latitude());
        assertNull(result.distanceKm());
        assertNull(result.durationMin());
        verify(naverDirectionsClient, times(3))
            .getDirections("127.0,37.5", "127.1,37.55", null, "trafast");
    }
}