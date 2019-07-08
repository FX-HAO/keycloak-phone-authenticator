package org.hfx.keycloak;

import org.hfx.keycloak.jpa.VerificationCode;

import java.util.Date;

public class VerificationCodeRepresentation {

    private String id;
    private String phoneNumber;
    private String code;
    private Date expiresAt;
    private Date createdAt;

    public VerificationCodeRepresentation() {
    }

    public VerificationCodeRepresentation(VerificationCode veriCode) {
        id = veriCode.getId();
        phoneNumber = veriCode.getPhoneNumber();
        code = veriCode.getCode();
        expiresAt = veriCode.getExpiresAt();
        createdAt = veriCode.getCreatedAt();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
