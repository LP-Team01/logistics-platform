-- 주문의 대표 정보를 저장하는 테이블입니다.
-- 상품별 상세 정보는 아래의 p_order_items 테이블에 따로 저장합니다.
CREATE TABLE p_order (
    order_id UUID PRIMARY KEY,
    receiver_company_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount BIGINT NOT NULL CHECK (total_amount >= 0),
    delivery_request TEXT,
    canceled_by BIGINT,
    cancel_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    -- Java의 OrderStatus에 정의된 값만 저장할 수 있습니다.
    CONSTRAINT ck_order_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'FAILED')
    )
);

-- 업체별 주문, 상태별 주문, 삭제되지 않은 주문을 빠르게 조회하기 위한 인덱스입니다.
CREATE INDEX idx_order_receiver_company_id ON p_order (receiver_company_id);
CREATE INDEX idx_order_status ON p_order (status);
CREATE INDEX idx_order_deleted_at ON p_order (deleted_at);

-- 한 주문에 포함된 상품별 주문 내용을 저장합니다.
CREATE TABLE p_order_items (
    order_item_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    unit_price BIGINT NOT NULL CHECK (unit_price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    supplier_company_id UUID NOT NULL,
    -- 하나의 배송 ID가 여러 주문 상품에 중복 연결되는 것을 방지합니다.
    delivery_id UUID UNIQUE,
    status VARCHAR(30) NOT NULL,
    subtotal BIGINT NOT NULL CHECK (subtotal >= 0),
    requested_deadline TIMESTAMP NOT NULL,
    canceled_by BIGINT,
    cancel_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    updated_at TIMESTAMP,
    updated_by UUID,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    -- 존재하지 않는 주문에 주문 상품이 저장되는 것을 방지합니다.
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES p_order(order_id),
    -- Java의 OrderItemStatus에 정의된 값만 저장할 수 있습니다.
    CONSTRAINT ck_order_item_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'DELIVERY_CREATED', 'COMPLETED', 'CANCELLED', 'FAILED')
    )
);

-- 주문·상품·공급업체·상태 조건 조회를 위한 인덱스입니다.
CREATE INDEX idx_order_items_order_id ON p_order_items (order_id);
CREATE INDEX idx_order_items_product_id ON p_order_items (product_id);
CREATE INDEX idx_order_items_supplier_company_id ON p_order_items (supplier_company_id);
CREATE INDEX idx_order_items_status ON p_order_items (status);
CREATE INDEX idx_order_items_deleted_at ON p_order_items (deleted_at);
