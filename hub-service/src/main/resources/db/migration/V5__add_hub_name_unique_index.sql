-- 허브 이름 중복 방지: 소프트 삭제 안 된 것끼리만 유일해야 함
CREATE UNIQUE INDEX uq_hub_name ON p_hubs (name)
    WHERE deleted_at IS NULL;
