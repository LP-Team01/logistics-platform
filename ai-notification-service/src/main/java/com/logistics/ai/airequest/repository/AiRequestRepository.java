package com.logistics.ai.airequest.repository;

import com.logistics.ai.airequest.entity.AiRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
     * Kafka 이벤트 ID로 삭제되지 않은 AI 요청을 조회합니다.
     *
     * <p>동일한 이벤트가 다시 수신됐을 때 기존 처리 결과를
     * 확인하는 용도로 사용할 수 있습니다.</p>
     *
     * @param eventId Kafka 이벤트 식별자
     * @return 해당 이벤트로 생성된 AI 요청
     */
    Optional<AiRequest> findByEventIdAndDeletedAtIsNull(
        UUID eventId
    );

    /**
     * 동일한 Kafka 이벤트가 이미 처리됐는지 확인합니다.
     *
     * <p>Soft Delete 여부와 관계없이 중복 이벤트 처리를 막아야 하므로
     * deletedAt 조건을 적용하지 않습니다.</p>
     *
     * @param eventId Kafka 이벤트 식별자
     * @return 동일 이벤트가 존재하면 true
     */
    boolean existsByEventId(UUID eventId);

    /**
     * 삭제되지 않은 AI 요청 목록을 페이징하여 조회합니다.
     *
     * @param pageable 페이지 번호, 크기 및 정렬 정보
     * @return AI 요청 목록
     */
    Page<AiRequest> findAllByDeletedAtIsNull(Pageable pageable);

    /**
     * 특정 주문에 대한 삭제되지 않은 AI 요청 목록을 조회합니다.
     *
     * @param orderId 주문 식별자
     * @param pageable 페이지 정보
     * @return 주문별 AI 요청 목록
     */
    Page<AiRequest> findAllByOrderIdAndDeletedAtIsNull(
        UUID orderId,
        Pageable pageable
    );

    /**
     * 특정 배송에 대한 삭제되지 않은 AI 요청 목록을 조회합니다.
     *
     * @param deliveryId 배송 식별자
     * @param pageable 페이지 정보
     * @return 배송별 AI 요청 목록
     */
    Page<AiRequest> findAllByDeliveryIdAndDeletedAtIsNull(
        UUID deliveryId,
        Pageable pageable
    );
}
