package com.campustable.campus_table.service;

import com.campustable.campus_table.dto.MileageDtos.BalanceResponse;
import com.campustable.campus_table.dto.MileageDtos.ChargeConfirmRequest;
import com.campustable.campus_table.entity.MileageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 토스 승인(외부 호출)과 적립(DB 트랜잭션)을 분리해 순서대로 수행한다.
 * ponytail: 승인 성공 후 DB 적립이 실패하면 수동 대사 필요. 운영 전 결제 내역 테이블/보상 로직 추가.
 */
@Service
@RequiredArgsConstructor
public class MileageChargeService {

    private final TossPaymentClient toss;
    private final MileageService mileageService;

    public BalanceResponse charge(Long userId, ChargeConfirmRequest req) {
        toss.confirm(req.paymentKey(), req.orderId(), req.amount());
        return new BalanceResponse(mileageService.apply(userId, MileageType.CHARGE, req.amount(), null));
    }
}
