CREATE TABLE p_hubs (
                        hub_id UUID PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        address VARCHAR(255) NOT NULL,
                        latitude DOUBLE PRECISION NOT NULL,
                        longitude DOUBLE PRECISION NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_by VARCHAR(100) NOT NULL,
                        updated_at TIMESTAMP,
                        updated_by VARCHAR(100),
                        deleted_at TIMESTAMP,
                        deleted_by VARCHAR(100)
);

CREATE INDEX idx_hub_deleted_at ON p_hubs (deleted_at);

CREATE TABLE p_hub_routes (
                              hub_route_id UUID PRIMARY KEY,
                              departure_hub_id UUID NOT NULL,
                              arrival_hub_id UUID NOT NULL,
                              distance DOUBLE PRECISION NOT NULL CHECK (distance > 0),
                              duration INT NOT NULL CHECK (duration > 0),
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              created_by VARCHAR(100) NOT NULL,
                              updated_at TIMESTAMP,
                              updated_by VARCHAR(100),
                              deleted_at TIMESTAMP,
                              deleted_by VARCHAR(100)
);

CREATE UNIQUE INDEX uq_hub_route_pair ON p_hub_routes (departure_hub_id, arrival_hub_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_hub_route_deleted_at ON p_hub_routes (deleted_at);
