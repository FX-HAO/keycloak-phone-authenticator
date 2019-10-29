package com.hfx.keycloak;

public class SmsException extends Exception {
    private int statusCode = -1;
    private int errorCode = -1;
    private String errorMessage = null;

    public SmsException(String message) {
        this(message, null);
    }

    public SmsException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
