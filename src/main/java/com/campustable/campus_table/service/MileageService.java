package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.dto.MileageDtos.BalanceResponse;
import com.campustable.campus_table.dto.MileageDtos.TransactionResponse;
import com.campustable.campus_table.entity.MileageTransaction;
import com.campustable.campus_table.entity.MileageType;
import com.campustable.campus_table.entity.Order;
import com.campustable.campus_table.entity.User;
import com.campustable.campus_table.repository.MileageTransactionRepository;
import com.campustable.campus_table.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MileageService {

    private final UserRepository userRepository;
    private final MileageTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public BalanceResponse balance(Long userId) {
        return new BalanceResponse(userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)).getMileageBalance());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> transactions(Long userId) {
        return transactionRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(TransactionResponse::from).toList();
    }

    /** 사용자 행을 잠근 채 잔액을 변경하고 내역을 남긴다. 호출자의 트랜잭션에 참여한다. */
    @Transactional
    public int apply(Long userId, MileageType type, int amount, Order order) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        int delta = type == MileageType.USE ? -amount : amount;
        if (user.getMileageBalance() + delta < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_MILEAGE);
        }
        user.changeMileage(delta);
        transactionRepository.save(MileageTransaction.builder()
                .user(user).order(order).type(type).amount(amount)
                .balanceAfter(user.getMileageBalance()).build());
        return user.getMileageBalance();
    }
}
