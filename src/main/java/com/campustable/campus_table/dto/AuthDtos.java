package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Role;
import com.campustable.campus_table.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private static final String SN_REGEX = "^\\d{8}$";
    private static final String SN_MESSAGE = "학번은 숫자 8자리여야 합니다.";

    private AuthDtos() {
    }

    /** 인증 메일은 {학번}@학교도메인 으로 발송된다. */
    public record EmailSendRequest(
            @NotBlank(message = "학번을 입력해주세요.") @Pattern(regexp = SN_REGEX, message = SN_MESSAGE) String studentNumber) {
    }

    public record EmailVerifyRequest(
            @NotBlank(message = "학번을 입력해주세요.") @Pattern(regexp = SN_REGEX, message = SN_MESSAGE) String studentNumber,
            @NotBlank(message = "인증 코드를 입력해주세요.") String code) {
    }

    public record SignupRequest(
            @NotBlank(message = "학번을 입력해주세요.") @Pattern(regexp = SN_REGEX, message = SN_MESSAGE) String studentNumber,
            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, max = 50, message = "비밀번호는 8~50자여야 합니다.") String password,
            @NotBlank(message = "이름을 입력해주세요.") @Size(max = 50, message = "이름은 50자 이하여야 합니다.") String name) {
    }

    public record LoginRequest(
            @NotBlank(message = "학번을 입력해주세요.") String studentNumber,
            @NotBlank(message = "비밀번호를 입력해주세요.") String password) {
    }

    public record MessageResponse(String message) {
    }

    public record UserResponse(Long userId, String studentNumber, String name, Role role) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getStudentNumber(), u.getName(), u.getRole());
        }
    }
}
