package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.config.AuthUser;
import com.campustable.campus_table.dto.AuthDtos.PasswordChangeRequest;
import com.campustable.campus_table.dto.AuthDtos.PasswordResetConfirmRequest;
import com.campustable.campus_table.entity.User;
import com.campustable.campus_table.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerification;
    private final LoginAttemptService loginAttempts;
    private final SessionInvalidator sessionInvalidator;

    /** 재설정 코드 발송. 가입 여부와 상관없이 응답이 같다. */
    public void sendResetCode(String studentNumber, String ip) {
        String email = userRepository.findByStudentNumber(studentNumber).map(User::getEmail).orElse(null);
        emailVerification.sendResetCode(studentNumber, email, ip);
    }

    /** 코드 확인 후 새 비밀번호로 변경하고, 모든 기기의 로그인을 해제한다. */
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest req) {
        emailVerification.verifyResetCode(req.studentNumber(), req.code());
        User user = userRepository.findByStudentNumber(req.studentNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_VERIFICATION_CODE));
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        loginAttempts.reset(req.studentNumber()); // 잠금도 풀어준다
        sessionInvalidator.invalidateAll(user.getId(), null);
    }

    /**
     * 로그인 상태에서 변경. 현재 비밀번호를 틀리면 로그인 실패와 같은 한도로 센다 (세션 탈취 후 무차별 대입 방지).
     * 변경 후 현재 기기를 뺀 나머지 세션은 종료한다.
     */
    @Transactional
    public void change(AuthUser me, PasswordChangeRequest req, String ip, String currentSessionId) {
        loginAttempts.begin(me.studentNumber(), ip);
        User user = userRepository.findById(me.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            loginAttempts.recordFailure(ip);
            throw new CustomException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        loginAttempts.reset(me.studentNumber()); // 현재 비밀번호를 맞췄으니 시도 횟수 초기화
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_UNCHANGED);
        }
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        sessionInvalidator.invalidateAll(user.getId(), currentSessionId);
    }
}
