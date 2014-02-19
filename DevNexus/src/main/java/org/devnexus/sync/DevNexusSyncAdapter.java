package org.devnexus.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.HttpStatus;
import org.devnexus.aerogear.RestRunner;
import org.devnexus.aerogear.ScheduleSynchronizer;
import org.devnexus.aerogear.SynchronizeEventListener;
import org.devnexus.aerogear.UserCalendarSynchronizer;
import org.devnexus.auth.CookieAuthenticator;
import org.devnexus.util.GsonUtils;
import org.devnexus.util.StoreConfigProvider;
import org.devnexus.util.VoidCallback;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.contract.SingleColumnJsonArrayList;
import org.devnexus.vo.contract.UserCalendarContract;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.impl.datamanager.StoreConfig;
import org.jboss.aerogear.android.impl.pipeline.GsonResponseParser;
import org.jboss.aerogear.android.impl.pipeline.PipeConfig;
import org.jboss.aerogear.android.impl.util.UrlUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by summers on 2/2/14.
 */
public class DevNexusSyncAdapter extends AbstractThreadedSyncAdapter implements SynchronizeEventListener {

    // Global variables
    // Define a variable to contain a content resolver instance
    private ContentResolver mContentResolver;

    public static final String CALENDAR_SYNC_FINISH = "org.devnexus.sync.calendar_finished";
    public static final String SCHEDULE_SYNC_FINISH = "org.devnexus.sync.schedule_finished";
    public static final String SCHEDULE_DATA = "org.devnexus.sync.schedule_data";
    public static final String CALENDAR_DATA = "org.devnexus.sync.calendar_data";
    public static final String EXCEPTION_DATA = "org.devnexus.sync.exception_data";

    private static final URL DEVNEXUS_URL;


    private static final Gson GSON = GsonUtils.GSON;
    private static final String TAG = DevNexusSyncAdapter.class.getSimpleName();
    private UserCalendarSynchronizer calendarSynchronizer;
    private ScheduleSynchronizer scheduleSynchronizer;


    static {
        try {
            DEVNEXUS_URL = new URL("http://192.168.1.194:9090/s/");
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }


    /**
     * Set up the sync adapter
     */
    public DevNexusSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public DevNexusSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();

    }


