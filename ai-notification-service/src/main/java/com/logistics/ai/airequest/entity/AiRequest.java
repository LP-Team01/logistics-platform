package com.logistics.ai.airequest.entity;

import com.logistics.ai.common.entity.BaseEntity;
import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 배송 정보를 AI에 전달하고 처리 결과를 기록하는 엔티티입니다.
 *
 * <p>주문 및 배송 서비스의 데이터는 MSA 원칙에 따라
 * 물리적인 FK 관계를 맺지 않고 UUID 값으로만 논리 참조합니다.</p>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "p_ai_request",
    uniqueConstraints = {
        // 동일한 Kafka 이벤트가 중복 처리되는 것을 방지합니다.
        @UniqueConstraint(
            name = "uk_ai_request_event",
            columnNames = "event_id"
        )
    },
    indexes = {
        // 주문별 AI 요청 이력 조회에 사용합니다.
        @Index(
            name = "idx_ai_request_order",
            columnList = "order_id"
        ),

        // 배송별 AI 요청 이력 조회에 사용합니다.
        @Index(
            name = "idx_ai_request_delivery",
            columnList = "delivery_id"
        ),

        // 상태별 최신 요청 조회와 모니터링에 사용합니다.
        @Index(
            name = "idx_ai_request_status_created",
            columnList = "status, created_at DESC"
        )
    }
)
public class AiRequest extends BaseEntity {

    // 최초 프롬프트 버전입니다.
    private static final String DEFAULT_PROMPT_VERSION = "v1";

    /**
     * AI 요청 이력의 식별자입니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ai_request_id", nullable = false, updatable = false)
    private UUID aiRequestId;

    /**
     * Kafka 이벤트 식별자입니다.
     *
     * <p>동일 이벤트를 중복으로 처리하지 않도록 UNIQUE 제약조건을 적용합니다.</p>
     */
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    /**
     * Order Service 주문의 논리 참조 ID입니다.
     */
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    /**
     * Delivery Service 배송의 논리 참조 ID입니다.
     */
    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;

    /**
     * 고객이 입력한 배송 요청사항입니다.
     *
     * <p>고객 요청사항이 없는 경우 NULL을 허용합니다.</p>
     */
    @Column(name = "request_text", columnDefinition = "TEXT")
    private String requestText;

    /**
     * 고객이 요청한 최종 도착 일시입니다.
     */
    @Column(name = "requested_arrival_at", nullable = false)
    private LocalDateTime requestedArrivalAt;

    /**
     * 출발지부터 목적지까지 예상되는 배송 소요 시간입니다.
     */
    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    /**
     * 상품 준비 등에 필요한 추가 여유 시간입니다.
     */
    @Column(name = "preparation_buffer_minutes", nullable = false)
    @ColumnDefault("0")
    private Integer preparationBufferMinutes;

    /**
     * Gemini에 전달한 최종 프롬프트입니다.
     *
     * <p>AI 결과 재현과 오류 분석을 위해 저장합니다.</p>
     */
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    /**
     * Gemini가 반환한 원본 응답입니다.
     *
     * <p>처리에 실패한 경우 NULL일 수 있습니다.</p>
     */
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    /**
     * AI가 계산한 최종 발송 시한입니다.
     *
     * <p>AI 처리에 성공한 경우에만 저장합니다.</p>
     */
    @Column(name = "dispatch_deadline")
    private LocalDateTime dispatchDeadline;

    /**
     * 현재 AI 요청의 처리 상태입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    private AiRequestStatus status;

    /**
     * 요청 처리에 사용한 Gemini 모델 이름입니다.
     */
    @Column(name = "model", length = 100)
    private String model;

    /**
     * 사용된 프롬프트의 버전입니다.
     */
    @Column(name = "prompt_version", nullable = false, length = 20)
    @ColumnDefault("'v1'")
    private String promptVersion;

    /**
     * Gemini 요청부터 응답 완료까지 걸린 시간입니다.
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * AI 처리 실패 원인입니다.
     *
     * <p>처리에 성공한 경우 NULL입니다.</p>
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * AI 요청 객체를 생성합니다.
     *
     * <p>생성 시점의 상태는 항상 PENDING이며,
     * 준비 시간이 전달되지 않으면 0분으로 처리합니다.</p>
     */
    private AiRequest(
        UUID eventId,
        UUID orderId,
        UUID deliveryId,
        String requestText,
        LocalDateTime requestedArrivalAt,
        Integer estimatedDurationMinutes,
        Integer preparationBufferMinutes,
        String prompt
    ) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.deliveryId = deliveryId;
        this.requestText = requestText;
        this.requestedArrivalAt = requestedArrivalAt;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.preparationBufferMinutes =
            preparationBufferMinutes == null ? 0 : preparationBufferMinutes;
        this.prompt = prompt;
        this.status = AiRequestStatus.PENDING;
        this.promptVersion = DEFAULT_PROMPT_VERSION;
    }

    /**
     * 외부에서 생성자를 직접 호출하지 않고 정적 팩토리 메서드로 생성합니다.
     */
    public static AiRequest create(
        UUID eventId,
        UUID orderId,
        UUID deliveryId,
        String requestText,
        LocalDateTime requestedArrivalAt,
        Integer estimatedDurationMinutes,
        Integer preparationBufferMinutes,
        String prompt
    ) {
        return new AiRequest(
            eventId,
            orderId,
            deliveryId,
            requestText,
            requestedArrivalAt,
            estimatedDurationMinutes,
            preparationBufferMinutes,
            prompt
        );
    }

    /**
     * AI 처리가 성공한 경우 호출합니다.
     */
    public void markSuccess(
        String response,
        LocalDateTime dispatchDeadline,
        String model,
        Long processingTimeMs
    ) {
        this.response = response;
        this.dispatchDeadline = dispatchDeadline;
        this.model = model;
        this.processingTimeMs = processingTimeMs;

        // 이전 실패 기록이 존재할 수 있으므로 제거합니다.
        this.errorMessage = null;
        this.status = AiRequestStatus.SUCCESS;
    }

    /**
     * AI 처리에 실패한 경우 호출합니다.
     */
    public void markFailed(
        String errorMessage,
        String model,
        Long processingTimeMs
    ) {
        this.response = null;
        this.dispatchDeadline = null;
        this.errorMessage = errorMessage;
        this.model = model;
        this.processingTimeMs = processingTimeMs;
        this.status = AiRequestStatus.FAILED;
    }

    /**
     * 실패한 AI 요청을 재처리 가능한 상태로 변경합니다.
     */
    public void prepareRetry() {
        if (status != AiRequestStatus.FAILED) {
            throw new BusinessException(
                ErrorCode.AI_REQUEST_RETRY_NOT_ALLOWED
            );
        }

        this.response = null;
        this.dispatchDeadline = null;
        this.errorMessage = null;
        this.model = null;
        this.processingTimeMs = null;
        this.status = AiRequestStatus.PENDING;
    }
}
