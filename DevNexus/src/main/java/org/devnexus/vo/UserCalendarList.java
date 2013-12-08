package org.devnexus.vo;

import org.jboss.aerogear.android.RecordId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by summers on 12/1/13.
 */
public class UserCalendarList {
    @RecordId
    private Long id = -1l;

    public List<UserCalendar> userCalendarList = new ArrayList<UserCalendar>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
