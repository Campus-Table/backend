package com.campustable.campus_table.config;

import com.campustable.campus_table.entity.Role;
import java.io.Serializable;
import java.security.Principal;

/**
 * 세션(Redis)에 저장되는 로그인 사용자 정보.
 * Principal의 이름을 사용자 ID로 두어, 세션 저장소가 "사용자별 세션"을 색인할 수 있게 한다
 * (비밀번호 변경 시 다른 기기의 세션을 종료하는 데 사용).
 */
public record AuthUser(Long userId, String studentNumber, Role role) implements Serializable, Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
