package com.hfx.keycloak.rest;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.Map;

public class RecaptchaResource {

    private static final Logger log = Logger.getLogger(RecaptchaResource.class);

    private final KeycloakSession session;

    public RecaptchaResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("key")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVerificationCodes() {
        Map<String, String> map = new HashMap<>();
        map.put("capacha_key",
                session.getContext().getRealm().getAttribute(VerificationCodeResource.CAPTCHA_KEY));
        return Response.status(Response.Status.OK).entity(map).build();
    }

}
