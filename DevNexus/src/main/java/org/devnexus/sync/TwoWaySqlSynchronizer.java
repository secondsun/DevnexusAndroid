package org.devnexus.sync;

import android.content.Context;
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
 * Created by summers on 12/11/13.
 */
public class TwoWaySqlSynchronizer<T> implements Synchronizer<T> {

    private final Object lock = new Object();
    private static final String TAG = TwoWaySqlSynchronizer.class.getSimpleName();
    private final PipeConfig pipeConfig;
    private Pipe<T> adapter;
    private StoreConfig storeConfig;
    private SQLStore<T> localStore;
    private ShadowStore<T> localShadowStore;
    private DataManager manager = new DataManager();

    private Pipeline pipeline;

    private final String recordIdFieldName;
    private final Property property;

    final List<SynchronizeEventListener<T>> listeners = new ArrayList<SynchronizeEventListener<T>>();

    public TwoWaySqlSynchronizer(TwoWaySqlSynchronizerConfig config) {
        storeConfig = new StoreConfig(config.getStoreConfig());
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
    public void beginSync(Context appContext, Callback<Void> syncBegunCallback) {

        CountDownLatch latch = new CountDownLatch(2);

        try {
            localShadowStore.open(new CountDownCallback(latch));
            localStore.open(new CountDownCallback(latch));
            latch.await(1000, TimeUnit.MILLISECONDS);
            syncBegunCallback.onSuccess(null);
        } catch (Exception e) {
            syncBegunCallback.onFailure(e);
        }

    }

    @Override
    public void syncNoMore() {

    }

    /**
     * This method will load the remote data set and reset the local data to it.
     * This is useful for the initial data fetch.
     */
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

        notifyListeners(localStore.readAll());

    }

    private void notifyListeners(Collection<T> data) {
        for (SynchronizeEventListener<T> listener : listeners) {
            listener.dataUpdated(data);
        }
    }

    public SQLStore<T> getLocalStore() {
        return localStore;
    }

    public static class TwoWaySqlSynchronizerConfig {
        private PipeConfig pipeConfig;
        private StoreConfig storeConfig;
        private Class klass;

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

        public Class getKlass() {
            return klass;
        }

        public void setKlass(Class klass) {
            this.klass = klass;
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
