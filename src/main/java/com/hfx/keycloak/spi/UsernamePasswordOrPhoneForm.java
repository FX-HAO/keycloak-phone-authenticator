/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hfx.keycloak.spi;

import com.hfx.keycloak.util.Recaptcha;
import com.hfx.keycloak.util.VerificationCode;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.theme.Theme;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsernamePasswordOrPhoneForm extends UsernamePasswordForm {
    protected static ServicesLogger log = ServicesLogger.LOGGER;

    private static final String VERIFICATION_CODE_KIND = "username-password-or-phone";

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }
        if (!validateForm(context, formData)) {
            Response challengeResponse = challenge(context, formData);
            context.challenge(challengeResponse);
            return;
        }
        context.success();
    }

    private boolean badPasswordHandler(AuthenticationFlowContext context, UserModel user, boolean clearUser, boolean isEmptyPassword) {
        context.getEvent().user(user);
        context.getEvent().error("invalid_user_credentials");
        Response challengeResponse = this.challenge(context, this.getDefaultChallengeMessage(context));
        if (isEmptyPassword) {
            context.forceChallenge(challengeResponse);
        } else {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
        }

        if (clearUser) {
            context.clearUser();
        }

        return false;
    }

    @Override
    public boolean validatePassword(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData, boolean clearUser) {
        String password = inputData.getFirst(CredentialRepresentation.PASSWORD);
        if (password == null || password.isEmpty()) {
            return badPasswordHandler(context, user, clearUser,true);
        }

        if (isDisabledByBruteForce(context, user)) return false;

        if (password != null && !password.isEmpty() && context.getSession().userCredentialManager().isValid(context.getRealm(), user, UserCredentialModel.password(password))) {
            return true;
        } else {
            return badPasswordHandler(context, user, clearUser,false);
        }
    }

    private UserModel getUser(AuthenticationFlowContext context, MultivaluedMap<String, String> inputData) {
        String username = (String)inputData.getFirst("username");
        String phoneNumber = (String)inputData.getFirst("phoneNumber");

        if (username == null && phoneNumber == null) {
            context.getEvent().error("user_not_found");
            Response challengeResponse = this.challenge(context, this.getDefaultChallengeMessage(context));
            context.failureChallenge(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return null;
        } else {
            if (username != null) {
                username = username.trim();
                context.getEvent().detail("username", username);
                context.getAuthenticationSession().setAuthNote("ATTEMPTED_USERNAME", username);
            } else if (phoneNumber != null) {
                phoneNumber = phoneNumber.trim();
                context.getEvent().detail("phone number", phoneNumber);
                context.getAuthenticationSession().setAuthNote("ATTEMPTED_PHONE_NUMBER", phoneNumber);
            }
            UserModel user = null;

            try {
                if (username != null) {
                    user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
                } else {
                    user = context.getSession().users().searchForUserByUserAttribute(
                            "phoneNumber", phoneNumber, context.getRealm()).stream().findFirst().orElse(null);
                }
            } catch (ModelDuplicateException var6) {
                ServicesLogger.LOGGER.modelDuplicateException(var6);
                if (var6.getDuplicateFieldName() != null && var6.getDuplicateFieldName().equals("email")) {
                    this.setDuplicateUserChallenge(context, "email_in_use", "emailExistsMessage", AuthenticationFlowError.INVALID_USER);
                } else {
                    this.setDuplicateUserChallenge(context, "username_in_use", "usernameExistsMessage", AuthenticationFlowError.INVALID_USER);
                }

                return user;
            }

            this.testInvalidUser(context, user);
            return user;
        }
    }

    private boolean validateUser(AuthenticationFlowContext context, UserModel user, MultivaluedMap<String, String> inputData) {
        if (!this.enabledUser(context, user)) {
            return false;
        } else {
            String rememberMe = (String)inputData.getFirst("rememberMe");
            boolean remember = rememberMe != null && rememberMe.equalsIgnoreCase("on");
            if (remember) {
                context.getAuthenticationSession().setAuthNote("remember_me", "true");
                context.getEvent().detail("remember_me", "true");
            } else {
                context.getAuthenticationSession().removeAuthNote("remember_me");
            }

            context.setUser(user);
            return true;
        }
    }

    public boolean validateVerificationCode(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        context.clearUser();
        UserModel user = this.getUser(context, formData);
        return user != null && VerificationCode.verify(context, VERIFICATION_CODE_KIND) && this.validateUser(context, user, formData);
    }

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        return validateUserAndPassword(context, formData) || validateVerificationCode(context, formData);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl<>();
        String loginHint = context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);

        String rememberMeUsername = AuthenticationManager.getRememberMeUsername(context.getRealm(), context.getHttpRequest().getHttpHeaders());

        if (loginHint != null || rememberMeUsername != null) {
            if (loginHint != null) {
                formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
            } else {
                formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                formData.add("rememberMe", "on");
            }
        }
        Response challengeResponse = challenge(context, formData);
        context.challenge(challengeResponse);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = context.form();

        if (formData.size() > 0) forms.setFormData(formData);

        String siteKey = Recaptcha.getSiteKeyAndEnableRecaptcha(context);
        return forms.setAttribute("captchaKey", siteKey)
                .setAttribute("verificationCodeKind", VERIFICATION_CODE_KIND)
                .setAttribute("login", new LoginBean((formData)))
                .createForm("login-with-phone.ftl");
    }

}
