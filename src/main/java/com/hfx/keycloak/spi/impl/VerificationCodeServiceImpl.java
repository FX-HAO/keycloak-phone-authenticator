package com.hfx.keycloak.spi.impl;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import com.hfx.keycloak.VerificationCodeRepresentation;
import com.hfx.keycloak.jpa.VerificationCode;
import com.hfx.keycloak.spi.VerificationCodeService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.*;

public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final KeycloakSession session;

    public VerificationCodeServiceImpl(KeycloakSession session) {
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
    public List<VerificationCodeRepresentation> listVerificationCodes() {
        List<VerificationCode> verificationCodesEntities = getEntityManager().createNamedQuery("VerificationCode.findByRealm", VerificationCode.class)
                .setParameter("realmId", getRealm().getId())
                .getResultList();
        List<VerificationCodeRepresentation> result = new LinkedList<>();
        for (VerificationCode entity : verificationCodesEntities) {
            result.add(new VerificationCodeRepresentation(entity));
        }
        return result;
    }

    protected static String getCode() {
        // It will generate 4 digit random Number.
        // from 0 to 9999
        Random rnd = new Random();
        int number = rnd.nextInt(9999);

        // this will convert any number sequence into 4 character.
        return String.format("%04d", number);
    }

    @Override
    public VerificationCodeRepresentation addVerificationCode(VerificationCodeRepresentation veriCode) {
        VerificationCode entity = new VerificationCode();
        String id = veriCode.getId() == null ? KeycloakModelUtils.generateId() : veriCode.getId();
        entity.setId(id);
        entity.setPhoneNumber(veriCode.getPhoneNumber());
        entity.setCode(getCode());
        entity.setKind(Optional.ofNullable(veriCode.getKind()).orElse(""));
        entity.setRealmId(getRealm().getId());
        Instant now = Instant.now();
        entity.setCreatedAt(Date.from(now));
        entity.setExpiresAt(Date.from(now.plusSeconds(5 * 60)));
        getEntityManager().persist(entity);

        return new VerificationCodeRepresentation(entity);
    }

    @Override
    public void close() {
    }
}
