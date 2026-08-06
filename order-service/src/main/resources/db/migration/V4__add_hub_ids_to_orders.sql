-- 기존 데이터가 있어도 실패하지 않도록 우선 NULL 허용으로 컬럼을 추가합니다.
ALTER TABLE p_orders
    ADD COLUMN receiver_hub_id UUID;

ALTER TABLE p_order_items
    ADD COLUMN supplier_hub_id UUID;

-- 기존 데이터는 허용하고 앞으로 생성되는 데이터부터 NULL을 방지합니다.
ALTER TABLE p_orders
    ADD CONSTRAINT chk_order_receiver_hub_id
        CHECK (receiver_hub_id IS NOT NULL) NOT VALID;

ALTER TABLE p_order_items
    ADD CONSTRAINT chk_order_item_supplier_hub_id
        CHECK (supplier_hub_id IS NOT NULL) NOT VALID;

-- 허브별 주문 조회를 위한 인덱스입니다.
CREATE INDEX idx_order_receiver_hub_id
    ON p_orders (receiver_hub_id);

CREATE INDEX idx_order_item_supplier_hub_id
    ON p_order_items (supplier_hub_id);
