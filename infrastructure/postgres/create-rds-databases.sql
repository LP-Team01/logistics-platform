-- Execute this file while connected to the RDS `postgres` database.
-- CREATE DATABASE must run outside a transaction.

CREATE DATABASE user_db;
CREATE DATABASE hub_db;
CREATE DATABASE company_db;
CREATE DATABASE order_db;
CREATE DATABASE delivery_db;
CREATE DATABASE ai_db;

SELECT datname
FROM pg_database
WHERE datname IN (
    'user_db',
    'hub_db',
    'company_db',
    'order_db',
    'delivery_db',
    'ai_db'
)
ORDER BY datname;
