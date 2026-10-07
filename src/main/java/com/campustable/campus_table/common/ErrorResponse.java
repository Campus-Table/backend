package com.campustable.campus_table.common;

public record ErrorResponse(int status, String code, String message) {

    public static ErrorResponse of(ErrorCode e) {
        return new ErrorResponse(e.getStatus().value(), e.name(), e.getMessage());
    }

    public static ErrorResponse of(ErrorCode e, String message) {
        return new ErrorResponse(e.getStatus().value(), e.name(), message);
    }
}
