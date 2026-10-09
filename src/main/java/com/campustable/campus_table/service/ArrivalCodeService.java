package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.entity.ArrivalCode;
import com.campustable.campus_table.entity.Cafeteria;
import com.campustable.campus_table.repository.ArrivalCodeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.security.SecureRandom;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 매일 바뀌는 현장 번호. 그날 첫 조회 시 자동 생성하고, 관리자가 재발급할 수 있다. */
@Service
@RequiredArgsConstructor
public class ArrivalCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ArrivalCodeRepository repository;
    private final EntityManager em;

    // 호출 측이 이후 실패해도 그날의 번호는 유지되어야 하므로 별도 트랜잭션
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArrivalCode getOrCreate(Long cafeteriaId, LocalDate date) {
        Cafeteria cafeteria = lockCafeteria(cafeteriaId); // 같은 날 중복 생성을 막는다
        return repository.findByCafeteriaIdAndCodeDate(cafeteriaId, date)
                .orElseGet(() -> repository.save(ArrivalCode.builder()
                        .cafeteria(cafeteria)
                        .codeDate(date)
                        .code(randomCode())
                        .build()));
    }

    /** 오늘의 현장 번호를 새 번호로 바꾼다 (이전 번호와 반드시 다르다). 이미 도착 인증한 주문에는 영향이 없다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArrivalCode regenerate(Long cafeteriaId, LocalDate date) {
        Cafeteria cafeteria = lockCafeteria(cafeteriaId);
        ArrivalCode existing = repository.findByCafeteriaIdAndCodeDate(cafeteriaId, date).orElse(null);
        if (existing == null) {
            return repository.save(ArrivalCode.builder()
                    .cafeteria(cafeteria).codeDate(date).code(randomCode()).build());
        }
        String next;
        do {
            next = randomCode();
        } while (next.equals(existing.getCode()));
        existing.changeCode(next);
        return existing;
    }

    private Cafeteria lockCafeteria(Long cafeteriaId) {
        Cafeteria cafeteria = em.find(Cafeteria.class, cafeteriaId, LockModeType.PESSIMISTIC_WRITE);
        if (cafeteria == null) {
            throw new CustomException(ErrorCode.CAFETERIA_NOT_FOUND);
        }
        return cafeteria;
    }

    private String randomCode() {
        return "%04d".formatted(RANDOM.nextInt(10_000));
    }
}
