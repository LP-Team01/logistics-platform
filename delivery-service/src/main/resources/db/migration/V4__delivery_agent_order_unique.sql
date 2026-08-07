-- (agentType, hubId) 그룹 내 delivery_order 유일성을 DB 레벨에서도 최종 방어
-- 애플리케이션의 advisory lock + 비관적 락이 정상 경로를 이미 직렬화하지만,
-- 락 경로를 우회하는 버그/배치/마이그레이션까지 대비한 안전망.

CREATE UNIQUE INDEX ux_delivery_agents_hub_order_active
    ON p_delivery_agents (agent_type, hub_id, delivery_order)
    WHERE deleted_at IS NULL AND hub_id IS NOT NULL;

CREATE UNIQUE INDEX ux_delivery_agents_global_order_active
    ON p_delivery_agents (agent_type, delivery_order)
    WHERE deleted_at IS NULL AND hub_id IS NULL;
