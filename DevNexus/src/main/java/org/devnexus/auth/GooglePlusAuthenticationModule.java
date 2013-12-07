package org.devnexus.auth;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
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

    public GooglePlusAuthenticationModule(URL baseURL, AuthenticationConfig config, Context appContext) {
        this.baseURL = baseURL;
        this.loginEndpoint = config.getLoginEndpoint();
        this.logoutEndpoint = config.getLogoutEndpoint();
        this.appContext = appContext.getApplicationContext();
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
                    Bundle appActivities = new Bundle();
                    appActivities.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES,
                            "http://schemas.google.com/AddActivity http://schemas.google.com/BuyActivity");


                    final String accessToken = GoogleAuthUtil.getToken(appContext,
                            authMap.get(ACCOUNT_NAME),
                            "oauth2:server:client_id:402595014005-cairesrhrd0p75jg62i8vdk4qteca2c4.apps.googleusercontent.com:api_scope:" + TextUtils.join(" ", SCOPES),
                            appActivities);

                    Log.d(TAG, String.format("curl -v -d '{\"gPlusId\":\"%s\",\"accessToken\":\"%s\"}' -X POST --header \"Content-Type:text/json\" http://localhost:8080/s/loginAndroid.json | json_reformat", authMap.get(ACCOUNT_ID), accessToken));



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
        return false;
    }

    @Override
    public AuthorizationFields getAuthorizationFields() {
        throw new IllegalStateException("deprecated");
    }

    @Override
    public AuthorizationFields getAuthorizationFields(URI uri, String s, byte[] bytes) {
        return null;
    }

    @Override
    public boolean retryLogin() throws HttpException {
        return true;
    }


}
