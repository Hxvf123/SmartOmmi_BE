package com.smartomni.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception nghiep vu goc - moi exception nghiep vu trong cac service
 * nen ke thua class nay de GlobalExceptionHandler xu ly thong nhat.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }

    public BusinessException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
