package org.devnexus.sync;

import java.util.Collection;

public interface SynchronizeEventListener<T> {

    /**
     * This is called whenever the underlying Synchronizer has new data.
     *
     * @param newData
     */
    void dataUpdated(Collection<T> newData);

    /**
     * This is called when there is a data conflict.
     * <p/>
     * The system will save the returned data.
     *
     * @param clientData
     * @param serverData
     * @return
     */
    T resolveConflicts(T clientData, T serverData);

}
