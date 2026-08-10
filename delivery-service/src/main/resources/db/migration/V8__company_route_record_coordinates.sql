-- 배송지 주소를 Naver Geocoding 좌표로 캐싱합니다.

ALTER TABLE p_company_delivery_route_records
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;
