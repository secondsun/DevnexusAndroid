package org.devnexus.sync;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.jboss.aerogear.android.Callback;

import java.util.ArrayList;
import java.util.List;

public class PeriodicDataSynchronizer<T> extends BroadcastReceiver implements Synchronizer<T> {

    private List<SynchronizeEventListener<T>> listeners = new ArrayList<SynchronizeEventListener<T>>();
    private final PeriodicSynchronizerConfig config;

    public PeriodicDataSynchronizer(PeriodicSynchronizerConfig config) {
        this.config = config;
    }


    @Override
    public void addListener(SynchronizeEventListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SynchronizeEventListener<T> listener) {
        listeners.remove(listener);
    }

    @Override
    public void beginSync(Context appContext, Callback syncReady) {
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = builidIntent(appContext);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, config.getPeriod() * 1000, config.getPeriod() * 1000, alarmIntent);
    }

    private Intent builidIntent(Context appContext) {
        Intent intent = new Intent(appContext, PeriodicDataSynchronizer.class);

        return intent;
    }

    @Override
    public void syncNoMore() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }

    @Override
    public void loadRemoteChanges() {

    }

    @Override
    public void sync() {

    }
}
