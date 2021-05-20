package com.hfx.keycloak.util;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.connections.jpa.JpaConnectionProvider;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.Optional;

public class VerificationCode {
    public static boolean verify(AuthenticationFlowContext context, String kind) {
        String phoneNumber = Optional.ofNullable(context.getHttpRequest().getDecodedFormParameters().getFirst("phone_number")).orElse(
                context.getHttpRequest().getDecodedFormParameters().getFirst("phoneNumber"));
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst("code");
        return verify(context, phoneNumber, code, kind);
    }

    public static boolean verify(AuthenticationFlowContext context, String phoneNumber, String code, String kind) {
        try {
            EntityManager entityManager = context.getSession().getProvider(JpaConnectionProvider.class).getEntityManager();
            Integer veriCode = entityManager.createNamedQuery("VerificationCode.validateVerificationCode", Integer.class)
                    .setParameter("realmId", context.getRealm().getId())
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
