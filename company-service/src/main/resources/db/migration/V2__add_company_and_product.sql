CREATE TABLE IF NOT EXISTS p_companies
(
    company_id UUID         NOT NULL,
    hub_id     UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    address    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    CONSTRAINT pk_p_companies PRIMARY KEY (company_id)
    );

CREATE TABLE IF NOT EXISTS p_products
(
    product_id UUID         PRIMARY KEY,
    company_id UUID         NOT NULL,
    hub_id     UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    quantity   INT          NOT NULL CHECK (quantity >= 0),
    price      INT          NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100)
    );

CREATE INDEX IF NOT EXISTS idx_companies_deleted_at ON p_companies(deleted_at);
CREATE INDEX IF NOT EXISTS idx_products_deleted_at ON p_products(deleted_at);
CREATE INDEX IF NOT EXISTS idx_products_company_id ON p_products(company_id);
