package com.logistics.hub.hubroute.repository;

import com.logistics.hub.hubroute.entity.HubRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    List<HubRoute> findByDeletedAtIsNull(); //목록 조회
    Optional<HubRoute> findByHubRouteIdAndDeletedAtIsNull(UUID hubRouteId);

    boolean existsByDepartureHubIdAndArrivalHubIdAndDeletedAtIsNull(
        UUID departureHubId, UUID arrivalHubId
    ); // 출발-도착 조합 중복 체크
}
