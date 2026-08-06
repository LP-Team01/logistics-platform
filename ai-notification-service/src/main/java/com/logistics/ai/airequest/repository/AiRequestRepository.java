package com.logistics.ai.airequest.repository;

import com.logistics.ai.airequest.entity.AiRequest;
import com.logistics.ai.airequest.entity.AiRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 요청 이력 데이터에 접근하는 Repository입니다.
 *
 * <p>JpaSpecificationExecutor를 함께 사용하여 추후
 * 주문 ID, 배송 ID, 처리 상태 등의 복합 검색 조건을
 * 동적으로 적용할 수 있도록 구성합니다.</p>
 */
public interface AiRequestRepository
    extends JpaRepository<AiRequest, UUID>,
    JpaSpecificationExecutor<AiRequest> {

    /**
     * 삭제되지 않은 AI 요청을 식별자로 조회합니다.
     *
     * @param aiRequestId AI 요청 식별자
     * @return 삭제되지 않은 AI 요청
     */
    Optional<AiRequest> findByAiRequestIdAndDeletedAtIsNull(
        UUID aiRequestId
    );

    /**
     * Kafka 이벤트 ID로 AI 요청을 조회합니다.
     *
     * <p>eventId는 논리 삭제 여부와 관계없이 영구적인
     * 멱등성 키이므로 deletedAt 조건을 적용하지 않습니다.</p>
     */
    Optional<AiRequest> findByEventId(
        UUID eventId
    );

    /**
     * 지정된 기간의 AI 요청 처리 통계를 조회합니다.
     *
     * <p>삭제되지 않은 요청만 집계하며 시작 일시는 포함하고,
     * 종료 일시는 포함하지 않습니다.</p>
     *
     * @param successStatus 성공 상태
     * @param failedStatus 실패 상태
     * @param startDateTime 조회 시작 일시
     * @param endDateTime 조회 종료 일시
     * @return AI 요청 처리 통계
     */
    @Query("""
    SELECT
        COUNT(ar) AS totalCount,
        COALESCE(
            SUM(
                CASE
                    WHEN ar.status = :successStatus THEN 1
                    ELSE 0
                END
            ),
            0
        ) AS successCount,
        COALESCE(
            SUM(
                CASE
                    WHEN ar.status = :failedStatus THEN 1
                    ELSE 0
                END
            ),
            0
        ) AS failedCount,
        COALESCE(
            AVG(ar.processingTimeMs),
            0.0
        ) AS averageProcessingTimeMs
    FROM AiRequest ar
    WHERE ar.deletedAt IS NULL
      AND ar.createdAt >= :startDateTime
      AND ar.createdAt < :endDateTime
    """)
    AiRequestStatisticsProjection findStatistics(
        @Param("successStatus")
        AiRequestStatus successStatus,

        @Param("failedStatus")
        AiRequestStatus failedStatus,

        @Param("startDateTime")
        LocalDateTime startDateTime,

        @Param("endDateTime")
        LocalDateTime endDateTime
    );
}
