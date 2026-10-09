package com.campustable.campus_table.controller;

import com.campustable.campus_table.dto.AuthDtos.*;
import com.campustable.campus_table.service.AuthService;
import com.campustable.campus_table.service.EmailVerificationService;
import com.campustable.campus_table.service.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerification;
    private final PasswordService passwordService;

    @PostMapping("/email/send")
    public MessageResponse sendCode(@Valid @RequestBody EmailSendRequest req, HttpServletRequest request) {
        emailVerification.sendCode(req.studentNumber(), request.getRemoteAddr());
        return new MessageResponse("Verification code sent");
    }

    @PostMapping("/email/verify")
    public MessageResponse verify(@Valid @RequestBody EmailVerifyRequest req) {
        emailVerification.verify(req.studentNumber(), req.code());
        return new MessageResponse("Email verified");
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest req) {
        return authService.signup(req);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest req,
                              HttpServletRequest request, HttpServletResponse response) {
        return authService.login(req, request, response);
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest request) {
        authService.logout(request);
        return new MessageResponse("Logout successful");
    }

    /** 가입된 학번이 아니어도 같은 응답을 준다 (가입 여부 노출 방지). */
    @PostMapping("/password/reset/send")
    public MessageResponse sendResetCode(@Valid @RequestBody PasswordResetSendRequest req,
                                         HttpServletRequest request) {
        passwordService.sendResetCode(req.studentNumber(), request.getRemoteAddr());
        return new MessageResponse("Verification code sent");
    }

    @PostMapping("/password/reset/confirm")
    public MessageResponse confirmReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        passwordService.confirmReset(req);
        return new MessageResponse("Password reset");
    }
}
