/**
 * JBoss, Home of Professional Open Source Copyright Red Hat, Inc., and
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.devnexus.aerogear;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.jboss.aerogear.android.Provider;

/**
 * This broadcast receiver will call Synchronizers and force them to update.
 */
public class SyncTimerReceiver extends BroadcastReceiver {

    private static final String TAG = SyncTimerReceiver.class.getSimpleName();
    public static final String SYNC_PROVIDER_KEY = "SYNC_PROVIDER_KEY";
    public static final String SYNC_MESSAGE_KEY = "SYNC_MESSAGE_KEY";
    public String syncMessageKey;
    private Provider<Synchronizer> synchronizerProvider;

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

        Synchronizer synchronizer = synchronizerProvider.get();
        synchronizer.loadRemoteChanges();

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
