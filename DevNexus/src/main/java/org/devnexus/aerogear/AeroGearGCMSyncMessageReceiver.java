/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.devnexus.aerogear;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.devnexus.DevnexusApplication;
import org.devnexus.auth.DevNexusAuthenticator;

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

                Bundle settingsBundle = new Bundle();

                settingsBundle.putBoolean(
                        ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                Log.e("SYNC_GCM", "A sync from GCM was requested");
                ContentResolver.requestSync(getAccount(), "org.devnexus.sync", settingsBundle);
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

    private Account getAccount() {
        final AccountManager am = AccountManager.get(DevnexusApplication.CONTEXT);
        Account[] accounts = am.getAccountsByType(DevNexusAuthenticator.ACCOUNT_TYPE);
        if (accounts.length == 0) {
            AccountManagerFuture<Bundle> future = am.addAccount(DevNexusAuthenticator.ACCOUNT_TYPE, null, null, null, null, null, null);
            try {
                Bundle bundle = future.getResult();
                accounts = am.getAccountsByType(DevNexusAuthenticator.ACCOUNT_TYPE);
            } catch (Exception e) {
                Log.e("Sync Received", e.getMessage(), e);
                throw new RuntimeException(e);
            }

        }
        return accounts[0];
    }

}
