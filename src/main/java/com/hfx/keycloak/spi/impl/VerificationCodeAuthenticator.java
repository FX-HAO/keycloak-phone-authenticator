package com.hfx.keycloak.spi.impl;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import com.hfx.keycloak.spi.BaseDirectGrantAuthenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import static com.hfx.keycloak.spi.VerificationCodeAuthenticatorFactory.KIND;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TemporalType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Optional;

public class VerificationCodeAuthenticator extends BaseDirectGrantAuthenticator {

    public static final String FAILURE_CHALLENGE = "_failure-challenge";

    private final KeycloakSession session;

    public VerificationCodeAuthenticator(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction("VERIFICATION_CODE_GRANT_CONFIG");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (!validateVerificationCode(context)) {
            context.getEvent().user(context.getUser());
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);

            Response challenge = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_grant", "Invalid user credentials");
            context.failure(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        context.success();
    }

    private boolean validateVerificationCode(AuthenticationFlowContext context) {
        String phoneNumber = Optional.ofNullable(context.getHttpRequest().getDecodedFormParameters().getFirst("phone_number")).orElse(
                context.getHttpRequest().getDecodedFormParameters().getFirst("phoneNumber"));
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst("code");
        String kind = null;
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig != null && authenticatorConfig.getConfig() != null) {
            kind = Optional.ofNullable(context.getAuthenticatorConfig().getConfig().get(KIND)).orElse("");
        }

        try {
            Integer veriCode = getEntityManager().createNamedQuery("VerificationCode.validateVerificationCode", Integer.class)
                    .setParameter("realmId", getRealm().getId())
                    .setParameter("phoneNumber", phoneNumber)
                    .setParameter("code", code)
                    .setParameter("now", new Date(), TemporalType.TIMESTAMP)
                    .setParameter("kind", kind)
                    .getSingleResult();
            if (veriCode == 1) {
                return true;
            }
        }
        catch (NoResultException err){ }
        return false;
    }

}
