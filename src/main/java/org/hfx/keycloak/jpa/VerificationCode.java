package org.hfx.keycloak.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "VERIFICATION_CODE")
@NamedQueries({
        @NamedQuery(name = "VerificationCode.validateVerificationCode", query = "SELECT 1 FROM VerificationCode t WHERE t.realmId = :realmId AND t.phoneNumber = :phoneNumber AND t.code = :code AND t.expiresAt >= :now "),
        @NamedQuery(name = "VerificationCode.findByRealm", query = "FROM VerificationCode t WHERE t.realmId = :realmId")
})
public class VerificationCode {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "PHONE_NUMBER", nullable = false)
    private String phoneNumber;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EXPIRES_AT")
    private Date expiresAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_AT")
    private Date createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}