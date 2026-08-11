package com.logistics.hub.hubroute.dto;

import com.logistics.hub.hubroute.entity.HubRoute;
import org.springframework.data.domain.Page;

import java.util.List;

public record HubRoutePageResponseDto(
    List<HubRouteResponseDto> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {
    public static HubRoutePageResponseDto from(Page<HubRoute> hubRoutePage) {
        List<HubRouteResponseDto> content = hubRoutePage.getContent().stream()
            .map(HubRouteResponseDto::from)
            .toList();

        return new HubRoutePageResponseDto(
            content,
            hubRoutePage.getTotalElements(),
            hubRoutePage.getTotalPages(),
            hubRoutePage.getNumber(),
            hubRoutePage.getSize()
        );
    }
}
