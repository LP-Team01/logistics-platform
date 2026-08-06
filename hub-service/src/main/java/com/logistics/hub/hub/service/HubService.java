package com.logistics.hub.hub.service;

import com.logistics.hub.global.exception.BusinessException;
import com.logistics.hub.global.exception.ErrorCode;
import com.logistics.hub.hub.dto.HubCreateRequestDto;
import com.logistics.hub.hub.dto.HubDeleteResponseDto;
import com.logistics.hub.hub.dto.HubPageResponseDto;
import com.logistics.hub.hub.dto.HubResponseDto;
import com.logistics.hub.hub.dto.HubUpdateRequestDto;
import com.logistics.hub.hub.entity.Hub;
import com.logistics.hub.hub.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

    @Transactional
    public HubResponseDto createHub(HubCreateRequestDto request) {
        Hub hub = Hub.builder()
            .name(request.name())
            .address(request.address())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .build();

        // todo: JWT 인증/게이트웨이 헤더 전달 방식 확정되면 실제 요청자로 교체
        hub.assignCreatedBy("system"); // 임시값

        Hub savedHub = hubRepository.save(hub);

        return HubResponseDto.from(savedHub);
    }

    @Cacheable(value = "hubs", key = "#hubId")
    public HubResponseDto getHub(UUID hubId) {
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
            .orElseThrow(()-> new BusinessException(ErrorCode.HUB_NOT_FOUND));

        return HubResponseDto.from(hub);
    }

    @Transactional
    @CacheEvict(value = "hubs", key = "#hubId") // 캐시 비우기
    public HubResponseDto updateHub(UUID hubId, HubUpdateRequestDto request) {
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));

        hub.updateInfo(request.name(),request.address(), request.latitude(), request.longitude());

        // todo: JWT 인증/게이트웨이 헤더 전달 방식 확정되면 실제 요청자로 교체
        hub.assignUpdatedBy("system"); // 임시값

        return HubResponseDto.from(hub);
    }

    @Transactional
    @CacheEvict(value = "hubs", key = "#hubId") // 캐시 비우기
    public HubDeleteResponseDto deleteHub(UUID hubId) {
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
            .orElseThrow(()->new BusinessException(ErrorCode.HUB_NOT_FOUND));

        // todo: JWT 인증/게이트웨이 헤더 전달 방식 확정되면 실제 요청자로 교체
        hub.assignDeletedInfo("system"); // 임시값

        return HubDeleteResponseDto.from(hub);
    }

    public HubPageResponseDto searchHubs(String keyword, Pageable pageable) {
        Page<Hub> hubPage = hubRepository.search(keyword, pageable);
        return HubPageResponseDto.from(hubPage);
    }
}
