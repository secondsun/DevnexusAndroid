package org.devnexus.vo;

import java.util.Date;

/**
 * Created by summers on 12/2/13.
 */
public class SyncStats {



    private Date scheduleExpires = new Date();
    private Date calendarExpires = new Date();

    public Date getScheduleExpires() {
        return scheduleExpires;
    }

    public void setScheduleExpires(Date scheduleExpires) {
        this.scheduleExpires = scheduleExpires;
    }

    public Date getCalendarExpires() {
        return calendarExpires;
    }

    public void setCalendarExpires(Date calendarExpires) {
        this.calendarExpires = calendarExpires;
    }
}
