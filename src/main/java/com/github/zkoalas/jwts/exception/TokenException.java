package com.github.zkoalas.jwts.exception;

/**
 * TokenException
 */
public abstract class TokenException extends RuntimeException {
    private static final long serialVersionUID = 2413958299445359500L;
    private int code;

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public TokenException(int code, String message) {
        super(message);
        this.code = code;
    }
}
