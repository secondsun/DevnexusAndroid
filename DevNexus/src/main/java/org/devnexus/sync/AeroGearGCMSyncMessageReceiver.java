package org.devnexus.sync;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.jboss.aerogear.android.Provider;

import static org.jboss.aerogear.android.unifiedpush.PushConstants.DELETED;
import static org.jboss.aerogear.android.unifiedpush.PushConstants.ERROR;

/**
 * Created by summers on 12/14/13.
 */
public class AeroGearGCMSyncMessageReceiver extends BroadcastReceiver {

    private static final String TAG = AeroGearGCMSyncMessageReceiver.class.getSimpleName();
    public static final String SYNC_PROVIDER_KEY = "SYNC_PROVIDER_KEY";
    public static final String SYNC_MESSAGE_KEY = "SYNC_MESSAGE_KEY";
    public String syncMessageKey;
    private Provider<Synchronizer> synchronizerProvider;

    /**
     * When a GCM message is received, the attached implementations of our <code>MessageHandler</code> interface
     * are being notified.
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle metaData = getMetadata(context);
        if (metaData != null) {

            String defaultHandlerClassName = metaData.getString(SYNC_PROVIDER_KEY);
            if (defaultHandlerClassName != null) {
                try {
                    synchronizerProvider = (Provider<Synchronizer>) Class.forName(defaultHandlerClassName).newInstance();
                    syncMessageKey = metaData.getString(SYNC_MESSAGE_KEY);

                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                }

            } else {
                throw new IllegalStateException("Sync config provider key missing!");
            }
        }

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            intent.putExtra(ERROR, true);
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            intent.putExtra(DELETED, true);
        } else {
            if (intent.hasExtra(syncMessageKey)) {
                Synchronizer synchronizer = synchronizerProvider.get();
                synchronizer.loadRemoteChanges();
            }

        }

    }

    private Bundle getMetadata(Context context) {
        final ComponentName componentName = new ComponentName(context, AeroGearGCMSyncMessageReceiver.class);
        try {
            ActivityInfo ai = context.getPackageManager().getReceiverInfo(componentName, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
            Bundle metaData = ai.metaData;
            if (metaData == null) {
                Log.d(TAG, "metaData is null. Unable to get meta data for " + componentName);
            } else {
                return metaData;
            }
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
        return null;

    }
}
