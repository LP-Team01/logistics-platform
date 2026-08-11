package com.logistics.hub.hubroute.service;

import com.logistics.hub.global.exception.BusinessException;
import com.logistics.hub.global.exception.ErrorCode;
import com.logistics.hub.hub.repository.HubRepository;
import com.logistics.hub.hubroute.dto.HubRouteCreateRequestDto;
import com.logistics.hub.hubroute.dto.HubRouteDeleteResponseDto;
import com.logistics.hub.hubroute.dto.HubRoutePageResponseDto;
import com.logistics.hub.hubroute.dto.HubRouteResponseDto;
import com.logistics.hub.hubroute.dto.HubRouteUpdateRequestDto;
import com.logistics.hub.hubroute.entity.HubRoute;
import com.logistics.hub.hubroute.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubRouteService {

    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;

    @Transactional
    @CacheEvict(value = "hubRouteGraph", key = "'all'")
    public HubRouteResponseDto createHubRoute(HubRouteCreateRequestDto request) {
        // 출발 허브와 도착 허브 같은지 검증
        if (request.departureHubId().equals(request.arrivalHubId())) {
            throw new BusinessException(ErrorCode.SAME_HUB_ROUTE);
        }

        // 허브 존재 여부 검증
        hubRepository.findByHubIdAndDeletedAtIsNull(request.departureHubId())
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
        hubRepository.findByHubIdAndDeletedAtIsNull(request.arrivalHubId())
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
        // 중복 경로 존재 검증
        if (hubRouteRepository.existsByDepartureHubIdAndArrivalHubIdAndDeletedAtIsNull(
                request.departureHubId(), request.arrivalHubId())
            || hubRouteRepository.existsByDepartureHubIdAndArrivalHubIdAndDeletedAtIsNull(
                request.arrivalHubId(), request.departureHubId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_HUB_ROUTE);
        }

        HubRoute hubRoute = HubRoute.builder()
            .departureHubId(request.departureHubId())
            .arrivalHubId(request.arrivalHubId())
            .distance(request.distance())
            .duration(request.duration())
            .build();

        HubRoute savedHubRoute = hubRouteRepository.save(hubRoute);

        return HubRouteResponseDto.from(savedHubRoute);
    }

    @Cacheable(value = "hubRoutes", key = "#hubRouteId")
    public HubRouteResponseDto getHubRoute(UUID hubRouteId) {
        HubRoute hubRoute = hubRouteRepository.findByHubRouteIdAndDeletedAtIsNull(hubRouteId)
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND));

        return HubRouteResponseDto.from(hubRoute);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hubRoutes", key = "#hubRouteId"),
        @CacheEvict(value = "hubRouteGraph", key = "'all'")
    })
    public HubRouteResponseDto updateHubRoute(UUID hubRouteId, HubRouteUpdateRequestDto request) {
        if (request.distance() == null && request.duration() == null) {
            throw new BusinessException(ErrorCode.INVALID_UPDATE_REQUEST);
        }

        HubRoute hubRoute = hubRouteRepository.findByHubRouteIdAndDeletedAtIsNull(hubRouteId)
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND));

        hubRoute.updateInfo(request.distance(), request.duration());

        return HubRouteResponseDto.from(hubRoute);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "hubRoutes", key = "#hubRouteId"),
        @CacheEvict(value = "hubRouteGraph", key = "'all'")
    })
    public HubRouteDeleteResponseDto deleteHubRoute(UUID hubRouteId, UUID deletedBy) {
        HubRoute hubRoute = hubRouteRepository.findByHubRouteIdAndDeletedAtIsNull(hubRouteId)
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_ROUTE_NOT_FOUND));

        hubRoute.assignDeletedInfo(deletedBy);

        return HubRouteDeleteResponseDto.from(hubRoute);
    }

    public HubRoutePageResponseDto searchHubRoutes(UUID departureHubId, UUID arrivalHubId, Pageable pageable) {
        int size = pageable.getPageSize();
        if (size != 10 && size != 30 && size != 50) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }

        Page<HubRoute> hubRoutePage = hubRouteRepository.search(departureHubId, arrivalHubId, pageable);
        return HubRoutePageResponseDto.from(hubRoutePage);
    }

    public List<UUID> findRelatedHubRouteIds(UUID hubId) {
        return hubRouteRepository.findAllByHubIdAndDeletedAtIsNull(hubId).stream()
            .map(HubRoute::getHubRouteId)
            .toList();
    }
}
