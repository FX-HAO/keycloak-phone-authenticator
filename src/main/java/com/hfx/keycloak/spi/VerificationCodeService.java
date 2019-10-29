package com.hfx.keycloak.spi;

import com.hfx.keycloak.VerificationCodeRepresentation;
import org.keycloak.provider.Provider;

import java.util.List;

public interface VerificationCodeService extends Provider {
    List<VerificationCodeRepresentation> listVerificationCodes();

    VerificationCodeRepresentation addVerificationCode(VerificationCodeRepresentation veriCode);
}
