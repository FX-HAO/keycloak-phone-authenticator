package org.hfx.keycloak.jpa;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class VerificationCodeJpaEntityProvider implements JpaEntityProvider {

    // List of your JPA entities.
    @Override
    public List<Class<?>> getEntities() {
        return Collections.<Class<?>>singletonList(VerificationCode.class);
    }

    // This is used to return the location of the Liquibase changelog file.
    // You can return null if you don't want Liquibase to create and update the DB schema.
    @Override
    public String getChangelogLocation() {
        return "META-INF/verification-code-changelog.xml";
    }

    @Override
    public void close() {}

    // Helper method, which will be used internally by Liquibase.
    @Override
    public String getFactoryId() {
        return VerificationCodeJpaEntityProviderFactory.ID;
    }
}
