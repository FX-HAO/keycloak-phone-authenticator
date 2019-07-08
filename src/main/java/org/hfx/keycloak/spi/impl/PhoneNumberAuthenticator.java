package org.hfx.keycloak.spi.impl;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.hfx.keycloak.spi.BaseDirectGrantAuthenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public class PhoneNumberAuthenticator extends BaseDirectGrantAuthenticator {
    public Response errorResponse(int status, String error, String errorDescription) {
        OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(error, errorDescription);
        return Response.status(status).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction("PHONE_NUMBER_GRANT_CONFIG");
    }

    protected UserModel findUser(AuthenticationFlowContext context) {
        List<UserModel> users = context.getSession().users().searchForUserByUserAttribute(
                "phone_number", context.getHttpRequest().getDecodedFormParameters().getFirst("phone_number"), context.getRealm());
        if (users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = findUser(context);
        if (user == null) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_grant", "Invalid user credentials");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }

        context.setUser(user);
        context.success();
    }
}
