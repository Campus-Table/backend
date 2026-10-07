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
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    @Transactional
    public UserResponse signup(SignupRequest req) {
        String email = emailVerification.normalize(req.email());
        emailVerification.checkDomain(email);
        if (userRepository.existsByStudentNumber(req.studentNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_STUDENT_NUMBER);
        }
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        emailVerification.consumeVerified(email);
        try {
            User user = userRepository.saveAndFlush(User.builder()
                    .studentNumber(req.studentNumber())
                    .email(email)
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
        User user = userRepository.findByStudentNumber(req.studentNumber())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

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
