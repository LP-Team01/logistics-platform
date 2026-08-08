CREATE TABLE IF NOT EXISTS p_companies
(
    company_id UUID         NOT NULL,
    hub_id     UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    address    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    CONSTRAINT pk_p_companies PRIMARY KEY (company_id)
    );

CREATE TABLE IF NOT EXISTS p_products
(
    product_id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    hub_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    price INT NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
    );
