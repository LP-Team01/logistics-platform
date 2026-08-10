-- 업체배송담당자의 당일 전체 동선(허브→업체1→업체2→...) 총 거리/시간 캐싱
-- 재계산될 때마다(CompanyRouteSequencingService) 덮어쓰기 - 1:1 관계라 별도 테이블 대신 컬럼으로 추가

ALTER TABLE p_delivery_agents
    ADD COLUMN total_distance    INTEGER,
    ADD COLUMN total_duration    INTEGER,
    ADD COLUMN route_computed_at TIMESTAMPTZ;
