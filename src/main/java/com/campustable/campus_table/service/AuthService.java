package com.campustable.campus_table.service;

import com.campustable.campus_table.common.CustomException;
import com.campustable.campus_table.common.ErrorCode;
import com.campustable.campus_table.config.AuthUser;
import com.campustable.campus_table.dto.AuthDtos.LoginRequest;
import com.campustable.campus_table.dto.AuthDtos.SignupRequest;
import com.campustable.campus_table.dto.AuthDtos.UserResponse;
import com.campustable.campus_table.entity.User;
import com.campustable.campus_table.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerification;
    private final LoginAttemptService loginAttempts;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("dummy-password-for-timing");

    @Transactional
    public UserResponse signup(SignupRequest req) {
        if (userRepository.existsByStudentNumber(req.studentNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
        emailVerification.consumeVerified(req.studentNumber());
        try {
            User user = userRepository.saveAndFlush(User.builder()
                    .studentNumber(req.studentNumber())
                    .email(emailVerification.emailOf(req.studentNumber()))
                    .password(passwordEncoder.encode(req.password()))
                    .name(req.name().trim())
                    .build());
            return UserResponse.from(user);
        } catch (DataIntegrityViolationException e) { // 동시 가입 경합
            throw new CustomException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        String ip = request.getRemoteAddr();
        loginAttempts.begin(req.studentNumber(), ip); // 시도 횟수를 먼저 올리고, 한도를 넘었으면 비밀번호가 맞아도 거절

        User found = userRepository.findByStudentNumber(req.studentNumber()).orElse(null);
        // 없는 학번도 같은 시간만큼 해시 비교를 해서, 응답 시간으로 가입 여부를 알 수 없게 한다
        boolean ok = passwordEncoder.matches(req.password(), found == null ? DUMMY_HASH : found.getPassword());
        if (found == null || !ok) {
            loginAttempts.recordFailure(ip);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        loginAttempts.reset(req.studentNumber());
        User user = found;

        var principal = new AuthUser(user.getId(), user.getStudentNumber(), user.getRole());
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        if (request.getSession(false) != null) {
            request.changeSessionId(); // 세션 고정 공격 방지
        }
        contextRepository.saveContext(context, request, response);
        return UserResponse.from(user);
    }

    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
