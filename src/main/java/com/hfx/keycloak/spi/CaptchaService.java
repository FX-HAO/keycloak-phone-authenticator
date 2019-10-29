package com.hfx.keycloak.spi;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.MultivaluedMap;

public interface CaptchaService extends Provider {
    boolean verify(String key, String secret, final MultivaluedMap<String, String> formParams);
}
