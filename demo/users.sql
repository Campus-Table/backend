-- 시연 전용 계정 300개 (학번 99000001~99000300, 비밀번호는 README 참고). 운영 서비스용 DB에는 실행하지 마세요.
-- 여러 번 실행해도 안전합니다(이미 있으면 건너뛰고 잔액만 채웁니다).
-- 비밀번호 해시는 BCrypt(cost 4)라 로그인이 빠릅니다.
SET NAMES utf8mb4;
INSERT IGNORE INTO users (student_number, email, password, name, role, mileage_balance, created_at)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 300)
SELECT CONCAT('99', LPAD(i, 6, '0')), CONCAT('99', LPAD(i, 6, '0'), '@dankook.ac.kr'),
       '$2a$04$rGRiUNIjYB8.ugkGpTI3uu86cCN/Okzg1d2ok6BrjINkvg5JRoCR.', CONCAT('시연', i), 'USER', 10000000, NOW()
FROM n;
UPDATE users SET mileage_balance = 10000000
WHERE student_number BETWEEN '99000001' AND '99000300' AND mileage_balance < 10000000;
