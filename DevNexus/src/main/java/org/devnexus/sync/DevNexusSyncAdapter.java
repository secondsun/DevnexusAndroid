package org.devnexus.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.apache.http.HttpStatus;
import org.devnexus.aerogear.RestRunner;
import org.devnexus.aerogear.SynchronizeEventListener;
import org.devnexus.aerogear.TwoWaySqlSynchronizer;
import org.devnexus.auth.CookieAuthenticator;
import org.devnexus.util.CountDownCallback;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.UserCalendar;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.http.HttpException;
import org.jboss.aerogear.android.impl.datamanager.SQLStore;
import org.jboss.aerogear.android.impl.datamanager.StoreConfig;
import org.jboss.aerogear.android.impl.datamanager.StoreTypes;
import org.jboss.aerogear.android.impl.pipeline.GsonResponseParser;
import org.jboss.aerogear.android.impl.pipeline.PipeConfig;
import org.jboss.aerogear.android.impl.util.UrlUtils;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
    public static final String EXCEPTION_DATA = "org.devnexus.sync.calendar_data";


    private static final URL DEVNEXUS_URL;
    private static final URI PUSH_URL;
    private GsonBuilder builder = new GsonBuilder();

    private static final String TAG = DevNexusSyncAdapter.class.getSimpleName();
    private final TwoWaySqlSynchronizer<UserCalendar> calendarSynchronizer;
    private final TwoWaySqlSynchronizer<Schedule> scheduleSynchronizer;

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

    private AuthenticationModule cookieAuth = new CookieAuthenticator();


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

    {


        builder.registerTypeAdapter(Date.class, new JsonDeserializer() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        builder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                    context) {
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        });


        PipeConfig schedulePipeConfig = new PipeConfig(DEVNEXUS_URL, Schedule.class);
        schedulePipeConfig.setEndpoint("schedule.json");
        schedulePipeConfig.setName("schedule");
        schedulePipeConfig.setResponseParser(new GsonResponseParser(builder.create()));
        schedulePipeConfig.setHandler(new RestRunner(Schedule.class, UrlUtils.appendToBaseURL(schedulePipeConfig.getBaseURL(), schedulePipeConfig.getEndpoint()), schedulePipeConfig));

        PipeConfig userCalendarPipeConfig = new PipeConfig(DEVNEXUS_URL, UserCalendar.class);
        userCalendarPipeConfig.setName("calendar");
        userCalendarPipeConfig.setAuthModule(cookieAuth);
        userCalendarPipeConfig.setResponseParser(new GsonResponseParser(builder.create()));
        userCalendarPipeConfig.setHandler(new RestRunner(UserCalendar.class, UrlUtils.appendToBaseURL(userCalendarPipeConfig.getBaseURL(), userCalendarPipeConfig.getEndpoint()), userCalendarPipeConfig));


        StoreConfig scheduleItemStoreConfig = new StoreConfig();
        scheduleItemStoreConfig.setType(StoreTypes.SQL);
        scheduleItemStoreConfig.setKlass(Schedule.class);
        scheduleItemStoreConfig.setBuilder(builder);
        scheduleItemStoreConfig.setContext(getContext());

        StoreConfig userCalendarStoreConfig = new StoreConfig();
        userCalendarStoreConfig.setType(StoreTypes.SQL);
        userCalendarStoreConfig.setKlass(UserCalendar.class);
        userCalendarStoreConfig.setContext(getContext());
        userCalendarStoreConfig.setBuilder(builder);


        TwoWaySqlSynchronizer.TwoWaySqlSynchronizerConfig syncConfig = new TwoWaySqlSynchronizer.TwoWaySqlSynchronizerConfig(UserCalendar.class);
        syncConfig.setPipeConfig(userCalendarPipeConfig);
        syncConfig.setStoreConfig(userCalendarStoreConfig);

        calendarSynchronizer = new TwoWaySqlSynchronizer<UserCalendar>(syncConfig);

        TwoWaySqlSynchronizer.TwoWaySqlSynchronizerConfig scheduleConfig = new TwoWaySqlSynchronizer.TwoWaySqlSynchronizerConfig(Schedule.class);

        scheduleConfig.setPipeConfig(schedulePipeConfig);
        scheduleConfig.setStoreConfig(scheduleItemStoreConfig);

        scheduleSynchronizer = new TwoWaySqlSynchronizer<Schedule>(scheduleConfig);

        scheduleSynchronizer.addListener(this);
        calendarSynchronizer.addListener(this);

        CountDownLatch latch = new CountDownLatch(2);
        calendarSynchronizer.beginSync(getContext(), new CountDownCallback<Void>(latch));
        scheduleSynchronizer.beginSync(getContext(), new CountDownCallback<Void>(latch));
    }

    @Override
    public void onPerformSync(Account account, final Bundle extras, String authority, ContentProviderClient provider, final SyncResult syncResult) {

        final Gson gson = builder.create();

        if (extras.getSerializable(CALENDAR_DATA) != null) {
            UserCalendar calendarItem = gson.fromJson(extras.getString(CALENDAR_DATA), UserCalendar.class);
            SQLStore<UserCalendar> calendarStore = calendarSynchronizer.getLocalStore();
            calendarStore.remove(calendarItem.getId());
            calendarStore.save(calendarItem);
        }


        if (syncResult.fullSyncRequested) {
            calendarSynchronizer.resetToRemoteState(new Callback<List<UserCalendar>>() {
                @Override
                public void onSuccess(List<UserCalendar> data) {
                    syncResult.stats.numUpdates = data.size();
                    sendCalendarSyncFinished(data, null);
                }

                @Override
                public void onFailure(Exception e) {
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
            calendarSynchronizer.sync();
            calendarSynchronizer.loadRemoteChanges();
            syncResult.stats.numUpdates = 1;
            Collection<UserCalendar> data = calendarSynchronizer.getLocalStore().readAll();
            sendCalendarSyncFinished(data, null);
        }
        scheduleSynchronizer.resetToRemoteState(new Callback<List<Schedule>>() {
            @Override
            public void onSuccess(List<Schedule> data) {
                Collection<Schedule> allData = scheduleSynchronizer.getLocalStore().readAll();
                syncResult.stats.numUpdates = allData.size();
                sendScheduleSyncFinished(allData, null);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof HttpException) {
                    HttpException httpException = (HttpException) e;
                    if (httpException.getStatusCode() == HttpStatus.SC_UNAUTHORIZED || httpException.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                        syncResult.stats.numAuthExceptions++;
                    } else {
                        syncResult.stats.numIoExceptions++;
                    }
                    sendScheduleSyncFinished(new ArrayList<Schedule>(1), e);
                }
            }
        });
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
