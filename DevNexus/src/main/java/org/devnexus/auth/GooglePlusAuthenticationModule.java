package org.devnexus.auth;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.devnexus.DevnexusApplication;
import org.devnexus.util.AccountUtil;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.http.HttpProvider;
import org.jboss.aerogear.android.impl.core.HttpProviderFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by summers on 12/5/13.
 */
public class GooglePlusAuthenticationModule extends AbstractAuthenticationModule {

    private static final String[] SCOPES = {"https://www.googleapis.com/auth/plus.login",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"};
    private static final String TAG = GooglePlusAuthenticationModule.class.getSimpleName();

    private final URL baseURL;
    private final String loginEndpoint;
    private final String logoutEndpoint;
    private final Context appContext;
    public static final String ACCOUNT_NAME = "GooglePlusAuthenticationModule.AccountName";
    public static final String ACCOUNT_ID = "GooglePlusAuthenticationModule.AccountId";
    protected final Provider<HttpProvider> httpProviderFactory = new HttpProviderFactory();
    private final int timeout;
    private String cookie;
    private boolean isLoggedIn = false;
    private String accountName;
    private String accountId;

    public GooglePlusAuthenticationModule(URL baseURL, AuthenticationConfig config, Context appContext) {
        this.baseURL = baseURL;
        this.loginEndpoint = config.getLoginEndpoint();
        this.logoutEndpoint = config.getLogoutEndpoint();
        this.appContext = appContext.getApplicationContext();
        timeout = config.getTimeout();
    }


    @Override
    public URL getBaseURL() {
        return baseURL;
    }

    @Override
    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    @Override
    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    private URL getLoginURL() {
        try {
            return new URL(baseURL + "/" + loginEndpoint);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getEnrollEndpoint() {
        return null;
    }

    @Override
    public void login(final Map<String, String> authMap, final Callback<HeaderAndBody> headerAndBodyCallback) {
        THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    if (AccountUtil.hasConnected(DevnexusApplication.CONTEXT)) {
                        cookie = AccountUtil.getCookie(DevnexusApplication.CONTEXT);
                        isLoggedIn = true;
                        accountName = AccountUtil.getUsername(DevnexusApplication.CONTEXT);
                        accountId = "";
                        headerAndBodyCallback.onSuccess(new HeaderAndBody(new byte[0], new HashMap<String, Object>()));
                    }

                    final String accessToken = GoogleAuthUtil.getToken(appContext,
                            authMap.get(ACCOUNT_NAME),
                            "audience:server:client_id:402595014005-cairesrhrd0p75jg62i8vdk4qteca2c4.apps.googleusercontent.com",
                            null);


                    HttpProvider provider = httpProviderFactory.get(getLoginURL(), timeout);
                    String loginRequest = new JSONObject(String.format("{\"gPlusId\":\"%s\",\"accessToken\":\"%s\"}", authMap.get(ACCOUNT_ID), accessToken)).toString();

                    HeaderAndBody result = provider.post(loginRequest);
                    cookie = result.getHeader("Set-Cookie").toString();
                    AccountUtil.setCookie(DevnexusApplication.CONTEXT, cookie);
                    isLoggedIn = true;
                    accountName = authMap.get(ACCOUNT_NAME);
                    accountId = authMap.get(ACCOUNT_ID);
                    headerAndBodyCallback.onSuccess(result);

                } catch (IOException authEx) {
                    Log.e(TAG, authEx.getMessage(), authEx);
                    headerAndBodyCallback.onFailure(authEx);
                    return;
                } catch (UserRecoverableAuthException e) {
                    Log.e(TAG, e.getMessage(), e);
                    headerAndBodyCallback.onFailure(e);
                    //    accessToken = null;
                } catch (GoogleAuthException authEx) {
                    Log.e(TAG, authEx.getMessage(), authEx);
                    headerAndBodyCallback.onFailure(authEx);
                    return;
                } catch (Exception e) {
                    headerAndBodyCallback.onFailure(e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    @Override
    public AuthorizationFields getAuthorizationFields() {
        AuthorizationFields fields = new AuthorizationFields();
        fields.addHeader("Cookie", cookie);
        return fields;
    }

    @Override
    public AuthorizationFields getAuthorizationFields(URI requestUri, String method, byte[] requestBody) {
        return getAuthorizationFields();
    }

    @Override
    public boolean retryLogin() throws HttpException {
        try {


            final String accessToken = GoogleAuthUtil.getToken(appContext,
                    accountName,
                    "audience:server:client_id:402595014005-cairesrhrd0p75jg62i8vdk4qteca2c4.apps.googleusercontent.com",
                    null);


            HttpProvider provider = httpProviderFactory.get(getLoginURL(), timeout);
            String loginRequest = new JSONObject(String.format("{\"gPlusId\":\"%s\",\"accessToken\":\"%s\"}", accountId, accessToken)).toString();

            HeaderAndBody result = provider.post(loginRequest);
            cookie = result.getHeader("Set-Cookie").toString();
            AccountUtil.setCookie(DevnexusApplication.CONTEXT, cookie);
            isLoggedIn = true;

        } catch (Exception e) {
            AccountUtil.setCookie(DevnexusApplication.CONTEXT, "");
            AccountUtil.setUsername(DevnexusApplication.CONTEXT, "");
            AccountUtil.setConnected(DevnexusApplication.CONTEXT, false);
            isLoggedIn = false;
        }
        return isLoggedIn;
    }


}