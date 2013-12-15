package org.devnexus.sync;

/**
 * Created by summers on 12/9/13.
 */
public class PeriodicSynchronizerConfig {

    private int period = 3600; //One Hour

    public void setPeriod(int periodInSeconds) {
        this.period = periodInSeconds;
    }

    public int getPeriod() {
        return period;
    }


}
