package com.campustable.campus_table.config;

import com.campustable.campus_table.entity.Role;
import java.io.Serializable;

/** 세션(Redis)에 저장되는 로그인 사용자 정보. */
public record AuthUser(Long userId, String studentNumber, Role role) implements Serializable {
}
