package org.devnexus.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleItemList {
    public int numberOfSessions;
    public int numberOfKeynoteSessions;
    public int numberOfBreakoutSessions;
    public int numberOfSpeakersAssigned;
    public int numberOfUnassignedSessions;
    public int numberOfBreaks;
    public int numberOfTracks;
    public List<Date> days = new ArrayList<Date>();
    public List<ScheduleItem> scheduleItems = new ArrayList<ScheduleItem>();
}
