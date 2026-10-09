-- 시연 데이터 정리: 시연 계정의 주문/알림/마일리지 내역과 시뮬레이터가 넣은 인원 스냅샷(SIMULATION)을 지웁니다.
-- 시연 계정(users)은 남겨 둡니다. 실제 사용자 데이터는 건드리지 않습니다.
DELETE FROM notifications WHERE user_id IN (SELECT id FROM users WHERE student_number BETWEEN '99000001' AND '99000300');
DELETE FROM order_items WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON u.id = o.user_id WHERE u.student_number BETWEEN '99000001' AND '99000300');
DELETE FROM mileage_transactions WHERE user_id IN (SELECT id FROM users WHERE student_number BETWEEN '99000001' AND '99000300');
DELETE FROM orders WHERE user_id IN (SELECT id FROM users WHERE student_number BETWEEN '99000001' AND '99000300');
UPDATE users SET mileage_balance = 10000000 WHERE student_number BETWEEN '99000001' AND '99000300';
DELETE FROM usage_snapshots WHERE source = 'SIMULATION';
