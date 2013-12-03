package org.devnexus.vo;

import java.util.Date;

/**
 * Created by summers on 12/1/13.
 */
public class UserCalendar {
    public int id;
    public Date createdDate;
    public Date updatedDate;
    public int version;
    public String username;
    public Date fromTime;
    public ScheduleItem item;
    public boolean fixed = false;
    public boolean template = false;

}
