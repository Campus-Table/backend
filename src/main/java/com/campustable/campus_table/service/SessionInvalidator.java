package com.campustable.campus_table.service;

import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Component;

/** 사용자의 로그인 세션을 서버에서 종료한다 (비밀번호 변경/재설정 후). */
@Component
@RequiredArgsConstructor
public class SessionInvalidator {

    private final FindByIndexNameSessionRepository<?> sessions;

    /** exceptSessionId가 null이면 모든 세션을 종료한다. */
    public void invalidateAll(Long userId, String exceptSessionId) {
        sessions.findByPrincipalName(String.valueOf(userId)).keySet().stream()
                .filter(id -> !id.equals(exceptSessionId))
                .forEach(sessions::deleteById);
    }
}
