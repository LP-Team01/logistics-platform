-- 배송지 주소를 Naver Geocoding한 좌표를 생성 시점에 1회 캐싱 (docs/naver.md 1번 항목 결정 B)

ALTER TABLE p_company_delivery_route_records
    ADD COLUMN latitude  DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;