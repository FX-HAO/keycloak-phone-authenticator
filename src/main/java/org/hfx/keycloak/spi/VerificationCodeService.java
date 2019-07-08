package org.hfx.keycloak.spi;

import org.hfx.keycloak.VerificationCodeRepresentation;
import org.keycloak.provider.Provider;

import java.util.List;

public interface VerificationCodeService extends Provider {
    List<VerificationCodeRepresentation> listVerificationCodes();

    VerificationCodeRepresentation addVerificationCode(VerificationCodeRepresentation veriCode);
}
