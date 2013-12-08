package org.devnexus;

import android.app.Application;
import android.util.Log;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.devnexus.adapters.ScheduleAdapter;
import org.devnexus.auth.GooglePlusAuthenticationModule;
import org.devnexus.fragments.ScheduleFragment;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.SyncStats;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.UserCalendarList;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.DataManager;
import org.jboss.aerogear.android.Pipeline;
import org.jboss.aerogear.android.authentication.AuthenticationConfig;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.authentication.impl.Authenticator;
import org.jboss.aerogear.android.impl.datamanager.SQLStore;
import org.jboss.aerogear.android.impl.datamanager.StoreConfig;
import org.jboss.aerogear.android.impl.datamanager.StoreTypes;
import org.jboss.aerogear.android.impl.pipeline.GsonResponseParser;
import org.jboss.aerogear.android.impl.pipeline.PipeConfig;
import org.jboss.aerogear.android.pipeline.Pipe;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by summers on 11/13/13.
 */
public class DevnexusApplication extends Application {

    private static final URL DEVNEXUS_URL;

    private static final String TAG = DevnexusApplication.class.getSimpleName();
    private SQLStore<Schedule> scheduleStore;
    private SQLStore<UserCalendarList> userCalendarStore;
    private SQLStore<SyncStats> statsStore;
    private Authenticator authenticator = new Authenticator("http://192.168.1.194:8080");


    private GsonBuilder builder = new GsonBuilder();


    private Pipe<Schedule> schedulePipe;
    private Pipe<UserCalendarList> userCalendarPipe;

    private final DataManager dm = new DataManager();
    private final Pipeline pipeline;

    static {
        try {
            DEVNEXUS_URL = new URL("http://192.168.1.194:8080/s/devnexus2013/");
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    {
        pipeline = new Pipeline(DEVNEXUS_URL);

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
    }

    @Override
    public void onCreate() {
        super.onCreate();


        AuthenticationConfig config = new AuthenticationConfig();

        config.setLoginEndpoint("s/loginAndroid.json");

        GooglePlusAuthenticationModule module = null;
        try {
            module = new GooglePlusAuthenticationModule(new URL("http://192.168.1.194:8080/"), config, getApplicationContext());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        authenticator.add("google", module);


        PipeConfig schedulePipeConfig = new PipeConfig(DEVNEXUS_URL, Schedule.class);
        schedulePipeConfig.setEndpoint("schedule.json");
        schedulePipeConfig.setName("schedule");
        schedulePipeConfig.setResponseParser(new GsonResponseParser(builder.create()));
        schedulePipe = pipeline.pipe(Schedule.class, schedulePipeConfig);

        PipeConfig userCalendarPipeConfig = new PipeConfig(DEVNEXUS_URL, UserCalendar.class);
        userCalendarPipeConfig.setEndpoint("calendar.json");
        userCalendarPipeConfig.setName("calendar");
        userCalendarPipeConfig.setAuthModule(module);
        userCalendarPipeConfig.setResponseParser(new GsonResponseParser(builder.create()));
        userCalendarPipe = pipeline.pipe(UserCalendarList.class, userCalendarPipeConfig);

        StoreConfig scheduleItemStoreConfig = new StoreConfig();
        scheduleItemStoreConfig.setType(StoreTypes.SQL);
        scheduleItemStoreConfig.setKlass(Schedule.class);
        scheduleItemStoreConfig.setBuilder(builder);
        scheduleItemStoreConfig.setContext(getApplicationContext());
        scheduleStore = (SQLStore<Schedule>) dm.store("scheduleItem", scheduleItemStoreConfig);

        StoreConfig userCalendarStoreConfig = new StoreConfig();
        userCalendarStoreConfig.setType(StoreTypes.SQL);
        userCalendarStoreConfig.setKlass(UserCalendarList.class);
        userCalendarStoreConfig.setContext(getApplicationContext());
        userCalendarStoreConfig.setBuilder(builder);
        userCalendarStore = (SQLStore<UserCalendarList>) dm.store("userCalendar", userCalendarStoreConfig);

        StoreConfig syncStatsStoreConfig = new StoreConfig();
        syncStatsStoreConfig.setKlass(SyncStats.class);
        syncStatsStoreConfig.setType(StoreTypes.SQL);
        syncStatsStoreConfig.setBuilder(builder);
        syncStatsStoreConfig.setContext(getApplicationContext());

        statsStore = (SQLStore<SyncStats>) dm.store("stats", syncStatsStoreConfig);

        final CountDownLatch latch = new CountDownLatch(3);
        CountdownCallback callback = new CountdownCallback();
        callback.latch = latch;
        statsStore.open(callback);
        scheduleStore.open(callback);
        userCalendarStore.open(callback);
        try {
            latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void getSchedule(final ScheduleAdapter adapter, ScheduleFragment scheduleFragment) {
        Schedule schedule;
        UserCalendarList calendar = new UserCalendarList();
        Collection<UserCalendarList> read = userCalendarStore.readAll();
        if (read != null && read.size() > 0) {
            calendar = read.iterator().next();
        }

        Collection<SyncStats> stats = statsStore.readAll();
        if (stats.isEmpty()) {
            loadScheudle(adapter, scheduleFragment);
            return;
        } else {
            SyncStats stat = stats.iterator().next();
            if (stat.getScheduleExpires().before(new Date())) {
                loadScheudle(adapter, scheduleFragment);
                return;
            }
        }
        //check Store
        Collection<Schedule> scheduleList = scheduleStore.readAll();
        if (scheduleList.isEmpty()) {
            loadScheudle(adapter, scheduleFragment);
            return;
        }

        schedule = scheduleList.iterator().next();

        adapter.update(schedule, calendar);

    }

    private void loadScheudle(final ScheduleAdapter adapter, final ScheduleFragment scheduleFragment) {

        schedulePipe = pipeline.get("schedule", scheduleFragment, getApplicationContext());
        schedulePipe.read(new Callback<List<Schedule>>() {
            @Override
            public void onSuccess(List<Schedule> schedules) {
                Schedule schedule = schedules.get(0);
                scheduleStore.reset();
                scheduleStore.save(schedule);
                statsStore.reset();
                adapter.update(schedule, new UserCalendarList());
                loadCalendar(adapter, scheduleFragment);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("LOAD_CAL", e.getMessage(), e);
            }
        });
    }

    private void loadCalendar(final ScheduleAdapter adapter, ScheduleFragment scheduleFragment) {

        userCalendarPipe = pipeline.get("calendar", scheduleFragment, getApplicationContext());
        userCalendarPipe.read(new Callback<List<UserCalendarList>>() {
            @Override
            public void onSuccess(List<UserCalendarList> schedules) {
                UserCalendarList schedule = schedules.get(0);
                userCalendarStore.reset();
                userCalendarStore.save(schedule);
                statsStore.reset();
                adapter.update(schedule);

            }

            @Override
            public void onFailure(Exception e) {
                Log.e("LOAD_CAL", e.getMessage(), e);
            }
        });
    }


    public AuthenticationModule getAuth(MainActivity mainActivity) {
        return authenticator.get("google", mainActivity);
    }

    private static class CountdownCallback implements Callback {

        CountDownLatch latch;

        @Override
        public void onSuccess(Object o) {
            latch.countDown();
        }

        @Override
        public void onFailure(Exception e) {
            latch.countDown();
        }
    }

}
