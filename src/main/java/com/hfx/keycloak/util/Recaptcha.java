package com.hfx.keycloak.util;

import com.hfx.keycloak.rest.VerificationCodeResource;
import org.apache.commons.lang.StringUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;

public class Recaptcha {
    public static final String RECAPTCHA_SITE_KEY = "phoneNumber.recaptcha.siteKey";
    public static final String RECAPTCHA_SECRET = "phoneNumber.recaptcha.secret";

    public static String getSiteKeyAndEnableRecaptcha(AuthenticationFlowContext context) {
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

        return siteKey;
    }

    public static String getSiteKey(AuthenticationFlowContext context) {
        String siteKey = null;
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig != null && authenticatorConfig.getConfig() != null) {
            siteKey = authenticatorConfig.getConfig().get(RECAPTCHA_SITE_KEY);
        }
        return siteKey;
    }
}
