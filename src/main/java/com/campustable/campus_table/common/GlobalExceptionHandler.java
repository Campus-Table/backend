package com.campustable.campus_table.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> uploadTooLarge(Exception e) {
        return build(ErrorResponse.of(ErrorCode.IMAGE_TOO_LARGE));
    }

    @ExceptionHandler(CustomException.class)
    ResponseEntity<ErrorResponse> custom(CustomException e) {
        return build(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        var field = e.getBindingResult().getFieldError();
        String msg = field == null ? ErrorCode.VALIDATION_FAILED.getMessage() : field.getDefaultMessage();
        return build(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        return build(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
        return build(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) throws Exception {
        // 404/405/415 등 Spring이 상태코드를 이미 정한 예외는 그 상태코드를 유지
        if (e instanceof org.springframework.web.ErrorResponse er) {
            int s = er.getStatusCode().value();
            return ResponseEntity.status(s)
                    .body(new ErrorResponse(s, ErrorCode.INVALID_REQUEST.name(), ErrorCode.INVALID_REQUEST.getMessage()));
        }
        log.error("Unhandled exception", e);
        return build(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ErrorResponse> build(ErrorResponse body) {
        return ResponseEntity.status(body.status()).body(body);
    }
}
