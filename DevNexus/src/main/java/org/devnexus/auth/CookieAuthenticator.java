package org.devnexus.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;

import org.devnexus.DevnexusApplication;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AbstractAuthenticationModule;
import org.jboss.aerogear.android.authentication.AuthorizationFields;
import org.jboss.aerogear.android.http.HeaderAndBody;
import org.jboss.aerogear.android.http.HttpException;

import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * Created by summers on 2/4/14.
 */
public class CookieAuthenticator extends AbstractAuthenticationModule {

    public String cookie = null;
    public boolean loggedIn = false;

    @Override
    public URL getBaseURL() {
        return null;
    }

    @Override
    public String getLoginEndpoint() {
        return null;
    }

    @Override
    public String getLogoutEndpoint() {
        return null;
    }

    @Override
    public String getEnrollEndpoint() {
        return null;
    }

    @Override
    public void enroll(Map<String, String> userData, Callback<HeaderAndBody> callback) {

    }

    @Override
    public void login(String username, String password, Callback<HeaderAndBody> callback) {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public void login(Map<String, String> loginData, final Callback<HeaderAndBody> callback) {
        THREAD_POOL_EXECUTOR.execute(new Runnable() {


            @Override
            public void run() {
                AccountManager am = AccountManager.get(DevnexusApplication.CONTEXT);
                Account[] accounts = am.getAccountsByType(DevNexusAuthenticator.ACCOUNT_TYPE);
                if (accounts.length == 0) {
                    AccountManagerFuture<Bundle> future = am.addAccount(DevNexusAuthenticator.ACCOUNT_TYPE, null, null, null, null, null, null);
                    try {
                        Bundle bundle = future.getResult();
                        accounts = am.getAccountsByType(DevNexusAuthenticator.ACCOUNT_TYPE);
                    } catch (Exception e) {
                        callback.onFailure(e);
                        return;
                    }

                }
                Account account = accounts[0];
                AccountManagerFuture<Bundle> tokenFuture = am.getAuthToken(account, "", null, null, null, null);
                Bundle tokenBundle;

                try {
                    tokenBundle = tokenFuture.getResult();
                } catch (Exception e) {
                    callback.onFailure(e);
                    return;
                }

                cookie = tokenBundle.getString(AccountManager.KEY_AUTHTOKEN);

                if (cookie != null) {
                    loggedIn = true;
                } else {
                    loggedIn = false;
                }
                callback.onSuccess(null);

            }
        });
    }

    @Override
    public void logout(Callback<Void> callback) {

    }

    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public AuthorizationFields getAuthorizationFields() {
        return getAuthorizationFields(null, null, null);
    }

    @Override
    public AuthorizationFields getAuthorizationFields(URI requestUri, String method, byte[] requestBody) {
        AuthorizationFields fields = new AuthorizationFields();
        fields.addHeader("Cookie", cookie);

        return fields;
    }

    @Override
    public boolean retryLogin() throws HttpException {
        AccountManager am = AccountManager.get(DevnexusApplication.CONTEXT);
        Account[] accounts = am.getAccountsByType(DevNexusAuthenticator.ACCOUNT_TYPE);
        if (accounts.length == 0) {
            AccountManagerFuture<Bundle> future = am.addAccount(DevNexusAuthenticator.ACCOUNT_TYPE, null, null, null, null, null, null);
            try {
                Bundle bundle = future.getResult();
                accounts = am.getAccountsByType(DevNexusAuthenticator.ACCOUNT_TYPE);
            } catch (Exception e) {
                return false;
            }

        }
        Account account = accounts[0];
        am.invalidateAuthToken(account.type, cookie);
        AccountManagerFuture<Bundle> tokenFuture = am.getAuthToken(account, "DevNexus", null, null, null, null);
        Bundle tokenBundle;

        try {
            tokenBundle = tokenFuture.getResult();
        } catch (Exception e) {
            return false;
        }

        cookie = tokenBundle.getString(AccountManager.KEY_AUTHTOKEN);

        if (cookie != null) {
            return true;
        } else {
            return false;
        }

    }
}
