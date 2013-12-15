package org.devnexus.sync;

import org.devnexus.DevnexusApplication;
import org.jboss.aerogear.android.Provider;
import org.jboss.aerogear.android.sync.*;
/**
 * Created by summers on 12/14/13.
 */
public class DevnexusCalendarSyncProvider implements Provider<Synchronizer> {
    @Override
    public Synchronizer get(Object... in) {
        return DevnexusApplication.CONTEXT.getUserCalendarSync();
    }
}
