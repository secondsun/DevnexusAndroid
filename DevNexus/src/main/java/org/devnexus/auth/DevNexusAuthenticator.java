package org.devnexus.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.devnexus.DevnexusApplication;
import org.devnexus.GoogleConnectActivity;
import org.devnexus.util.AccountUtil;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.http.HeaderAndBody;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by summers on 2/2/14.
 */

public class DevNexusAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = DevNexusAuthenticator.class.getSimpleName();
    private DevnexusApplication app;

    public static final String ACCOUNT_TYPE = "org.devnexus.Account";

    public DevNexusAuthenticator(Context context) {
        super(context);
        app = DevnexusApplication.CONTEXT;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Bundle toReturn = new Bundle();
        toReturn.putParcelable(AccountManager.KEY_INTENT, new Intent(app, GoogleConnectActivity.class).putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response));
        toReturn.putParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        return toReturn;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, final Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        final Bundle toReturn = new Bundle();

        AuthenticationModule module = app.getAuth();
        HashMap<String, String> loginParams = new HashMap<String, String>();
        loginParams.put(GooglePlusAuthenticationModule.ACCOUNT_NAME, AccountUtil.getUsername(app));
        loginParams.put(GooglePlusAuthenticationModule.ACCOUNT_ID, "");

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> exceptionRef = new AtomicReference<Exception>();
        module.login(loginParams, new Callback<HeaderAndBody>() {
            @Override
            public void onSuccess(HeaderAndBody data) {
                toReturn.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                toReturn.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                toReturn.putString(AccountManager.KEY_AUTHTOKEN, AccountUtil.getCookie(app));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
                toReturn.putString(AccountManager.KEY_ERROR_CODE, "0");
                toReturn.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            toReturn.putString(AccountManager.KEY_ERROR_CODE, "0");
            toReturn.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
        }

        return toReturn;

    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return "Devnexus Calendar";
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
}
