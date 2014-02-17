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

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import org.devnexus.util.CountDownCallback;
import org.devnexus.util.GsonUtils;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.contract.ScheduleContract;
import org.devnexus.vo.contract.SingleColumnJsonArrayList;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.Pipeline;
import org.jboss.aerogear.android.impl.datamanager.StoreConfig;
import org.jboss.aerogear.android.impl.pipeline.PipeConfig;
import org.jboss.aerogear.android.impl.reflection.Property;
import org.jboss.aerogear.android.impl.reflection.Scan;
import org.jboss.aerogear.android.pipeline.Pipe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This Synchronizer uses two SQL databases to manage local state
 */
public class ScheduleSynchronizer implements Synchronizer<Schedule> {

    private final Object lock = new Object();
    private static final String TAG = ScheduleSynchronizer.class.getSimpleName();
    private final PipeConfig pipeConfig;
    private final Pipe<Schedule> adapter;
    private final StoreConfig storeConfig;
    private final ShadowStore<Schedule> localShadowStore;

    private static final Gson GSON = GsonUtils.GSON;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Pipeline pipeline;

    private final String recordIdFieldName;
    private final Property property;

    final List<SynchronizeEventListener<Schedule>> listeners = new ArrayList<SynchronizeEventListener<Schedule>>();

    public ScheduleSynchronizer(TwoWaySqlSynchronizerConfig config) {
        storeConfig = config.getStoreConfig();
        pipeConfig = config.getPipeConfig();

        pipeline = new Pipeline(config.getPipeConfig().getBaseURL());

        localShadowStore = new ShadowStore<Schedule>(storeConfig.getKlass(), storeConfig.getContext(), storeConfig.getBuilder(), storeConfig.getIdGenerator());
        adapter = pipeline.pipe(config.klass, pipeConfig);

        recordIdFieldName = Scan.recordIdFieldNameIn(config.klass);
        property = new Property(config.klass, recordIdFieldName);
    }

    @Override
    public void addListener(SynchronizeEventListener<Schedule> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SynchronizeEventListener<Schedule> listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized void beginSync(Context appContext, Callback<Void> syncBegunCallback) {

        CountDownLatch latch = new CountDownLatch(2);

        try {
            localShadowStore.open(new CountDownCallback<ShadowStore<Schedule>>(latch));
            latch.await(1000, TimeUnit.MILLISECONDS);
            syncBegunCallback.onSuccess(null);
        } catch (Exception e) {
            syncBegunCallback.onFailure(e);
        }

    }

    @Override
    public void syncNoMore(Context appContext, Callback<Schedule> syncFinishedCallback) {
        localShadowStore.close();
    }

    /**
     * This method will load the remote data set and reset the local data to it.
     * This is useful for the initial data fetch.
     */
    @Override
    public void resetToRemoteState(final ContentProviderClient provider, final Callback<List<Schedule>> callback) {

        final Gson gson = GSON;

        adapter.read(new Callback<List<Schedule>>() {
            @Override
            public void onSuccess(List<Schedule> data) {
                localShadowStore.reset();
                try {
                    provider.delete(ScheduleContract.URI, null, null);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                    callback.onFailure(e);
                    return;
                }

                ContentValues[] values = new ContentValues[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    Schedule item = data.get(i);
                    ContentValues value = new ContentValues();
                    value.put(ScheduleContract.DATA, gson.toJson(item));
                    values[i] = value;

                    localShadowStore.save(item);
                }
                try {
                    provider.bulkInsert(ScheduleContract.URI, values);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                    callback.onFailure(e);
                    return;
                }
                notifyListeners(data);
                callback.onSuccess(data);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * This method will sync a local object with a remote object.
     *
     * @param provider
     */
    public void sync(ContentProviderClient provider) {
        ArrayList<Schedule> calendars = new ArrayList<Schedule>();
        synchronized (lock) {
            try {
                SingleColumnJsonArrayList var = (SingleColumnJsonArrayList) provider.query(ScheduleContract.URI, null, null, null, null);
                while (var.moveToNext()) {
                    calendars.add(GSON.fromJson(var.getString(0), Schedule.class));
                }
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage(), e);
                throw new RuntimeException(e);
            }
            Set<Schedule> localData = new HashSet<Schedule>(calendars);
            Set<Schedule> shadowData = new HashSet<Schedule>(localShadowStore.readAll());
            Sets.SetView<Schedule> localChanges = Sets.difference(localData, shadowData);
            //pipeConfig.getRequestBuilder().getBody(shadowData)
            for (Schedule change : localChanges) {
                adapter.save(change, new LoggingCallback());
                localShadowStore.remove((Serializable) property.getValue(change));
                localShadowStore.save(change);
            }
        }

    }

    @Override
    public void loadRemoteChanges(final ContentProviderClient provider) {
        final CountDownLatch latch = new CountDownLatch(1);


        synchronized (lock) {
            adapter.read(new Callback<List<Schedule>>() {
                @Override
                public void onSuccess(List<Schedule> data) {
                    try {
                        Set<Schedule> localData = new HashSet<Schedule>(data);
                        Set<Schedule> shadowData = new HashSet<Schedule>(localShadowStore.readAll());
                        Sets.SetView<Schedule> localChanges = Sets.difference(localData, shadowData);
                        for (Schedule change : localChanges) {
                            localShadowStore.remove((Serializable) property.getValue(change));
                            localShadowStore.save(change);

                            ContentValues values = new ContentValues();
                            values.put(ScheduleContract.DATA, GSON.toJson(change));
                            provider.update(ScheduleContract.URI, values, "", new String[]{change.getId() + ""});

                            SingleColumnJsonArrayList var = (SingleColumnJsonArrayList) provider.query(ScheduleContract.URI, null, null, null, null);
                            ArrayList<Schedule> calendars = new ArrayList<Schedule>();
                            while (var.moveToNext()) {
                                calendars.add(GSON.fromJson(var.getString(0), Schedule.class));
                            }
                            latch.countDown();
                            notifyListeners(calendars);

                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        latch.countDown();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

    }

    private void notifyListeners(final Collection<Schedule> data) {

        handler.post(new Runnable() {

            @Override
            public void run() {
                for (SynchronizeEventListener<Schedule> listener : listeners) {
                    listener.dataUpdated(data);
                }
            }
        });

    }


    public static class TwoWaySqlSynchronizerConfig {

        private PipeConfig pipeConfig;
        private StoreConfig storeConfig;
        private Class klass;

        public TwoWaySqlSynchronizerConfig(Class klass) {
            this.klass = klass;
        }

        public PipeConfig getPipeConfig() {
            return pipeConfig;
        }

        public void setPipeConfig(PipeConfig pipeConfig) {
            this.pipeConfig = pipeConfig;
        }

        public StoreConfig getStoreConfig() {
            return storeConfig;
        }

        public void setStoreConfig(StoreConfig storeConfig) {
            this.storeConfig = storeConfig;
        }

    }

    private static class LoggingCallback implements Callback<Schedule> {

        @Override
        public void onSuccess(Schedule data) {
            Log.d("LoggingCallback", "Succesfully saved:" + data);
        }

        @Override
        public void onFailure(Exception e) {
            Log.e("LoggingCallback", e.getMessage(), e);
        }
    }
}
