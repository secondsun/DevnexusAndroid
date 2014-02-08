package org.devnexus.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.devnexus.auth.DevNexusAuthenticator;

/**
 * Created by summers on 2/2/14.
 */
public class DevnexusAuthenticatorService extends Service {

    // Instance field that stores the authenticator object
    private DevNexusAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new DevNexusAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }


}
