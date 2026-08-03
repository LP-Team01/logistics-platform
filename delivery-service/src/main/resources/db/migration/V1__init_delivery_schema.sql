-- 감사필드(created_at/by, updated_at/by, deleted_at/by), 인덱스는 추후 V2에서 추가 예정

CREATE TABLE p_delivery_agents (
                                   agent_id       UUID PRIMARY KEY,
                                   hub_id         UUID,
                                   agent_type     VARCHAR(30) NOT NULL,
                                   slack_id       VARCHAR(100),
                                   delivery_order INTEGER NOT NULL,
                                   is_available   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE p_deliveries (
                              delivery_id        UUID PRIMARY KEY,
                              order_id            UUID NOT NULL UNIQUE,
                              status              VARCHAR(40) NOT NULL,
                              departure_hub_id    UUID NOT NULL,
                              destination_hub_id  UUID NOT NULL,
                              delivery_address    VARCHAR(255) NOT NULL,
                              receiver            VARCHAR(100) NOT NULL,
                              receiver_slack_id   VARCHAR(50),
                              company_agent_id    UUID
);

CREATE TABLE p_delivery_route_records (
                                          route_record_id     UUID PRIMARY KEY,
                                          delivery_id          UUID NOT NULL,
                                          sequence              INTEGER NOT NULL,
                                          departure_hub_id      UUID NOT NULL,
                                          arrival_hub_id        UUID NOT NULL,
                                          estimated_distance    INTEGER NOT NULL,
                                          estimated_duration    INTEGER NOT NULL,
                                          actual_distance       INTEGER,
                                          actual_duration       INTEGER,
                                          status                VARCHAR(40) NOT NULL,
                                          agent_id              UUID
);

CREATE TABLE p_company_delivery_route_records (
                                                  record_id             UUID PRIMARY KEY,
                                                  delivery_id            UUID NOT NULL,
                                                  departure_hub_id       UUID NOT NULL,
                                                  receiver_company_id    UUID NOT NULL,
                                                  estimated_distance     INTEGER,
                                                  estimated_duration     INTEGER,
                                                  actual_distance        INTEGER,
                                                  actual_duration        INTEGER,
                                                  status                 VARCHAR(40) NOT NULL,
                                                  agent_id               UUID,
                                                  delivery_sequence      INTEGER
);
