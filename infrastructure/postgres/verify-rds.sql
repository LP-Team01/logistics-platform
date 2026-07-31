-- Run against the RDS `ai_db` database after creating the service databases.
-- Example: psql "host=<endpoint> port=5432 dbname=ai_db user=<user> sslmode=require" -f verify-rds.sql

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SELECT current_database() AS database_name;

SELECT extname, extversion
FROM pg_extension
WHERE extname IN ('vector', 'hstore', 'uuid-ossp')
ORDER BY extname;
