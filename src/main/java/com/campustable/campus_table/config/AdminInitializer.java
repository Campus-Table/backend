package com.campustable.campus_table.config;

import com.campustable.campus_table.entity.Role;
import com.campustable.campus_table.entity.User;
import com.campustable.campus_table.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 기동 시 환경변수의 관리자 계정이 없으면 생성한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.student-number:}")
    private String studentNumber;
    @Value("${app.admin.email:}")
    private String email;
    @Value("${app.admin.password:}")
    private String password;
    @Value("${app.admin.name:관리자}")
    private String name;

    @Override
    public void run(ApplicationArguments args) {
        if (studentNumber.isBlank() || password.isBlank() || userRepository.existsByStudentNumber(studentNumber)) {
            return;
        }
        userRepository.save(User.builder()
                .studentNumber(studentNumber)
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(Role.ADMIN)
                .build());
        log.info("관리자 계정 생성: {}", studentNumber);
    }
}
