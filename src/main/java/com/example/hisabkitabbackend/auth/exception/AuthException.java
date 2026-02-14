package com.example.hisabkitabbackend.auth.exception;

import com.example.hisabkitabbackend.common.exception.BusinessException;

public class AuthException extends BusinessException {

    public AuthException(String message) {
        super("AUTH_ERROR", message);
    }
}
