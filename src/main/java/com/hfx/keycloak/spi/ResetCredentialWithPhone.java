package com.hfx.keycloak.spi;

import com.hfx.keycloak.rest.VerificationCodeResource;
import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.resetcred.ResetCredentialChooseUser;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TemporalType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ResetCredentialWithPhone extends ResetCredentialChooseUser {

    private static final Logger logger = Logger.getLogger(ResetCredentialWithPhone.class);

    public static final String PROVIDER_ID = "reset-credentials-with-phone";

    public static final String RECAPTCHA_SITE_KEY = "phoneNumber.recaptcha.siteKey";
    public static final String RECAPTCHA_SECRET = "phoneNumber.recaptcha.secret";

    private static final String VERIFICATION_CODE_KIND = "reset-credential";

    public static final String NOT_SEND_EMAIL = "should-send-email";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String siteKey = null;
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig != null && authenticatorConfig.getConfig() != null) {
            siteKey = authenticatorConfig.getConfig().get(RECAPTCHA_SITE_KEY);
            String secret = authenticatorConfig.getConfig().get(RECAPTCHA_SECRET);

            RealmModel realm = context.getRealm();
            if (StringUtils.isNotEmpty(siteKey) && StringUtils.isNotEmpty(secret)) {
                if (StringUtils.isEmpty(realm.getAttribute(VerificationCodeResource.CAPTCHA_KEY)) ||
                        StringUtils.isEmpty(realm.getAttribute(VerificationCodeResource.CAPTCHA_SECRET))) {
                    realm.setAttribute(VerificationCodeResource.CAPTCHA_KEY, siteKey);
                    realm.setAttribute(VerificationCodeResource.CAPTCHA_SECRET, secret);
                }
            } else {
                if (StringUtils.isNotEmpty(realm.getAttribute(VerificationCodeResource.CAPTCHA_KEY))) {
                    realm.removeAttribute(VerificationCodeResource.CAPTCHA_KEY);
                }
                if (StringUtils.isNotEmpty(realm.getAttribute(VerificationCodeResource.CAPTCHA_SECRET))) {
                    realm.removeAttribute(VerificationCodeResource.CAPTCHA_SECRET);
                }
            }
        }

        super.authenticate(context);

        Response challenge = context.form()
                .setAttribute("captchaKey", siteKey)
                .setAttribute("verificationCodeKind", VERIFICATION_CODE_KIND)
                .createForm("login-reset-password-with-phone.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        EventBuilder event = context.getEvent();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String username = formData.getFirst("username");
        String phoneNumber = formData.getFirst("phoneNumber");
        String siteKey = null;
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig != null && authenticatorConfig.getConfig() != null) {
            siteKey = authenticatorConfig.getConfig().get(RECAPTCHA_SITE_KEY);
        }

        if ((username == null || username.isEmpty()) && (phoneNumber == null || phoneNumber.isEmpty())) {
            if (authenticatorConfig != null && authenticatorConfig.getConfig() != null) {
                siteKey = authenticatorConfig.getConfig().get(RECAPTCHA_SITE_KEY);
            }
            event.error(Errors.USERNAME_MISSING);
            Response challenge = context.form()
                    .setError(Messages.MISSING_USERNAME)
                    .setAttribute("captchaKey", siteKey)
                    .setAttribute("verificationCodeKind", VERIFICATION_CODE_KIND)
                    .createForm("login-reset-password-with-phone.ftl");
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
            return;
        }

        RealmModel realm = context.getRealm();
        UserModel user = context.getSession().users().getUserByUsername(
                Optional.ofNullable(username).map(String::trim).orElse(""), realm);
        if (user == null && realm.isLoginWithEmailAllowed() && username != null && username.contains("@")) {
            user = context.getSession().users().getUserByEmail(username, realm);
        }

        if (user == null) {
            List<UserModel> users = context.getSession().users().searchForUserByUserAttribute(
                    "phoneNumber", phoneNumber, context.getRealm());
            if (users.isEmpty() || !validateVerificationCode(context)) {
                Response challenge = context.form()
                        .setError(Messages.INVALID_USER)
                        .setAttribute("captchaKey", siteKey)
                        .setAttribute("verificationCodeKind", VERIFICATION_CODE_KIND)
                        .createForm("login-reset-password-with-phone.ftl");
                context.failureChallenge(AuthenticationFlowError.INVALID_USER, challenge);
                return;
            }
            user = users.get(0);

            context.getAuthenticationSession().setAuthNote(NOT_SEND_EMAIL, "");
        }

        context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);

        // we don't want people guessing usernames, so if there is a problem, just continue, but don't set the user
        // a null user will notify further executions, that this was a failure.
        if (user == null) {
            event.clone()
                    .detail(Details.USERNAME, username)
                    .error(Errors.USER_NOT_FOUND);
        } else if (!user.isEnabled()) {
            event.clone()
                    .detail(Details.USERNAME, username)
                    .user(user).error(Errors.USER_DISABLED);
        } else {
            context.setUser(user);
        }

        context.success();
    }


    private boolean validateVerificationCode(AuthenticationFlowContext context) {
        String phoneNumber = Optional.ofNullable(context.getHttpRequest().getDecodedFormParameters().getFirst("phone_number")).orElse(
                context.getHttpRequest().getDecodedFormParameters().getFirst("phoneNumber"));
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

        try {
            EntityManager entityManager = context.getSession().getProvider(JpaConnectionProvider.class).getEntityManager();
            Integer veriCode = entityManager.createNamedQuery("VerificationCode.validateVerificationCode", Integer.class)
                    .setParameter("realmId", context.getRealm().getId())
                    .setParameter("phoneNumber", phoneNumber)
                    .setParameter("code", code)
                    .setParameter("now", new Date(), TemporalType.TIMESTAMP)
                    .setParameter("kind", VERIFICATION_CODE_KIND)
                    .getSingleResult();
            if (veriCode == 1) {
                return true;
            }
        }
        catch (NoResultException err){ }
        return false;
    }

    @Override
    public String getDisplayType() {
        return "Reset Credential With Phone";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return this;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(RECAPTCHA_SITE_KEY);
        property.setLabel("recaptcha site key");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("recaptcha site key");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(RECAPTCHA_SECRET);
        property.setLabel("recaptcha secret");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("recaptcha secret");
        configProperties.add(property);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

}
