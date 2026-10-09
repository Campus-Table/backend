package com.campustable.campus_table.service;

import com.campustable.campus_table.dto.MileageDtos.BalanceResponse;
import com.campustable.campus_table.dto.MileageDtos.ChargeConfirmRequest;
import com.campustable.campus_table.entity.MileageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 토스 승인(외부 호출)과 적립(DB 트랜잭션)을 분리해 순서대로 수행한다.
 * ponytail: 승인 성공 후 DB 적립이 실패하면 수동 대사 필요. 운영 전 결제 내역 테이블/보상 로직 추가.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MileageChargeService {

    private final TossPaymentClient toss;
    private final MileageService mileageService;
    private final NotificationService notificationService;

    public BalanceResponse charge(Long userId, ChargeConfirmRequest req) {
        toss.confirm(req.paymentKey(), req.orderId(), req.amount());
        int balance = mileageService.apply(userId, MileageType.CHARGE, req.amount(), null);
        try {
            notificationService.mileageCharged(userId, req.amount());
        } catch (Exception e) { // 충전은 이미 완료되었으므로 알림 실패가 충전 응답을 실패시키지 않게 한다
            log.warn("충전 알림 생성 실패 userId={}", userId, e);
        }
        return new BalanceResponse(balance);
    }
}
