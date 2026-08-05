-- 수령업체의 담당 허브
ALTER TABLE p_order
    ADD COLUMN receiver_hub_id UUID NOT NULL;

-- 공급업체의 담당 허브
ALTER TABLE p_order_items
    ADD COLUMN supplier_hub_id UUID NOT NULL;

-- 담당 허브별 주문 조회 인덱스
CREATE INDEX idx_order_receiver_hub_id
    ON p_order (receiver_hub_id);

CREATE INDEX idx_order_items_supplier_hub_id
    ON p_order_items (supplier_hub_id);
