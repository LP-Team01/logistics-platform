package com.logistics.hub.hub.dto;

import com.logistics.hub.hub.entity.Hub;
import org.springframework.data.domain.Page;
import java.util.List;

public record HubPageResponseDto (
    List<HubResponseDto> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {
    public static HubPageResponseDto from(Page<Hub> hubPage){
        List<HubResponseDto> content = hubPage.getContent().stream()
            .map(HubResponseDto::from)
            .toList();

        return new HubPageResponseDto(
            content,
            hubPage.getTotalElements(),
            hubPage.getTotalPages(),
            hubPage.getNumber(), // 현재 페이지 번호
            hubPage.getSize()
        );
    }
}
