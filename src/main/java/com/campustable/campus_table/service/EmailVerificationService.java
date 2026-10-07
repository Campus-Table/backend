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

    @Value("${spring.mail.username:}")
    private String from;

    public String normalize(String email) {
        return email.trim().toLowerCase();
    }

    public void checkDomain(String email) {
        if (!email.endsWith("@" + schoolDomain)) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }
    }

    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        checkDomain(email);
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(codeKey(email), code, CODE_TTL);
        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(email);
            msg.setSubject("[Campus Table] 이메일 인증 코드");
            msg.setText("인증 코드: " + code + "\n5분 안에 입력해주세요.");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("인증 메일 발송 실패", e);
            redis.delete(codeKey(email));
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    public void verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        String saved = redis.opsForValue().get(codeKey(email));
        if (saved == null || !saved.equals(code.trim())) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        redis.delete(codeKey(email));
        redis.opsForValue().set(verifiedKey(email), "1", VERIFIED_TTL);
    }

    /** 가입 시 호출: 인증 완료 여부를 확인하고 소비한다. */
    public void consumeVerified(String email) {
        if (!Boolean.TRUE.equals(redis.delete(verifiedKey(email)))) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private String codeKey(String email) {
        return "email:code:" + email;
    }

    private String verifiedKey(String email) {
        return "email:verified:" + email;
    }
}
