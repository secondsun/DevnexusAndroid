package org.devnexus;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
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
import org.devnexus.util.AccountUtil;
import org.devnexus.util.CountDownCallback;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.UserCalendar;
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
import org.jboss.aerogear.android.sync.PeriodicDataSynchronizer;
import org.jboss.aerogear.android.sync.PeriodicDataSynchronizer.PeriodicSynchronizerConfig;
import org.jboss.aerogear.android.sync.Synchronizer;
import org.jboss.aerogear.android.sync.TwoWaySqlSynchronizer;
import org.jboss.aerogear.android.unifiedpush.PushConfig;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.Registrations;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    private static final URI PUSH_URL;
    public static DevnexusApplication CONTEXT = null;
    private SQLStore<Schedule> scheduleStore;
    private SQLStore<UserCalendar> userCalendarStore;
    private Authenticator authenticator;
    private TwoWaySqlSynchronizer<UserCalendar> userCalendarSync;

    private GsonBuilder builder = new GsonBuilder();

    private PeriodicDataSynchronizer<Schedule> scheduleSynchronizer;

    private Pipe<Schedule> schedulePipe;

    private final DataManager dm = new DataManager();
    private final Pipeline pipeline;

    private Handler mainLoopHandler;

    private final Registrations registrations = new Registrations();
    private PushConfig pushConfig;

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

    {
        pipeline = new Pipeline(DEVNEXUS_URL);
        authenticator = new Authenticator(DEVNEXUS_URL);

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

        CONTEXT = this;

        mainLoopHandler = new Handler(getMainLooper());

        AuthenticationConfig config = new AuthenticationConfig();

        config.setLoginEndpoint("loginAndroid.json");

        GooglePlusAuthenticationModule module = new GooglePlusAuthenticationModule(DEVNEXUS_URL, config, getApplicationContext());

        authenticator.add("google", module);


        PipeConfig schedulePipeConfig = new PipeConfig(DEVNEXUS_URL, Schedule.class);
        schedulePipeConfig.setEndpoint("schedule.json");
        schedulePipeConfig.setName("schedule");
        schedulePipeConfig.setResponseParser(new GsonResponseParser(builder.create()));
        schedulePipe = pipeline.pipe(Schedule.class, schedulePipeConfig);

        PipeConfig userCalendarPipeConfig = new PipeConfig(DEVNEXUS_URL, UserCalendar.class);
        userCalendarPipeConfig.setName("calendar");
        userCalendarPipeConfig.setAuthModule(module);
        userCalendarPipeConfig.setResponseParser(new GsonResponseParser(builder.create()));

        StoreConfig scheduleItemStoreConfig = new StoreConfig();
        scheduleItemStoreConfig.setType(StoreTypes.SQL);
        scheduleItemStoreConfig.setKlass(Schedule.class);
        scheduleItemStoreConfig.setBuilder(builder);
        scheduleItemStoreConfig.setContext(getApplicationContext());
        scheduleStore = (SQLStore<Schedule>) dm.store("scheduleItem", scheduleItemStoreConfig);

        StoreConfig userCalendarStoreConfig = new StoreConfig();
        userCalendarStoreConfig.setType(StoreTypes.SQL);
        userCalendarStoreConfig.setKlass(UserCalendar.class);
        userCalendarStoreConfig.setContext(getApplicationContext());
        userCalendarStoreConfig.setBuilder(builder);
        userCalendarStore = (SQLStore<UserCalendar>) dm.store("userCalendar", userCalendarStoreConfig);



        TwoWaySqlSynchronizer.TwoWaySqlSynchronizerConfig syncConfig = new TwoWaySqlSynchronizer.TwoWaySqlSynchronizerConfig();
        syncConfig.setKlass(UserCalendar.class);
        syncConfig.setPipeConfig(userCalendarPipeConfig);
        syncConfig.setStoreConfig(userCalendarStoreConfig);

        userCalendarSync = new TwoWaySqlSynchronizer<UserCalendar>(syncConfig);

        PeriodicSynchronizerConfig scheduleConfig = new PeriodicSynchronizerConfig(Schedule.class);
        scheduleConfig.setPeriod(3600);
        scheduleConfig.setPipeConfig(schedulePipeConfig);
        scheduleConfig.setStoreConfig(scheduleItemStoreConfig);

        scheduleSynchronizer = new PeriodicDataSynchronizer<Schedule>(scheduleConfig);

        // Create a PushConfig for the UnifiedPush Server:
        pushConfig = new PushConfig(PUSH_URL, "402595014005");
        pushConfig.setVariantID("a26c1609-873e-427e-9106-7a6d435cbc78");
        pushConfig.setSecret("5264fdc8-f0e6-480a-8725-f24caa9440e5");

        CountDownLatch latch = new CountDownLatch(2);
        CountDownCallback callback = new CountDownCallback(latch);

        userCalendarStore.open(callback);
        scheduleStore.open(callback);


        try {
            latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        latch = new CountDownLatch(2);
        callback = new CountDownCallback(latch);


        userCalendarSync.beginSync(this, callback);
        scheduleSynchronizer.beginSync(this, callback);

        try {
            latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (AccountUtil.hasConnected(this)) {
            registerForPush(AccountUtil.getUsername(this));
        }


    }

    public void getSchedule(final ScheduleAdapter adapter, final ScheduleFragment scheduleFragment) {
        if (scheduleStore.readAll() == null || scheduleStore.readAll().isEmpty()) {
            scheduleSynchronizer.loadRemoteChanges();
        }
    }

    public Schedule getScheduleFromDataStore() {
        return scheduleStore.readAll().iterator().next();
    }

    private void loadCalendar(final ScheduleAdapter adapter, ScheduleFragment scheduleFragment) {

        userCalendarSync.resetToRemoteState(new Callback<List<UserCalendar>>() {
            @Override
            public void onSuccess(final List<UserCalendar> schedules) {

                mainLoopHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.update(schedules);
                    }
                });

            }

            @Override
            public void onFailure(Exception e) {
                Log.e("LOAD_CAL", e.getMessage(), e);
            }
        });

    }


    public AuthenticationModule getAuth(Activity mainActivity) {
        return authenticator.get("google", mainActivity);
    }

    public TwoWaySqlSynchronizer<UserCalendar> getUserCalendarSync() {
        return userCalendarSync;
    }


    public SQLStore<UserCalendar> getCalendarStore() {
        return userCalendarStore;
    }

    public void registerForPush(String accountName) {
        pushConfig.setAlias(accountName);
        PushRegistrar pushRegistration = registrations.push("gcm", pushConfig);
        pushRegistration.register(this, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "Push registration successful.");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    public Synchronizer getScheduleSync() {
        return scheduleSynchronizer;
    }
}
