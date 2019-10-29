package com.hfx.keycloak.spi;

import com.hfx.keycloak.VerificationCodeRepresentation;
import org.keycloak.provider.Provider;
import com.hfx.keycloak.SmsException;

import java.util.Map;

public interface SmsService<T> extends Provider {
    public boolean send(String phoneNumber, Map<String, ? super T> params) throws SmsException;

    public boolean sendVerificationCode(VerificationCodeRepresentation rep, Map<String, ? super T> params) throws SmsException;
}
