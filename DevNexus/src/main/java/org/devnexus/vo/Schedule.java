package org.devnexus.vo;

import org.jboss.aerogear.android.RecordId;

/**
 * Created by summers on 11/13/13.
 */
public class Schedule {

    @RecordId
    private Long id = -1l;

    public String headerTitle = "";
    public ScheduleItemList scheduleItemList = new ScheduleItemList();
    public String tag = "";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
