package org.hfx.keycloak.spi;

import org.hfx.keycloak.VerificationCodeRepresentation;
import org.keycloak.provider.Provider;
import org.hfx.keycloak.SmsException;

import java.util.Map;

public interface SmsService<T> extends Provider {
    public boolean send(String phoneNumber, Map<String, ? super T> params) throws SmsException;

    public boolean sendVerificationCode(VerificationCodeRepresentation rep, Map<String, ? super T> params) throws SmsException;
}
