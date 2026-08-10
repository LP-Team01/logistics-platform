package com.logistics.delivery.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

// orderedRecordIds: AI가 미세조정한 최종 방문 순서 (recordId를 방문 순서대로 나열)
public record VisitSequenceRefinementResponseDto(
    UUID agentId,
    List<UUID> orderedRecordIds
) {
}