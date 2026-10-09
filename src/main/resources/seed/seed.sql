-- 학식당 초기 데이터(가게 9개, 메뉴 93개). 프론트 목 데이터(mockData.js) 기준이며 이미지는 비어 있습니다.
-- 같은 내용이 있으면 건너뛰므로 여러 번 실행해도 안전합니다. 수동 실행: mysql ... campus_table < seed.sql
-- 가게별 동시 조리 수(cooking_capacity): 기본 3, 51장국밥 4. 이미 만든 DB는 UPDATE stores SET cooking_capacity = 3 로 맞춥니다.
-- 기동 시 자동 입력은 SEED_ENABLED=true 이고 식당이 하나도 없을 때만 동작합니다.
SET NAMES utf8mb4;
INSERT INTO cafeterias (name, seat_count, dining_minutes, opening_time, closing_time, created_at)
SELECT '학식당', 220, 30, '09:00:00', '20:00:00', NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cafeterias WHERE name = '학식당');
SET @cid = (SELECT MIN(id) FROM cafeterias WHERE name = '학식당');

-- 51장국밥
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '51장국밥', '든든한 국밥과 한식 메뉴', '한식', 3, 4, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '51장국밥');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '고기만국밥' AS name, 6500 AS price
  UNION ALL SELECT '공기밥 추가' AS name, 1000 AS price
  UNION ALL SELECT '광주 크림순대국밥' AS name, 8400 AS price
  UNION ALL SELECT '닭곰탕(밥 포함)' AS name, 5500 AS price
  UNION ALL SELECT '닭칼국수' AS name, 5900 AS price
  UNION ALL SELECT '물냉면' AS name, 6500 AS price
  UNION ALL SELECT '물냉면+왕만두' AS name, 8400 AS price
  UNION ALL SELECT '물비빔냉면' AS name, 6500 AS price
  UNION ALL SELECT '물비빔냉면+왕만두' AS name, 8400 AS price
  UNION ALL SELECT '비빔냉면' AS name, 6500 AS price
  UNION ALL SELECT '비빔냉면+왕만두' AS name, 8400 AS price
  UNION ALL SELECT '순대국밥' AS name, 6500 AS price
  UNION ALL SELECT '순대만국밥' AS name, 6500 AS price
  UNION ALL SELECT '얼큰고기만국밥' AS name, 7500 AS price
  UNION ALL SELECT '얼큰순대국밥' AS name, 7500 AS price
  UNION ALL SELECT '왕만두' AS name, 2000 AS price
  UNION ALL SELECT '편육한접시' AS name, 2500 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '51장국밥'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 가오슝
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '가오슝', '다양한 덮밥과 면 요리', '아시안', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '가오슝');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '갈비튀김덮밥' AS name, 8900 AS price
  UNION ALL SELECT '대만식고기덮밥' AS name, 8400 AS price
  UNION ALL SELECT '동파육덮밥' AS name, 9400 AS price
  UNION ALL SELECT '마늘쫑민찌덮밥' AS name, 5900 AS price
  UNION ALL SELECT '마파두부덮밥' AS name, 6900 AS price
  UNION ALL SELECT '마파참깨덮밥' AS name, 7400 AS price
  UNION ALL SELECT '마파참깨면' AS name, 7900 AS price
  UNION ALL SELECT '우육면' AS name, 7400 AS price
  UNION ALL SELECT '자장면' AS name, 6400 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '가오슝'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 값찌개
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '값찌개', '따뜻하고 든든한 찌개', '한식', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '값찌개');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '공기밥' AS name, 1000 AS price
  UNION ALL SELECT '돼지김치찌개(밥포함)' AS name, 6500 AS price
  UNION ALL SELECT '바지락순두부(밥포함)' AS name, 6200 AS price
  UNION ALL SELECT '순두부찌개(밥포함)' AS name, 6000 AS price
  UNION ALL SELECT '스팸김치찌개(밥포함)' AS name, 6500 AS price
  UNION ALL SELECT '스팸순두부(밥포함)' AS name, 6900 AS price
  UNION ALL SELECT '우삼겹된장찌개(밥포함)' AS name, 6500 AS price
  UNION ALL SELECT '우삼겹순두부(밥포함)' AS name, 6900 AS price
  UNION ALL SELECT '참치김치찌개(밥포함)' AS name, 6500 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '값찌개'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 경성카츠
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '경성카츠', '바삭한 한 끼', '돈카츠', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '경성카츠');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '고구마치즈돈카츠' AS name, 8300 AS price
  UNION ALL SELECT '등심돈카츠' AS name, 7500 AS price
  UNION ALL SELECT '새우튀김 우동' AS name, 6900 AS price
  UNION ALL SELECT '샐러드파스타+등심카츠' AS name, 8900 AS price
  UNION ALL SELECT '샐러드파스타+치킨카츠' AS name, 8900 AS price
  UNION ALL SELECT '왕돈카츠' AS name, 8900 AS price
  UNION ALL SELECT '우동' AS name, 4900 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '경성카츠'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 광뚝
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '광뚝', '불고기와 뚝배기 한식', '한식', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '광뚝');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '간장 불고기' AS name, 7900 AS price
  UNION ALL SELECT '고추장 불고기' AS name, 7900 AS price
  UNION ALL SELECT '떡왕만두뚝배기' AS name, 7900 AS price
  UNION ALL SELECT '뚝배기알밥' AS name, 5900 AS price
  UNION ALL SELECT '물만두' AS name, 1900 AS price
  UNION ALL SELECT '부산물밀면(곱빼기)' AS name, 8900 AS price
  UNION ALL SELECT '부산비빔면(곱빼기)' AS name, 8900 AS price
  UNION ALL SELECT '사골갈제비' AS name, 6900 AS price
  UNION ALL SELECT '얼큰떡왕만두뚝배기' AS name, 8900 AS price
  UNION ALL SELECT '얼큰사골갈제비' AS name, 7900 AS price
  UNION ALL SELECT '얼큰칼왕만두뚝배기' AS name, 8900 AS price
  UNION ALL SELECT '칼왕만두뚝배기' AS name, 7900 AS price
  UNION ALL SELECT '콩나물불백' AS name, 6500 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '광뚝'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 도쿄야
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '도쿄야', '오므라이스와 함박스테이크', '일식', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '도쿄야');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '59오므라이스' AS name, 5900 AS price
  UNION ALL SELECT '갈릭함박스테이크' AS name, 9900 AS price
  UNION ALL SELECT '돈코츠라멘' AS name, 7400 AS price
  UNION ALL SELECT '도쿄야오므라이스' AS name, 7400 AS price
  UNION ALL SELECT '도쿄함박스테이크' AS name, 8900 AS price
  UNION ALL SELECT '불닭함박스테이크' AS name, 9900 AS price
  UNION ALL SELECT '음료' AS name, 2200 AS price
  UNION ALL SELECT '투움바오므라이스' AS name, 7900 AS price
  UNION ALL SELECT '투움바함박스테이크' AS name, 9900 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '도쿄야'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 바비든든
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '바비든든', '든든한 덮밥 한 끼', '덮밥', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '바비든든');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '고기든든' AS name, 3900 AS price
  UNION ALL SELECT '고기든든(킹)' AS name, 5900 AS price
  UNION ALL SELECT '스팸마요덮밥(라지)' AS name, 4900 AS price
  UNION ALL SELECT '제육덮밥' AS name, 3900 AS price
  UNION ALL SELECT '제육덮밥(킹)' AS name, 5900 AS price
  UNION ALL SELECT '참치마요덮밥(라지)' AS name, 4900 AS price
  UNION ALL SELECT '춘천닭갈비덮밥(킹)' AS name, 5900 AS price
  UNION ALL SELECT '치킨마요덮밥(라지)' AS name, 4900 AS price
  UNION ALL SELECT '햄볶음김치덮밥' AS name, 4900 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '바비든든'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 비비고고
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '비비고고', '카레와 비빔밥', '한식', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '비비고고');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '기본카레' AS name, 4900 AS price
  UNION ALL SELECT '불고기비빔밥' AS name, 7500 AS price
  UNION ALL SELECT '불고기카레' AS name, 6900 AS price
  UNION ALL SELECT '새우카레' AS name, 6900 AS price
  UNION ALL SELECT '오색비빔밥' AS name, 6500 AS price
  UNION ALL SELECT '육회비빔밥' AS name, 7900 AS price
  UNION ALL SELECT '치킨카레' AS name, 6900 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '비비고고'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);

