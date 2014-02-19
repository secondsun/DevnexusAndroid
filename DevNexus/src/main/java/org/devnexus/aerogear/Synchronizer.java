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

import android.content.ContentProviderClient;
import android.content.Context;

import org.jboss.aerogear.android.Callback;

import java.util.List;

/**
 * Created by summers on 12/9/13.
 */
public interface Synchronizer<T> {

    /**
     * A listener listens for data on this synchronizer.
     *
     * @param listener
     */
    public void addListener(SynchronizeEventListener<T> listener);

    /**
     * A listener listens for data on this synchronizer.
     *
     * @param listener
     */
    public void removeListener(SynchronizeEventListener<T> listener);


    /**
     * Does what ever is necessary to start syncing
     */
    public void beginSync(Context appContext, Callback<Void> syncReadyCallback);

    /**
     * Sync no more
     *
     * @param appContext            The Application Context for the Android application
     * @param syncCancelledCallback This callback will be called when the sync has been cancelled.  The callback MAY be provided with data if a sync was in progress.
     */
    public void syncNoMore(Context appContext, Callback<T> syncCancelledCallback);


    /**
     * Pulls down the remote data and synchronizes
     *
     * @param provider
     */
    public void loadRemoteChanges(ContentProviderClient provider);

    /**
     * Notify the synchronizer that local changes have been made and should be sent to the remote server for synchronization.
     *
     * @param provider
     */
    public void sync(ContentProviderClient provider);

    /**
     * This method will load the remote data set and reset the local data to it.
     * This is useful for the initial data fetch.
     *
     * @param provider
     * @param callback callback with the remote data
     */
    public void resetToRemoteState(ContentProviderClient provider, final Callback<List<T>> callback);

}
