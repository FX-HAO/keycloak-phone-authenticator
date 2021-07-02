package com.hfx.keycloak.services;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.UriUtils;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.account.AccountPages;
import org.keycloak.forms.account.AccountProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.AbstractSecuredLocalService;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.resources.account.AccountFormService;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.utils.UserUpdateHelper;
import org.keycloak.userprofile.validation.UserProfileValidationResult;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TemporalType;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.keycloak.userprofile.profile.UserProfileContextFactory.forOldAccount;

public class PhoneAccountFormService extends AbstractSecuredLocalService {
    private static Set<String> VALID_PATHS = new HashSet<>();

    static {
        for (Method m : AccountFormService.class.getMethods()) {
            Path p = m.getAnnotation(Path.class);
            if (p != null) {
                VALID_PATHS.add(p.value());
            }
        }
    }

    // Used when some other context (ie. IdentityBrokerService) wants to forward error to account management and display it here
    public static final String ACCOUNT_MGMT_FORWARDED_ERROR_NOTE = "ACCOUNT_MGMT_FORWARDED_ERROR";

    private static final Logger logger = Logger.getLogger(PhoneAccountFormService.class);
    private final AppAuthManager authManager;
    private EventBuilder event;
    private AccountProvider account;
    private EventStoreProvider eventStore;

    public PhoneAccountFormService(KeycloakSession session, RealmModel realm, ClientModel client, EventBuilder event) {
        super(realm, client);
        this.session = session;
        this.headers = session.getContext().getRequestHeaders();
        this.clientConnection = session.getContext().getConnection();
        this.event = event;
        this.authManager = new AppAuthManager();
    }

    public void init() {
        this.eventStore = (EventStoreProvider)this.session.getProvider(EventStoreProvider.class);
        this.account = ((AccountProvider)this.session.getProvider(AccountProvider.class)).setRealm(this.realm).setUriInfo(this.session.getContext().getUri()).setHttpHeaders(this.headers);
        AuthenticationManager.AuthResult authResult = this.authManager.authenticateIdentityCookie(this.session, this.realm);
        if (authResult != null) {
            this.stateChecker = (String)this.session.getAttribute("state_checker");
            this.auth = new Auth(this.realm, authResult.getToken(), authResult.getUser(), this.client, authResult.getSession(), true);
            this.account.setStateChecker(this.stateChecker);
        }

        String requestOrigin = UriUtils.getOrigin(this.session.getContext().getUri().getBaseUri());
        String origin = (String)this.headers.getRequestHeaders().getFirst("Origin");
        if (origin != null && !origin.equals("null") && !requestOrigin.equals(origin)) {
            throw new ForbiddenException();
        } else {
            if (!this.request.getHttpMethod().equals("GET")) {
                String referrer = (String)this.headers.getRequestHeaders().getFirst("Referer");
                if (referrer != null && !requestOrigin.equals(UriUtils.getOrigin(referrer))) {
                    throw new ForbiddenException();
                }
            }

            if (authResult != null) {
                UserSessionModel userSession = authResult.getSession();
                if (userSession != null) {
                    AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(this.client.getId());
                    if (clientSession == null) {
                        clientSession = this.session.sessions().createClientSession(userSession.getRealm(), this.client, userSession);
                    }

                    this.auth.setClientSession(clientSession);
                }

                this.account.setUser(this.auth.getUser());
            }

            this.account.setFeatures(this.realm.isIdentityFederationEnabled(), this.eventStore != null && this.realm.isEventsEnabled(), true, true);
        }
    }

    protected Set<String> getValidPaths() {
        return PhoneAccountFormService.VALID_PATHS;
    }

