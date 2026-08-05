package com.logistics.ai.slackmessage.repository;

import com.logistics.ai.slackmessage.dto.requestdto
    .SlackMessageSearchCondition;
import com.logistics.ai.slackmessage.entity.SlackMessage;
import com.logistics.ai.slackmessage.entity.SlackMessageStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Slack 메시지 목록의 동적 검색 조건을 생성합니다.
 *
 * <p>검색 조건으로 전달된 값만 WHERE 절에 포함하며,
 * 논리적으로 삭제된 메시지는 항상 제외합니다.</p>
 */
public final class SlackMessageSpecification {

    private SlackMessageSpecification() {
    }

    /**
     * Slack 메시지 목록 검색 조건을 생성합니다.
     *
     * @param condition 검색 조건
     * @return 조합된 검색 조건
     */
    public static Specification<SlackMessage> withCondition(
        SlackMessageSearchCondition condition
    ) {
        List<Specification<SlackMessage>> specifications =
            new ArrayList<>();

        specifications.add(notDeleted());

        if (condition == null) {
            return Specification.allOf(specifications);
        }

        if (condition.recipientUserId() != null) {
            specifications.add(
                hasRecipientUserId(condition.recipientUserId())
            );
        }

        if (condition.status() != null) {
            specifications.add(
                hasStatus(condition.status())
            );
        }

        return Specification.allOf(specifications);
    }

    /**
     * 논리적으로 삭제되지 않은 메시지만 조회합니다.
     */
    private static Specification<SlackMessage> notDeleted() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.isNull(root.get("deletedAt"));
    }

    /**
     * 특정 물류 시스템 사용자의 메시지만 조회합니다.
     */
    private static Specification<SlackMessage> hasRecipientUserId(
        UUID recipientUserId
    ) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(
                root.get("recipientUserId"),
                recipientUserId
            );
    }

    /**
     * 특정 발송 상태의 메시지만 조회합니다.
     */
    private static Specification<SlackMessage> hasStatus(
        SlackMessageStatus status
    ) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(
                root.get("status"),
                status
            );
    }
}
