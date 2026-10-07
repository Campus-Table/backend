package com.campustable.campus_table.dto;

import com.campustable.campus_table.entity.Role;
import com.campustable.campus_table.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record EmailSendRequest(
            @NotBlank(message = "이메일을 입력해주세요.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email) {
    }

    public record EmailVerifyRequest(
            @NotBlank(message = "이메일을 입력해주세요.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,
            @NotBlank(message = "인증 코드를 입력해주세요.") String code) {
    }

    public record SignupRequest(
            @NotBlank(message = "학번을 입력해주세요.")
            @Pattern(regexp = "^\\d{8}$", message = "학번은 숫자 8자리여야 합니다.") String studentNumber,
            @NotBlank(message = "이메일을 입력해주세요.") @Email(message = "이메일 형식이 올바르지 않습니다.") String email,
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
