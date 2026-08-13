-- 기존 데이터베이스와의 호환성을 위해 생성자 컬럼을 선택값으로 변경합니다.
ALTER TABLE p_companies
    ALTER COLUMN created_by DROP NOT NULL;

ALTER TABLE p_products
    ALTER COLUMN created_by DROP NOT NULL;