-- 폭풍분식
INSERT INTO stores (cafeteria_id, name, description, category, avg_wait_minutes, cooking_capacity, created_at)
SELECT @cid, '폭풍분식', '라면, 김밥, 떡볶이 분식', '분식', 3, 3, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE cafeteria_id = @cid AND name = '폭풍분식');
INSERT INTO menus (store_id, name, price, is_available, created_at)
SELECT s.id, v.name, v.price, true, NOW() FROM stores s JOIN (
  SELECT '닭가슴살포케' AS name, 5900 AS price
  UNION ALL SELECT '떡라면' AS name, 4000 AS price
  UNION ALL SELECT '라볶이' AS name, 5000 AS price
  UNION ALL SELECT '라죽' AS name, 4000 AS price
  UNION ALL SELECT '만두라면' AS name, 4500 AS price
  UNION ALL SELECT '짜파게티' AS name, 4000 AS price
  UNION ALL SELECT '참치김밥' AS name, 3800 AS price
  UNION ALL SELECT '치즈김밥' AS name, 3800 AS price
  UNION ALL SELECT '치즈라면' AS name, 4000 AS price
  UNION ALL SELECT '폭풍김밥' AS name, 3500 AS price
  UNION ALL SELECT '폭풍라면' AS name, 3500 AS price
  UNION ALL SELECT '훈제연어포케' AS name, 5900 AS price
  UNION ALL SELECT '훈제오리포케' AS name, 5900 AS price
) v
WHERE s.cafeteria_id = @cid AND s.name = '폭풍분식'
AND NOT EXISTS (SELECT 1 FROM menus m WHERE m.store_id = s.id AND m.name = v.name);
