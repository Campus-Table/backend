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

    private final com.campustable.campus_table.repository.MileageChargeRepository charges;
    @org.springframework.beans.factory.annotation.Value("${app.toss.client-key:}")
    private String clientKey;
    @org.springframework.beans.factory.annotation.Value("${app.toss.secret-key:}")
    private String secretKey;
    private final TossPaymentClient toss;
    private final MileageService mileageService;
    private final NotificationService notificationService;

    @org.springframework.transaction.annotation.Transactional
    public com.campustable.campus_table.dto.MileageDtos.ChargePrepareResponse prepare(Long userId, int amount) {
        requireTestKeys();
        var id = java.util.UUID.randomUUID().toString();
        charges.save(new com.campustable.campus_table.entity.MileageCharge(id, userId, amount));
        String customerKey = java.util.UUID.nameUUIDFromBytes(("campus-user:" + userId).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        return new com.campustable.campus_table.dto.MileageDtos.ChargePrepareResponse(clientKey, customerKey, id, amount);
    }

    private void requireTestKeys() {
        if (!((clientKey.startsWith("test_ck_") && secretKey.startsWith("test_sk_"))
                || (clientKey.startsWith("test_gck_") && secretKey.startsWith("test_gsk_"))))
            throw new com.campustable.campus_table.common.CustomException(com.campustable.campus_table.common.ErrorCode.PAYMENT_FAILED,
                    "토스 결제창 테스트 API 키를 설정해주세요.");
    }

    @org.springframework.transaction.annotation.Transactional
    public BalanceResponse charge(Long userId, ChargeConfirmRequest req) {
        requireTestKeys();
        var charge = charges.findForUpdate(req.orderId()).orElseThrow(() ->
                new com.campustable.campus_table.common.CustomException(com.campustable.campus_table.common.ErrorCode.INVALID_REQUEST));
        if (!charge.getUserId().equals(userId) || charge.getAmount() != req.amount())
            throw new com.campustable.campus_table.common.CustomException(com.campustable.campus_table.common.ErrorCode.INVALID_REQUEST);
        if (charge.getPaymentKey() != null) {
            if (!charge.getPaymentKey().equals(req.paymentKey()))
                throw new com.campustable.campus_table.common.CustomException(com.campustable.campus_table.common.ErrorCode.INVALID_REQUEST);
            return new BalanceResponse(charge.getBalanceAfter());
        }
        toss.confirm(req.paymentKey(), req.orderId(), req.amount());
        int balance = mileageService.apply(userId, MileageType.CHARGE, req.amount(), null);
        charge.complete(req.paymentKey(), balance);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        try { notificationService.mileageCharged(userId, req.amount()); }
                        catch (RuntimeException e) { log.warn("충전 알림 생성 실패 userId={}", userId); }
                    }
                });
        return new BalanceResponse(balance);
    }
}
