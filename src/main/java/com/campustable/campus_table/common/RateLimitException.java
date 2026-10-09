package com.campustable.campus_table.common;

import lombok.Getter;

/** 요청 제한 초과(429). retryAfterSeconds가 0보다 크면 Retry-After 헤더로 내려간다. */
@Getter
public class RateLimitException extends CustomException {
    private final long retryAfterSeconds;

    public RateLimitException(ErrorCode errorCode, String message, long retryAfterSeconds) {
        super(errorCode, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** "45초", "3분" 처럼 사용자에게 보여줄 대기 시간 문구 */
    public static String waitText(long seconds) {
        return seconds < 60 ? Math.max(seconds, 1) + "초" : (seconds + 59) / 60 + "분";
    }
}
