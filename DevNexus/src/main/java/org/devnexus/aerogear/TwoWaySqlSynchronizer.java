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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.collect.Sets;

import org.devnexus.util.CountDownCallback;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.DataManager;
import org.jboss.aerogear.android.Pipeline;
import org.jboss.aerogear.android.impl.datamanager.SQLStore;
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
public class TwoWaySqlSynchronizer<T> implements Synchronizer<T> {

    private final Object lock = new Object();
    private static final String TAG = TwoWaySqlSynchronizer.class.getSimpleName();
    private final PipeConfig pipeConfig;
    private final Pipe<T> adapter;
    private final StoreConfig storeConfig;
    private final SQLStore<T> localStore;
    private final ShadowStore<T> localShadowStore;
    private final DataManager manager = new DataManager();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Pipeline pipeline;

    private final String recordIdFieldName;
    private final Property property;

    final List<SynchronizeEventListener<T>> listeners = new ArrayList<SynchronizeEventListener<T>>();

    public TwoWaySqlSynchronizer(TwoWaySqlSynchronizerConfig config) {
        storeConfig = config.getStoreConfig();
        pipeConfig = config.getPipeConfig();

        pipeline = new Pipeline(config.getPipeConfig().getBaseURL());

        localStore = (SQLStore<T>) manager.store("local", storeConfig);
        localShadowStore = new ShadowStore<T>(storeConfig.getKlass(), storeConfig.getContext(), storeConfig.getBuilder(), storeConfig.getIdGenerator());
        adapter = pipeline.pipe(config.klass, pipeConfig);

        recordIdFieldName = Scan.recordIdFieldNameIn(config.klass);
        property = new Property(config.klass, recordIdFieldName);
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
    public synchronized void beginSync(Context appContext, Callback<Void> syncBegunCallback) {

        CountDownLatch latch = new CountDownLatch(2);

        try {
            localShadowStore.open(new CountDownCallback<ShadowStore<T>>(latch));
            localStore.open(new CountDownCallback(latch));
            latch.await(1000, TimeUnit.MILLISECONDS);
            syncBegunCallback.onSuccess(null);
        } catch (Exception e) {
            syncBegunCallback.onFailure(e);
        }

    }

    @Override
    public void syncNoMore(Context appContext, Callback<T> syncFinishedCallback) {
        localStore.close();
        localShadowStore.close();
    }

    /**
     * This method will load the remote data set and reset the local data to it.
     * This is useful for the initial data fetch.
     */
    @Override
    public void resetToRemoteState(final Callback<List<T>> callback) {

        adapter.read(new Callback<List<T>>() {
            @Override
            public void onSuccess(List<T> data) {
                localShadowStore.reset();
                localStore.reset();

                for (T item : data) {
                    localStore.save(item);
                    localShadowStore.save(item);
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
     */
    public void sync() {
        synchronized (lock) {
            Set<T> localData = new HashSet<T>(localStore.readAll());
            Set<T> shadowData = new HashSet<T>(localShadowStore.readAll());
            Sets.SetView<T> localChanges = Sets.difference(localData, shadowData);
            //pipeConfig.getRequestBuilder().getBody(shadowData)
            for (T change : localChanges) {
                adapter.save(change, new LoggingCallback<T>());
                localShadowStore.remove((Serializable) property.getValue(change));
                localShadowStore.save(change);
            }
        }

    }

    @Override
    public void loadRemoteChanges() {
        final CountDownLatch latch = new CountDownLatch(1);
        synchronized (lock) {
            adapter.read(new Callback<List<T>>() {
                @Override
                public void onSuccess(List<T> data) {
                    try {
                        Set<T> localData = new HashSet<T>(data);
                        Set<T> shadowData = new HashSet<T>(localShadowStore.readAll());
                        Sets.SetView<T> localChanges = Sets.difference(localData, shadowData);
                        for (T change : localChanges) {
                            localShadowStore.remove((Serializable) property.getValue(change));
                            localShadowStore.save(change);
                            localStore.remove((Serializable) property.getValue(change));
                            localStore.save(change);

                        }
                    } finally {
                        latch.countDown();
                        notifyListeners(localStore.readAll());
                    }
                }

                @Override
                public void onFailure(Exception e) {
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

    private void notifyListeners(final Collection<T> data) {

        handler.post(new Runnable() {

            @Override
            public void run() {
                for (SynchronizeEventListener<T> listener : listeners) {
                    listener.dataUpdated(data);
                }
            }
        });

    }

    public SQLStore<T> getLocalStore() {
        return localStore;
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

    private static class LoggingCallback<T> implements Callback<T> {

        @Override
        public void onSuccess(T data) {
            Log.d("LoggingCallback", "Succesfully saved:" + data);
        }

        @Override
        public void onFailure(Exception e) {
            Log.e("LoggingCallback", e.getMessage(), e);
        }
    }
}
