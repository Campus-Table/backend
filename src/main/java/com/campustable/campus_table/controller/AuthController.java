package com.campustable.campus_table.controller;

import com.campustable.campus_table.dto.AuthDtos.*;
import com.campustable.campus_table.service.AuthService;
import com.campustable.campus_table.service.EmailVerificationService;
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

    @PostMapping("/email/send")
    public MessageResponse sendCode(@Valid @RequestBody EmailSendRequest req) {
        emailVerification.sendCode(req.email());
        return new MessageResponse("Verification code sent");
    }

    @PostMapping("/email/verify")
    public MessageResponse verify(@Valid @RequestBody EmailVerifyRequest req) {
        emailVerification.verify(req.email(), req.code());
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
}
