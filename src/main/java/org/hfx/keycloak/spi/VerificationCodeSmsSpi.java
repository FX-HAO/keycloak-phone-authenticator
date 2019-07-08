package org.hfx.keycloak.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class VerificationCodeSmsSpi implements Spi {
    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "verification-code-sms";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return SmsService.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return SmsServiceProviderFactory.class;
    }
}
