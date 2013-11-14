package org.devnexus;

import android.app.Application;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by summers on 11/13/13.
 */
public class DevnexusApplication extends Application {

    private static final URL DEVNEXUS_URL;

    private static final String TAG = DevnexusApplication.class.getSimpleName();

    static {
        try {
            DEVNEXUS_URL = new URL("http://devnexus.com/s");
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
