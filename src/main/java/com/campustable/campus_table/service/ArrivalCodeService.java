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

/** 매일 바뀌는 현장 번호. 그날 첫 조회 시 자동 생성한다. */
@Service
@RequiredArgsConstructor
public class ArrivalCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ArrivalCodeRepository repository;
    private final EntityManager em;

    // 호출 측이 이후 실패해도 그날의 번호는 유지되어야 하므로 별도 트랜잭션
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArrivalCode getOrCreate(Long cafeteriaId, LocalDate date) {
        // 식당 행을 잠가 같은 날 중복 생성을 막는다
        Cafeteria cafeteria = em.find(Cafeteria.class, cafeteriaId, LockModeType.PESSIMISTIC_WRITE);
        if (cafeteria == null) {
            throw new CustomException(ErrorCode.CAFETERIA_NOT_FOUND);
        }
        return repository.findByCafeteriaIdAndCodeDate(cafeteriaId, date)
                .orElseGet(() -> repository.save(ArrivalCode.builder()
                        .cafeteria(cafeteria)
                        .codeDate(date)
                        .code("%04d".formatted(RANDOM.nextInt(10_000)))
                        .build()));
    }
}
