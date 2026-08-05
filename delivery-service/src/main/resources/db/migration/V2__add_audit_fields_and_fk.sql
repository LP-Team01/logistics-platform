-- 감사필드(created_at/by, updated_at/by, deleted_at/by) 및 서비스 내부 FK 추가
-- 타 서비스 DB를 참조하는 컬럼(hub_id, order_id, receiver_company_id 등)은 FK를 걸지 않음

ALTER TABLE p_delivery_agents
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN updated_by UUID,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID;

ALTER TABLE p_deliveries
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN updated_by UUID,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID;

ALTER TABLE p_delivery_route_records
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN updated_by UUID,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID;

ALTER TABLE p_company_delivery_route_records
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now(),
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN updated_by UUID,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by UUID;

-- 같은 DB(delivery_db) 내부 FK
ALTER TABLE p_delivery_route_records
    ADD CONSTRAINT fk_route_record_delivery FOREIGN KEY (delivery_id) REFERENCES p_deliveries (delivery_id),
    ADD CONSTRAINT fk_route_record_agent FOREIGN KEY (agent_id) REFERENCES p_delivery_agents (agent_id);

ALTER TABLE p_company_delivery_route_records
    ADD CONSTRAINT fk_company_route_record_delivery FOREIGN KEY (delivery_id) REFERENCES p_deliveries (delivery_id),
    ADD CONSTRAINT fk_company_route_record_agent FOREIGN KEY (agent_id) REFERENCES p_delivery_agents (agent_id);

ALTER TABLE p_deliveries
    ADD CONSTRAINT fk_delivery_company_agent FOREIGN KEY (company_agent_id) REFERENCES p_delivery_agents (agent_id);
