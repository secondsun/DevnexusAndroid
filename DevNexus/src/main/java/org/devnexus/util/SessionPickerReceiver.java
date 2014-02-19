package org.devnexus.util;

import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;

/**
 * Created by summers on 12/17/13.
 */
public interface SessionPickerReceiver {
    void receiveSessionItem(UserCalendar calendarItem, ScheduleItem session);
}