    @Override
    protected URI getBaseRedirectUri() {
        return Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName());
    }

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processAccountUpdate(@Context HttpRequest request, final MultivaluedMap<String, String> formData) {
        this.request = request;
        init();

        if (auth == null) {
            return login(null);
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("Cancel")) {
            setReferrerOnPage();
            return account.createResponse(AccountPages.ACCOUNT);
        }

        csrfCheck(formData);

        UserModel user = auth.getUser();

        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        String oldEmail = user.getEmail();

        event.event(EventType.UPDATE_PROFILE).client(auth.getClient()).user(auth.getUser());

        UserProfileValidationResult result = forOldAccount(user, formData, session).validate();
        List<FormMessage> errors = Validation.getFormErrorsFromValidation(result);
        if (!errors.isEmpty()) {
            setReferrerOnPage();
            Response.Status status = Response.Status.OK;

            if (result.hasFailureOfErrorType(Messages.READ_ONLY_USERNAME)) {
                status = Response.Status.BAD_REQUEST;
            } else if (result.hasFailureOfErrorType(Messages.EMAIL_EXISTS, Messages.USERNAME_EXISTS)) {
                status = Response.Status.CONFLICT;
            }

            return account.setErrors(status, errors).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }

        String phoneNumber = Optional.ofNullable(formData.getFirst("user.attributes.phoneNumber")).map(String::trim).orElse("");
        List<String> phoneNumberAttr = user.getAttribute("phoneNumber");
        if (!"".equals(phoneNumber) && !phoneNumber.equals(phoneNumberAttr.stream().findFirst().orElse(""))) {
            if (!session.users().searchForUserByUserAttribute("phoneNumber", phoneNumber, realm).isEmpty()) {
                return account.setError(Response.Status.OK, "phoneNumberHasBeenUsed").setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
            }

            String verificationCode = formData.getFirst("verificationCode");

            boolean valid = false;
            try {
                EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                Integer veriCode = entityManager.createNamedQuery("VerificationCode.validateVerificationCode", Integer.class)
                        .setParameter("realmId", realm.getId())
                        .setParameter("phoneNumber", phoneNumber)
                        .setParameter("code", verificationCode)
                        .setParameter("now", new Date(), TemporalType.TIMESTAMP)
                        .setParameter("kind", "updatePhoneNumber")
                        .getSingleResult();
                if (veriCode == 1) {
                    valid = true;
                }
            }
            catch (NoResultException err){ }
            if (!valid) {
                return account.setError(Response.Status.OK, "invalidVerificationCode").setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
            }
        }

        UserProfile updatedProfile = result.getProfile();
        String newEmail = updatedProfile.getAttributes().getFirstAttribute(UserModel.EMAIL);
        String newFirstName = updatedProfile.getAttributes().getFirstAttribute(UserModel.FIRST_NAME);
        String newLastName = updatedProfile.getAttributes().getFirstAttribute(UserModel.LAST_NAME);


        try {
            // backward compatibility with old account console where attributes are not removed if missing
            UserUpdateHelper.updateAccountOldConsole(realm, user, updatedProfile);
        } catch (ReadOnlyException e) {
            setReferrerOnPage();
            return account.setError(Response.Status.BAD_REQUEST, Messages.READ_ONLY_USER).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }

        if (result.hasAttributeChanged(UserModel.FIRST_NAME)) {
            event.detail(Details.PREVIOUS_FIRST_NAME, oldFirstName).detail(Details.UPDATED_FIRST_NAME, newFirstName);
        }
        if (result.hasAttributeChanged(UserModel.LAST_NAME)) {
            event.detail(Details.PREVIOUS_LAST_NAME, oldLastName).detail(Details.UPDATED_LAST_NAME, newLastName);
        }
        if (result.hasAttributeChanged(UserModel.EMAIL)) {
            user.setEmailVerified(false);
            event.detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, newEmail);
        }

        event.success();
        setReferrerOnPage();
        return account.setSuccess(Messages.ACCOUNT_UPDATED).createResponse(AccountPages.ACCOUNT);

    }

    private void setReferrerOnPage() {
        String[] referrer = this.getReferrer();
        if (referrer != null) {
            this.account.setReferrer(referrer);
        }
    }

    private String[] getReferrer() {
        String referrer = (String)this.session.getContext().getUri().getQueryParameters().getFirst("referrer");
        if (referrer == null) {
            return null;
        } else {
            String referrerUri = (String)this.session.getContext().getUri().getQueryParameters().getFirst("referrer_uri");
            ClientModel referrerClient = this.realm.getClientByClientId(referrer);
            if (referrerClient != null) {
                if (referrerUri != null) {
                    referrerUri = RedirectUtils.verifyRedirectUri(this.session, referrerUri, referrerClient);
                } else {
                    referrerUri = ResolveRelative.resolveRelativeUri(this.session, referrerClient.getRootUrl(), referrerClient.getBaseUrl());
                }

                if (referrerUri != null) {
                    String referrerName = referrerClient.getName();
                    if (Validation.isBlank(referrerName)) {
                        referrerName = referrer;
                    }

                    return new String[]{referrerName, referrerUri};
                }
            } else if (referrerUri != null && this.client != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(this.session, referrerUri, this.client);
                if (referrerUri != null) {
                    return new String[]{referrer, referrerUri};
                }
            }

            return null;
        }
    }

    private void updateUsername(String username, UserModel user, KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        boolean usernameChanged = username == null || !user.getUsername().equals(username);
        if (realm.isEditUsernameAllowed() && !realm.isRegistrationEmailAsUsername()) {
            if (usernameChanged) {
                UserModel existing = session.users().getUserByUsername(username, realm);
                if (existing != null && !existing.getId().equals(user.getId())) {
                    throw new ModelDuplicateException("usernameExistsMessage");
                }

                user.setUsername(username);
            }
        } else if (usernameChanged) {
        }

    }

    private void updateEmail(String email, UserModel user, KeycloakSession session, EventBuilder event) {
        RealmModel realm = session.getContext().getRealm();
        String oldEmail = user.getEmail();
        boolean emailChanged = oldEmail != null ? !oldEmail.equals(email) : email != null;
        UserModel existing;
        if (emailChanged && !realm.isDuplicateEmailsAllowed()) {
            existing = session.users().getUserByEmail(email, realm);
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new ModelDuplicateException("emailExistsMessage");
            }
        }

        user.setEmail(email);
        if (emailChanged) {
            user.setEmailVerified(false);
            event.clone().event(EventType.UPDATE_EMAIL).detail("previous_email", oldEmail).detail("updated_email", email).success();
        }

        if (realm.isRegistrationEmailAsUsername()) {
            if (!realm.isDuplicateEmailsAllowed()) {
                existing = session.users().getUserByEmail(email, realm);
                if (existing != null && !existing.getId().equals(user.getId())) {
                    throw new ModelDuplicateException("usernameExistsMessage");
                }
            }

            user.setUsername(email);
        }

    }

    private void csrfCheck(MultivaluedMap<String, String> formData) {
        String formStateChecker = (String)formData.getFirst("stateChecker");
        if (formStateChecker == null || !formStateChecker.equals(this.stateChecker)) {
            throw new ForbiddenException();
        }
    }
}
