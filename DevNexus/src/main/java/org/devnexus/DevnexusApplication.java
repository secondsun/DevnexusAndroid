package org.devnexus;

import android.accounts.Account;
import android.app.Application;
import android.content.ContentResolver;
import android.os.Bundle;
import android.util.Log;

import org.devnexus.auth.DevNexusAuthenticator;
import org.devnexus.auth.GooglePlusAuthenticationModule;
import org.devnexus.util.AccountUtil;
import org.devnexus.vo.Schedule;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.authentication.impl.Authenticator;
import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.Registrations;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by summers on 11/13/13.
 */
public class DevnexusApplication extends Application {


    private static final String TAG = DevnexusApplication.class.getSimpleName();
    private static final URL DEVNEXUS_URL;

    private static final URI PUSH_URL;
    public static final ExecutorService EXECUTORS = Executors.newCachedThreadPool();
    public static DevnexusApplication CONTEXT = null;
    private List<Schedule> currentSchedule = new ArrayList<Schedule>();
    //private List<UserCalendar> currentCalendar = new ArrayList<UserCalendar>();
    private Authenticator authenticator;

    private final Registrations registrations = new Registrations();
    private PushConfig pushConfig;

    static {
        try {
            DEVNEXUS_URL = new URL("http://192.168.1.194:9090/s/");
            PUSH_URL = new URI("http://192.168.1.194:8080/ag-push");
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    {

        authenticator = new Authenticator(DEVNEXUS_URL);


        // Create a PushConfig for the UnifiedPush Server:
        pushConfig = new PushConfig(PUSH_URL, "402595014005");
        pushConfig.setVariantID("a26c1609-873e-427e-9106-7a6d435cbc78");
        pushConfig.setSecret("5264fdc8-f0e6-480a-8725-f24caa9440e5");

    }

    private ContentResolver resolver;

    @Override
    public void onCreate() {
        super.onCreate();

        CONTEXT = this;
        resolver = getContentResolver();
        AuthenticationConfig config = new AuthenticationConfig();
        config.setLoginEndpoint("loginAndroid.json");
        GooglePlusAuthenticationModule module = new GooglePlusAuthenticationModule(DEVNEXUS_URL, config, getApplicationContext());
        authenticator.add("google", module);
    }

    public AuthenticationModule getAuth() {
        return authenticator.get("google");
    }


    public void registerForPush(String accountName) {
        pushConfig.setAlias(accountName);
        PushRegistrar pushRegistration = registrations.push("gcm", pushConfig);
        pushRegistration.register(this, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "Push registration successful.");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    public void startUpSync() {

        ContentResolver.setSyncAutomatically(new Account(AccountUtil.getUsername(DevnexusApplication.CONTEXT), DevNexusAuthenticator.ACCOUNT_TYPE), "org.devnexus.sync", true);
        ContentResolver.addPeriodicSync(new Account(AccountUtil.getUsername(DevnexusApplication.CONTEXT), DevNexusAuthenticator.ACCOUNT_TYPE),
                "org.devnexus.sync", new Bundle(), 3600);

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);


        ContentResolver.requestSync(new Account(AccountUtil.getUsername(DevnexusApplication.CONTEXT), DevNexusAuthenticator.ACCOUNT_TYPE),
                "org.devnexus.sync", settingsBundle);

        registerForPush(AccountUtil.getUsername(getApplicationContext()));

    }


}
