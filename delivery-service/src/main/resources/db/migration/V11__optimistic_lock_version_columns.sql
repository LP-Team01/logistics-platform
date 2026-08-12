ALTER TABLE p_deliveries
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE p_delivery_route_records
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE p_company_delivery_route_records
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;