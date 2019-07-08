package org.hfx.keycloak.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class VerificationCodeRestSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "verification-code";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return VerificationCodeService.class;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return VerificationCodeServiceProviderFactory.class;
    }
}
