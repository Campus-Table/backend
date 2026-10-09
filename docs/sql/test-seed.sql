-- 로컬/테스트 MySQL 전용. 서버가 테이블을 만든 뒤 수동 실행하세요.
-- 전용 테스트 식당에 가게 9개·메뉴 93개를 추가합니다. 사용자/잔액/기존 식당 설정은 변경하지 않습니다.
-- 같은 이름의 테스트 데이터가 있으면 건너뜁니다. 단일 실행 기준으로 재실행 가능합니다.
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO cafeterias (name, seat_count, dining_minutes, opening_time, closing_time, created_at)
SELECT '테스트 학식당', 120, 30, '09:00:00', '20:00:00', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cafeterias WHERE name = '테스트 학식당');
SET @seed_cafeteria_id = (SELECT MIN(id) FROM cafeterias WHERE name = '테스트 학식당');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 한식', '임시 테스트 메뉴를 제공하는 가게', '한식', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 한식');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 10');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '김치찌개 테스트 11', 9500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 한식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '김치찌개 테스트 11');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 국밥', '임시 테스트 메뉴를 제공하는 가게', '한식', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 국밥');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 10');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '돼지국밥 테스트 11', 9500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '돼지국밥 테스트 11');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 분식', '임시 테스트 메뉴를 제공하는 가게', '분식', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 분식');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 10');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '떡볶이 테스트 11', 9500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '떡볶이 테스트 11');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 돈까스', '임시 테스트 메뉴를 제공하는 가게', '일식', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 돈까스');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '등심돈까스 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 돈까스'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '등심돈까스 테스트 10');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 중식', '임시 테스트 메뉴를 제공하는 가게', '중식', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 중식');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '짜장면 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 중식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '짜장면 테스트 10');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 면요리', '임시 테스트 메뉴를 제공하는 가게', '면류', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 면요리');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '잔치국수 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 면요리'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '잔치국수 테스트 10');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 덮밥', '임시 테스트 메뉴를 제공하는 가게', '덮밥', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 덮밥');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '불고기덮밥 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 덮밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '불고기덮밥 테스트 10');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 샐러드', '임시 테스트 메뉴를 제공하는 가게', '샐러드', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 샐러드');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '닭가슴살샐러드 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 샐러드'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '닭가슴살샐러드 테스트 10');
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, created_at)
SELECT @seed_cafeteria_id, '테스트 양식', '임시 테스트 메뉴를 제공하는 가게', '양식', 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @seed_cafeteria_id AND name = '테스트 양식');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타', 4500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 2', 5000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 2');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 3', 5500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 3');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 4', 6000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 4');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 5', 6500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 5');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 6', 7000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 6');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 7', 7500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 7');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 8', 8000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 8');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 9', 8500, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 9');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, '토마토파스타 테스트 10', 9000, true, NOW() FROM stores s
WHERE s.cafeteria_id = @seed_cafeteria_id AND s.name = '테스트 양식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = '토마토파스타 테스트 10');
COMMIT;
SELECT @seed_cafeteria_id AS cafeteria_id,
 (SELECT COUNT(*) FROM stores WHERE cafeteria_id = @seed_cafeteria_id) AS store_count,
 (SELECT COUNT(*) FROM menus m JOIN stores s ON s.id=m.store_id WHERE s.cafeteria_id=@seed_cafeteria_id) AS menu_count;
