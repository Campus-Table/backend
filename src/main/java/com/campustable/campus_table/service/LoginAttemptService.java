package com.campustable.campus_table.service;

import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.common.RateLimitException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 무차별 대입 방지.
 *
 * - 계정(학번): 15분 안에 5번까지만 비밀번호 확인을 시도할 수 있다. 시도 횟수를 **확인 전에 원자적으로 먼저 올리므로**
 *   동시에 요청을 쏟아내도 5번을 넘겨 확인할 수 없다. 로그인에 성공하면 초기화된다.
 * - IP: 실패가 50번 쌓이면 15분 동안 막는다. 학교 공용 IP(같은 와이파이)에서 정상 로그인이 몰려도 막히지 않도록
 *   성공은 세지 않고 실패만 센다. 여러 계정을 돌려 대입하는 공격을 걸러내는 보조 장치다.
 * - 존재하지 않는 학번도 똑같이 센다 (계정 존재 여부 노출 방지). 잠금 중에는 비밀번호가 맞아도 거절한다.
 * ponytail: 고정 윈도우(첫 시도부터 15분). 한 계정을 일부러 잠그는 공격(6번 시도)은 가능하다 — 15분 뒤 풀리고,
 * 비밀번호 재설정으로 즉시 풀 수 있다. IP는 getRemoteAddr 기준이라 ALB 뒤에서는 forward-headers 설정이 필요하다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    static final int MAX_ACCOUNT_ATTEMPTS = 5;
    static final int MAX_IP_FAILURES = 50;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final RateLimiter limiter;

    /** 비밀번호를 확인하기 전에 호출한다. 한도를 넘었으면 429. */
    public void begin(String studentNumber, String ip) {
        long attempts = limiter.increment(accountKey(studentNumber), WINDOW);
        if (attempts > MAX_ACCOUNT_ATTEMPTS) {
            throw locked(accountKey(studentNumber));
        }
        if (limiter.count(ipKey(ip)) >= MAX_IP_FAILURES) {
            throw locked(ipKey(ip));
        }
    }

    /** 비밀번호가 틀렸을 때 호출한다 (IP 실패 횟수). */
    public void recordFailure(String ip) {
        limiter.increment(ipKey(ip), WINDOW);
    }

    /** 성공했을 때 계정의 시도 횟수를 초기화한다. */
    public void reset(String studentNumber) {
        limiter.reset(accountKey(studentNumber));
    }

    private RateLimitException locked(String key) {
        long wait = limiter.retryAfterSeconds(key);
        return new RateLimitException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS,
                "로그인 시도가 너무 많습니다. " + RateLimitException.waitText(wait) + " 후 다시 시도해주세요.", wait);
    }

    private String accountKey(String studentNumber) {
        return "login:fail:acct:" + studentNumber.trim();
    }

    private String ipKey(String ip) {
        return "login:fail:ip:" + ip;
    }
}
