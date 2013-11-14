package org.devnexus.vo;

import java.util.Date;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleItem {
    public int id;
    public Date createdDate;
    public Date updatedDate;
    public int version;
    public String scheduleItemType;
    public String title;
    public Date fromTime;
    public Date toTime;
    public Room room;
    public Presentation presentation;
    public int rowspan;
}
