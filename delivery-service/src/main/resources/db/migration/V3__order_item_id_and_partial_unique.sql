-- 배송 유일성 기준을 order_id(주문) → order_item_id(주문아이템)로 변경
-- Order 서비스 리뷰: 주문 1건에 주문아이템 여러 개가 있고 아이템마다 다른 허브를 거칠 수 있어
-- "주문 1건당 배송 1건"이 아니라 "주문아이템 1건당 배송 1건"이 맞는 모델임

ALTER TABLE p_deliveries ADD COLUMN order_item_id UUID;

-- 기존(개발용) 데이터 백필: order_item_id가 따로 없던 시절 데이터라 임시로 order_id를 채워넣음
UPDATE p_deliveries SET order_item_id = order_id WHERE order_item_id IS NULL;

ALTER TABLE p_deliveries ALTER COLUMN order_item_id SET NOT NULL;

-- order_id는 더 이상 유일하지 않음 (같은 주문에 속한 여러 배송을 묶는 용도로 재정의)
ALTER TABLE p_deliveries DROP CONSTRAINT p_deliveries_order_id_key;
CREATE INDEX idx_delivery_order_id ON p_deliveries (order_id);

-- 논리 삭제와 UNIQUE 충돌 방지: 활성 행(deleted_at IS NULL)만 대상으로 유일성 강제
-- 삭제된 배송이 있어도 같은 주문아이템으로 재생성 가능
CREATE UNIQUE INDEX ux_deliveries_order_item_id_active ON p_deliveries (order_item_id) WHERE deleted_at IS NULL;
