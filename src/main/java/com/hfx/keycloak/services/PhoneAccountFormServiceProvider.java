package com.hfx.keycloak.services;

import com.hfx.keycloak.rest.VerificationCodeResource;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;

public class PhoneAccountFormServiceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public PhoneAccountFormServiceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        KeycloakContext ctx = session.getContext();
        RealmModel realm = ctx.getRealm();
        EventBuilder event = new EventBuilder(realm, session, ctx.getConnection());
        ClientModel client = ctx.getClient();
        if (client == null) {
            client = realm.getClientByClientId("account");
        }
        return new PhoneAccountFormService(session, realm, client, event);
    }

    @Override
    public void close() {
    }

}
