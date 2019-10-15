package org.hfx.keycloak.rest;

import org.keycloak.models.KeycloakSession;

import javax.ws.rs.Path;

public class VerificationCodeRestResource {
    private final KeycloakSession session;

    public VerificationCodeRestResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("verification_codes")
    public VerificationCodeResource getCompanyResource() {
        return new VerificationCodeResource(session);
    }
}
