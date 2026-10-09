package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.common.RateLimitException;
import com.campustable.campus_table.repository.UserRepository;


import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 학교 메일로 6자리 인증 코드를 보내고 확인한다. 용도는 회원가입(signup)과 비밀번호 재설정(reset).
 *
 * 남용 방지
 * - 발송: 학번(용도)당 60초 간격 + 시간당 5회, IP당 시간당 20회
 * - 확인: 코드 하나당 5번까지만 비교한다 (5번째까지 틀리면 코드를 폐기하고 새로 요청해야 함)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String SIGNUP = "signup";
    private static final String RESET = "reset";

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_WINDOW = Duration.ofHours(1);
    private static final int MAX_SENDS_PER_HOUR = 5;
    private static final int MAX_SENDS_PER_IP_HOUR = 20;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final RateLimiter limiter;

    @Value("${app.school-email-domain}")
    private String schoolDomain;

    @Value("${app.mail-enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String from;

    public String emailOf(String studentNumber) {
        return studentNumber + "@" + schoolDomain;
    }

    // ---------- 회원가입 ----------

    public void sendCode(String studentNumber, String ip) {
        limitSend(SIGNUP, studentNumber, ip);
        if (userRepository.existsByStudentNumber(studentNumber)) {
            throw new CustomException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
        issue(SIGNUP, studentNumber, emailOf(studentNumber), "[Campus Table] 이메일 인증 코드");
    }

    public void verify(String studentNumber, String code) {
        checkCode(SIGNUP, studentNumber, code);
        redis.opsForValue().set(verifiedKey(studentNumber), "1", VERIFIED_TTL);
    }

    /** 가입 시 호출: 인증 완료 여부를 확인하고 소비한다. */
    public void consumeVerified(String studentNumber) {
        if (!Boolean.TRUE.equals(redis.delete(verifiedKey(studentNumber)))) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    // ---------- 비밀번호 재설정 ----------

    /**
     * 가입된 계정이 아니면(email == null) 메일은 보내지 않지만, 발송 제한은 똑같이 적용한다.
     * 응답이 같아 학번의 가입 여부를 알 수 없게 하기 위함이다.
     */
    public void sendResetCode(String studentNumber, String emailOrNull, String ip) {
        limitSend(RESET, studentNumber, ip);
        if (emailOrNull != null) {
            issue(RESET, studentNumber, emailOrNull, "[Campus Table] 비밀번호 재설정 코드");
        }
    }

    public void verifyResetCode(String studentNumber, String code) {
        checkCode(RESET, studentNumber, code);
    }

    // ---------- 내부 ----------

    private void limitSend(String purpose, String studentNumber, String ip) {
        String cooldown = "mail:cool:" + purpose + ":" + studentNumber;
        if (!limiter.tryAcquire(cooldown, SEND_COOLDOWN)) {
            throw tooMany(cooldown, "인증 메일은 잠시 후 다시 요청해주세요.");
        }
        String perStudent = "mail:hour:" + purpose + ":" + studentNumber;
        if (limiter.increment(perStudent, SEND_WINDOW) > MAX_SENDS_PER_HOUR) {
            throw tooMany(perStudent, "인증 메일 요청이 너무 많습니다.");
        }
        String perIp = "mail:ip:" + ip;
        if (limiter.increment(perIp, SEND_WINDOW) > MAX_SENDS_PER_IP_HOUR) {
            throw tooMany(perIp, "인증 메일 요청이 너무 많습니다.");
        }
    }

    private RateLimitException tooMany(String key, String prefix) {
        long wait = limiter.retryAfterSeconds(key);
        return new RateLimitException(ErrorCode.TOO_MANY_REQUESTS,
                prefix + " " + RateLimitException.waitText(wait) + " 후 다시 시도해주세요.", wait);
    }

    private void issue(String purpose, String studentNumber, String email, String subject) {
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        redis.opsForValue().set(codeKey(purpose, studentNumber), code, CODE_TTL);
        limiter.reset(failKey(purpose, studentNumber));
        if (!mailEnabled) { // 로컬 개발용: 메일 대신 로그로 확인
            log.info("[MAIL_ENABLED=false] {} {} 코드: {}", purpose, email, code);
            return;
        }
        try {
            var msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(email);
            msg.setSubject(subject);
            msg.setText("인증 코드: " + code + "\n5분 안에 입력해주세요.");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("인증 메일 발송 실패", e);
            redis.delete(codeKey(purpose, studentNumber));
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    /**
     * 코드 확인과 소비를 Redis에서 **한 번에(원자적으로)** 처리한다.
     * 확인(조회)과 삭제를 따로 하면, 같은 코드로 동시에 요청한 둘이 모두 통과할 수 있다 (코드 1회 사용 보장 실패).
     * 스크립트 결과: 1 성공(코드·시도 횟수 삭제), 0 코드 없음/만료, -1 오답(시도 남음), -2 시도 소진(코드 폐기)
     */
    private static final DefaultRedisScript<Long> CHECK_AND_CONSUME = new DefaultRedisScript<>("""
            local saved = redis.call('GET', KEYS[1])
            if not saved then return 0 end
            local attempts = redis.call('INCR', KEYS[2])
            if redis.call('TTL', KEYS[2]) < 0 then redis.call('EXPIRE', KEYS[2], ARGV[3]) end
            local max = tonumber(ARGV[2])
            if attempts > max then
              redis.call('DEL', KEYS[1], KEYS[2])
              return -2
            end
            if saved == ARGV[1] then
              redis.call('DEL', KEYS[1], KEYS[2])
              return 1
            end
            if attempts >= max then
              redis.call('DEL', KEYS[1], KEYS[2])
              return -2
            end
            return -1
            """, Long.class);

    private void checkCode(String purpose, String studentNumber, String code) {
        Long result = redis.execute(CHECK_AND_CONSUME,
                List.of(codeKey(purpose, studentNumber), failKey(purpose, studentNumber)),
                code.trim(), String.valueOf(MAX_CODE_ATTEMPTS), String.valueOf(CODE_TTL.toSeconds()));
        if (result != null && result == 1) {
            return;
        }
        if (result != null && result == -2) { // 5번까지 틀림: 코드가 폐기되었으니 새로 요청해야 한다
            throw tooManyCodeAttempts();
        }
        throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
    }

    private RateLimitException tooManyCodeAttempts() {
        return new RateLimitException(ErrorCode.TOO_MANY_REQUESTS,
                "인증 시도가 너무 많습니다. 인증 코드를 다시 요청해주세요.", 0);
    }

    private String codeKey(String purpose, String studentNumber) {
        return "email:code:" + purpose + ":" + studentNumber;
    }

    private String failKey(String purpose, String studentNumber) {
        return "email:fail:" + purpose + ":" + studentNumber;
    }

    private String verifiedKey(String studentNumber) {
        return "email:verified:" + studentNumber;
    }
}
