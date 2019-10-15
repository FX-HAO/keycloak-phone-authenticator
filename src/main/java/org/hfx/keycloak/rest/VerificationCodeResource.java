package org.hfx.keycloak.rest;

import org.hfx.keycloak.SmsException;
import org.hfx.keycloak.spi.SmsService;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.hfx.keycloak.VerificationCodeRepresentation;
import org.hfx.keycloak.spi.VerificationCodeService;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerificationCodeResource {

    private static final Logger log = Logger.getLogger(VerificationCodeResource.class);

    private final KeycloakSession session;

    public VerificationCodeResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<VerificationCodeRepresentation> getVerificationCodes() {
        return session.getProvider(VerificationCodeService.class).listVerificationCodes();
    }

    @POST
    @Path("")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createVerificationCode(VerificationCodeRepresentation rep) {
        VerificationCodeRepresentation vc = session.getProvider(VerificationCodeService.class).addVerificationCode(rep);
        Map<String, Object> params = new HashMap<>();
        try {
            session.getProvider(SmsService.class).sendVerificationCode(vc, params);
        }
        catch (SmsException e) {
            log.error(e.getMessage());
        }
        return Response.noContent().build();
    }
}
