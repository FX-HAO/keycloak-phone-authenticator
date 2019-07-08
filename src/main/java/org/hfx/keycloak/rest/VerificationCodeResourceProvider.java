package org.hfx.keycloak.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class VerificationCodeResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public VerificationCodeResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new VerificationCodeResource(session);
    }

    @Override
    public void close() {
    }

}
