package com.hfx.keycloak.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class RecaptchaResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public RecaptchaResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new RecaptchaResource(session);
    }

    @Override
    public void close() {
    }
}
