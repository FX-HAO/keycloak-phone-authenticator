package com.hfx.keycloak.spi.impl;

import com.hfx.keycloak.spi.CaptchaService;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GoogleRecaptchaService implements CaptchaService {
    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";

    private final KeycloakSession session;

    public GoogleRecaptchaService(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean verify(String key, String secret, final MultivaluedMap<String, String> formParams) {
        boolean success = false;
        HttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        HttpPost post = new HttpPost("https://www.recaptcha.net/recaptcha/api/siteverify");
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(3000).build();
        List<NameValuePair> formparams = new LinkedList<>();
        post.setConfig(requestConfig);
        formparams.add(new BasicNameValuePair("secret", secret));
        formparams.add(new BasicNameValuePair("response", formParams.getFirst(G_RECAPTCHA_RESPONSE)));
        formparams.add(new BasicNameValuePair("remoteip", session.getContext().getConnection().getRemoteAddr()));
        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);
            HttpResponse response = httpClient.execute(post);
            InputStream content = response.getEntity().getContent();
            try {
                Map json = JsonSerialization.readValue(content, Map.class);
                Object val = json.get("success");
                success = Boolean.TRUE.equals(val);
            } finally {
                content.close();
            }
        } catch (Exception e) {
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }
        return success;
    }

    @Override
    public void close() {
    }
}
