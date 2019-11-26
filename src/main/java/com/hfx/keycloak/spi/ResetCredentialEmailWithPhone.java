package com.hfx.keycloak.spi;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialEmail;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;

public class ResetCredentialEmailWithPhone extends ResetCredentialEmail {
    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getExecution().isRequired() ||
                (context.getExecution().isConditional() &&
                        configuredFor(context))) {
            super.authenticate(context);
        } else {
            context.success();
        }
    }

    protected boolean configuredFor(AuthenticationFlowContext context) {
        if (context.getAuthenticationSession().getAuthNote(ResetCredentialWithPhone.NOT_SEND_EMAIL) == null) {
            return true;
        }
        return false;
    }

    @Override
    public String getId() {
        return "reset-credential-email-with-phone";
    }

    @Override
    public String getDisplayType() {
        return "Send Reset Email If Not Phone";
    }

    @Override
    public String getHelpText() {
        return "Send email to user if not phone provided.";
    }

    static {
        REQUIREMENT_CHOICES = new Requirement[]{
                Requirement.REQUIRED,
                Requirement.CONDITIONAL,
                Requirement.DISABLED
        };
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

}
