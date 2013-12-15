package org.devnexus.sync;

import android.content.Context;

import org.jboss.aerogear.android.Callback;

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
     */
    public void syncNoMore();


    /**
     * Pulls down the remote data and synchronizes
     */
    public void loadRemoteChanges();

    /**
     * Notify the synchronizer that local changes have been made and should be sent to the remote server for synchronization.
     */
    public void sync();

}
