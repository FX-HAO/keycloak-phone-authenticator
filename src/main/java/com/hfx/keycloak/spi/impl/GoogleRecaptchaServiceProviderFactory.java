package com.hfx.keycloak.spi.impl;

import com.hfx.keycloak.spi.CaptchaService;
import com.hfx.keycloak.spi.CaptchaServiceProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class GoogleRecaptchaServiceProviderFactory implements CaptchaServiceProviderFactory {
    @Override
    public CaptchaService create(KeycloakSession session) {
        return new GoogleRecaptchaService(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "GoogleRecaptchaServiceProviderFactory";
    }
}
