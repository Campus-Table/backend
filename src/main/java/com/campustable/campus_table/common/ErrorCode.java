package com.campustable.campus_table.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // auth / user
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "학번 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_STUDENT_NUMBER(HttpStatus.CONFLICT, "이미 가입된 학번입니다."),
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일 발송에 실패했습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료되었습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),

    // cafeteria / store / menu
    CAFETERIA_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 학생식당을 찾을 수 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가게를 찾을 수 없습니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 메뉴를 찾을 수 없습니다."),
    MENU_UNAVAILABLE(HttpStatus.BAD_REQUEST, "주문할 수 없는 메뉴입니다."),

    // order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 주문을 찾을 수 없습니다."),
    ACTIVE_ORDER_EXISTS(HttpStatus.CONFLICT, "진행 중인 주문이 있습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "현재 주문 상태에서는 처리할 수 없습니다."),
    INVALID_ARRIVAL_CODE(HttpStatus.BAD_REQUEST, "현장 번호가 올바르지 않습니다."),

    // mileage
    INSUFFICIENT_MILEAGE(HttpStatus.BAD_REQUEST, "마일리지가 부족합니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제 승인에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
