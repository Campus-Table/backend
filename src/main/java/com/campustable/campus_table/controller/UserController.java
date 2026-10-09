package com.campustable.campus_table.controller;

import com.campustable.campus_table.config.AuthUser;
import com.campustable.campus_table.dto.AuthDtos.MessageResponse;
import com.campustable.campus_table.dto.AuthDtos.PasswordChangeRequest;
import com.campustable.campus_table.dto.AuthDtos.UserResponse;
import com.campustable.campus_table.service.AuthService;
import com.campustable.campus_table.service.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final PasswordService passwordService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return authService.me(user.userId());
    }

    /** 비밀번호 변경. 현재 기기를 제외한 다른 기기의 로그인은 해제된다. */
    @PatchMapping("/me/password")
    public MessageResponse changePassword(@AuthenticationPrincipal AuthUser user,
                                          @Valid @RequestBody PasswordChangeRequest req,
                                          HttpServletRequest request) {
        passwordService.change(user, req, request.getRemoteAddr(), request.getSession().getId());
        return new MessageResponse("Password changed");
    }
}
