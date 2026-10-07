package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** 학번으로 학교 이메일({학번}@도메인)을 만들어 인증 코드를 보내고 확인한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.school-email-domain}")
    private String schoolDomain;

    @Value("${app.mail-enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String from;

    public String emailOf(String studentNumber) {
        return studentNumber + "@" + schoolDomain;
    }

    public void sendCode(String studentNumber) {
        if (userRepository.existsByStudentNumber(studentNumber)) {
            throw new CustomException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(codeKey(studentNumber), code, CODE_TTL);
        if (!mailEnabled) { // 로컬 개발용: 메일 대신 로그로 확인
            log.info("[MAIL_ENABLED=false] {} 인증 코드: {}", emailOf(studentNumber), code);
            return;
        }
        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(emailOf(studentNumber));
            msg.setSubject("[Campus Table] 이메일 인증 코드");
            msg.setText("인증 코드: " + code + "\n5분 안에 입력해주세요.");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("인증 메일 발송 실패", e);
            redis.delete(codeKey(studentNumber));
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    public void verify(String studentNumber, String code) {
        String saved = redis.opsForValue().get(codeKey(studentNumber));
        if (saved == null || !saved.equals(code.trim())) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        redis.delete(codeKey(studentNumber));
        redis.opsForValue().set(verifiedKey(studentNumber), "1", VERIFIED_TTL);
    }

    /** 가입 시 호출: 인증 완료 여부를 확인하고 소비한다. */
    public void consumeVerified(String studentNumber) {
        if (!Boolean.TRUE.equals(redis.delete(verifiedKey(studentNumber)))) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private String codeKey(String studentNumber) {
        return "email:code:" + studentNumber;
    }

    private String verifiedKey(String studentNumber) {
        return "email:verified:" + studentNumber;
    }
}