    @Override
    public synchronized void onPerformSync(Account account, final Bundle extras, String authority, ContentProviderClient provider, final SyncResult syncResult) {

        final CountDownLatch scheduleLatch = new CountDownLatch(1);
        final CountDownLatch calendarLatch = new CountDownLatch(1);

        StoreConfig userCalendarStoreConfig = StoreConfigProvider.getUserStoreConfig(getContext());


        PipeConfig schedulePipeConfig = new PipeConfig(DEVNEXUS_URL, Schedule.class);
        schedulePipeConfig.setEndpoint("schedule.json");
        schedulePipeConfig.setName("schedule");
        schedulePipeConfig.setResponseParser(new GsonResponseParser(GSON));
        schedulePipeConfig.setHandler(new RestRunner(Schedule.class, UrlUtils.appendToBaseURL(schedulePipeConfig.getBaseURL(), schedulePipeConfig.getEndpoint()), schedulePipeConfig));

        PipeConfig userCalendarPipeConfig = new PipeConfig(DEVNEXUS_URL, UserCalendar.class);
        userCalendarPipeConfig.setName("calendar");
        CookieAuthenticator cookieAuth = new CookieAuthenticator();
        userCalendarPipeConfig.setAuthModule(cookieAuth);
        userCalendarPipeConfig.setResponseParser(new GsonResponseParser(GSON));
        userCalendarPipeConfig.setHandler(new RestRunner(UserCalendar.class, UrlUtils.appendToBaseURL(userCalendarPipeConfig.getBaseURL(), userCalendarPipeConfig.getEndpoint()), userCalendarPipeConfig));


        UserCalendarSynchronizer.TwoWaySqlSynchronizerConfig syncConfig = new UserCalendarSynchronizer.TwoWaySqlSynchronizerConfig(UserCalendar.class);
        syncConfig.setPipeConfig(userCalendarPipeConfig);
        syncConfig.setStoreConfig(userCalendarStoreConfig);

        calendarSynchronizer = new UserCalendarSynchronizer(syncConfig);


        ScheduleSynchronizer.TwoWaySqlSynchronizerConfig scheduleConfig = new ScheduleSynchronizer.TwoWaySqlSynchronizerConfig(Schedule.class);
        StoreConfig scheduleItemStoreConfig = StoreConfigProvider.getScheduleStoreConfig(getContext());

        scheduleConfig.setPipeConfig(schedulePipeConfig);
        scheduleConfig.setStoreConfig(scheduleItemStoreConfig);
        scheduleSynchronizer = new ScheduleSynchronizer(scheduleConfig);

        scheduleSynchronizer.beginSync(getContext(), VoidCallback.INSTANCE);
        scheduleSynchronizer.resetToRemoteState(provider, new Callback<List<Schedule>>() {
            @Override
            public void onSuccess(List<Schedule> data) {
                scheduleSynchronizer.syncNoMore(getContext(), null);
                syncResult.stats.numUpdates = data.size();
                scheduleLatch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                scheduleSynchronizer.syncNoMore(getContext(), null);
                scheduleLatch.countDown();
                if (e instanceof HttpException) {
                    HttpException httpException = (HttpException) e;
                    if (httpException.getStatusCode() == HttpStatus.SC_UNAUTHORIZED || httpException.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                        syncResult.stats.numAuthExceptions++;
                    } else {
                        syncResult.stats.numIoExceptions++;
                    }
                }

                sendScheduleSyncFinished(new ArrayList<Schedule>(1), e);
            }
        });

        calendarSynchronizer.beginSync(getContext(), VoidCallback.INSTANCE);
        if (syncResult.fullSyncRequested) {

            calendarSynchronizer.resetToRemoteState(provider, new Callback<List<UserCalendar>>() {
                @Override
                public void onSuccess(List<UserCalendar> data) {
                    calendarSynchronizer.syncNoMore(getContext(), null);
                    syncResult.stats.numUpdates = data.size();
                    sendCalendarSyncFinished(data, null);
                    calendarLatch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    calendarSynchronizer.syncNoMore(getContext(), null);
                    calendarLatch.countDown();
                    if (e instanceof HttpException) {
                        HttpException httpException = (HttpException) e;
                        if (httpException.getStatusCode() == HttpStatus.SC_UNAUTHORIZED || httpException.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                            syncResult.stats.numAuthExceptions++;
                        } else {
                            syncResult.stats.numIoExceptions++;
                        }
                    }

                    sendCalendarSyncFinished(new ArrayList<UserCalendar>(1), e);
                }
            });
        } else {
            try {
                try {
                    calendarSynchronizer.sync(provider);
                    calendarSynchronizer.loadRemoteChanges(provider);
                } catch (RuntimeException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                    syncResult.stats.numIoExceptions++;
                    sendCalendarSyncFinished(new ArrayList<UserCalendar>(1), ex);
                    return;
                }

                syncResult.stats.numUpdates = 1;
                SingleColumnJsonArrayList cursor = null;
                try {
                    cursor = (SingleColumnJsonArrayList) provider.query(UserCalendarContract.URI, null, null, null, null);
                    ArrayList<UserCalendar> calendars = new ArrayList<UserCalendar>();
                    while (cursor != null && cursor.moveToNext()) {
                        calendars.add(GSON.fromJson(cursor.getString(0), UserCalendar.class));
                    }
                    Collection<UserCalendar> data = calendars;
                    sendCalendarSyncFinished(data, null);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                    sendCalendarSyncFinished(new ArrayList<UserCalendar>(1), e);
                    return;
                } finally {
                    if (cursor != null)
                        cursor.close();
                }

            } finally {
                calendarSynchronizer.syncNoMore(getContext(), null);
                calendarLatch.countDown();
            }

        }


        try {
            scheduleLatch.await(60, TimeUnit.SECONDS);
            calendarLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void sendScheduleSyncFinished(Collection<Schedule> schedules, Exception exception) {
        Intent broadcast = new Intent().setAction(SCHEDULE_SYNC_FINISH);

        if (schedules == null) {
            broadcast.putExtra(EXCEPTION_DATA, exception);
        } else {
            broadcast.putExtra(SCHEDULE_DATA, new ArrayList<Schedule>(schedules));
        }
        getContext().sendBroadcast(broadcast);
    }

    private void sendCalendarSyncFinished(Collection<UserCalendar> calendar, Exception exception) {
        Intent broadcast = new Intent().setAction(CALENDAR_SYNC_FINISH);

        if (calendar == null) {
            broadcast.putExtra(EXCEPTION_DATA, exception);
        } else {
            broadcast.putExtra(CALENDAR_DATA, new ArrayList<UserCalendar>(calendar));
        }
        getContext().sendBroadcast(broadcast);
    }

    @Override
    public void dataUpdated(Collection newData) {
        Object first = newData.iterator().next();
        if (first instanceof UserCalendar) {
            sendCalendarSyncFinished(newData, null);
        } else {
            sendScheduleSyncFinished(newData, null);
        }
    }

    @Override
    public Schedule resolveConflicts(Object clientData, Object serverData) {
        return null;
    }

}
