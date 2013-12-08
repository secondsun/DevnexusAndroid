package org.devnexus.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.devnexus.R;
import org.devnexus.util.ResourceUtils;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.UserCalendarList;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by summers on 12/2/13.
 *
 * This is the list adapter for the custom schedule lists
 */
public class ScheduleAdapter extends BaseAdapter {


    public static final int DATE_TYPE = 0;
    public static final int ITEM_TYPE = 1;
    private static final String TAG = ScheduleAdapter.class.getSimpleName();


    final DateFormat format = new SimpleDateFormat("h:mm a");

    private Schedule schedule;
    private UserCalendarList calendar;

    private Date dayOne;
    private Date dayTwo;

    private int dayOneSize = 0;
    private final Context appContext;


    public ScheduleAdapter(Schedule schedule, UserCalendarList calendar, Context appContext) {
        Log.d(TAG, "Constructor with data:" + calendar.userCalendarList.size());
        this.schedule = schedule;
        this.calendar = calendar;
        dayOne = schedule.scheduleItemList.days.get(0);
        dayTwo = schedule.scheduleItemList.days.get(1);
        dayOneSize = 0;
        this.appContext = appContext;
        for (UserCalendar item : calendar.userCalendarList) {
            if (item.fromTime.after(dayTwo)) {

            } else {
                dayOneSize++;
            }
        }



    }

    public void update(Schedule schedule, UserCalendarList calendar) {
        Log.d(TAG, "Updating new data:" + calendar.userCalendarList.size());
        this.schedule = schedule;
        this.calendar = calendar;
        dayOne = schedule.scheduleItemList.days.get(0);
        dayTwo = schedule.scheduleItemList.days.get(1);
        dayOneSize = 0;
        for (UserCalendar item : calendar.userCalendarList) {
            if (item.fromTime.after(dayTwo)) {

            } else {
                dayOneSize++;
            }
        }

        super.notifyDataSetChanged();
    }

    public void update(UserCalendarList calendar) {
        Log.d(TAG, "Updating new data:" + calendar.userCalendarList.size());
        this.calendar = calendar;
        dayOne = schedule.scheduleItemList.days.get(0);
        dayTwo = schedule.scheduleItemList.days.get(1);
        dayOneSize = 0;
        for (UserCalendar item : calendar.userCalendarList) {
            if (item.fromTime.after(dayTwo)) {

            } else {
                dayOneSize++;
            }
        }

        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return calendar.userCalendarList.size() + schedule.scheduleItemList.days.size();
    }

    @Override
    public Object getItem(int position) {
        if (position == 0 ) {
            return dayOne;
        } else if (position == dayOneSize + 1) {
            return dayTwo;
        } else if (position > dayOneSize) {
            return calendar.userCalendarList.get(position - 2); //Offset for two date header items
        } else {
            return calendar.userCalendarList.get(position - 1); //Offset for one date header item
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 ) {
            return DATE_TYPE;
        } else if (position == dayOneSize + 1) {
            return DATE_TYPE;
        }  else if (position > dayOneSize) {
            return ITEM_TYPE;
        } else {
            return ITEM_TYPE;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        switch (getItemViewType(position)) {
            case DATE_TYPE:
                convertView = getDateView(convertView);
                ((ViewHolder)convertView.getTag()).date.setText(getDateHeader(position));
                return convertView;
            case ITEM_TYPE:
                convertView = getItemView(convertView);

                ViewHolder holder = (ViewHolder) convertView.getTag();
                UserCalendar calendarEntry = ((UserCalendar) getItem(position));
                ScheduleItem item = calendarEntry.item;
                holder.date.setText(format.format(calendarEntry.fromTime));
                if (item  != null) {
                    holder.date.setBackgroundResource(ResourceUtils.trackCSSToColor(item.room.cssStyleName));
                    if (item.room != null && item.room.name != null)
                        holder.roomName.setText(item.room.name);
                    if (item.presentation != null) {
                        holder.title.setText(item.presentation.title);
                    } else {
                        holder.title.setText(item.title);
                    }
                } else {
                    holder.date.setBackgroundResource(R.color.dn_blue);
                    holder.title.setText("Available");
                }
                return convertView;
        }
        return null;
    }

    private String getDateHeader(int position) {
        return position == 0 ? "Day 0" : "Day 1";
    }

    private View getDateView(View convertView) {
        if (convertView == null || convertView.findViewById(R.id.date) == null) {
            Log.d(TAG, "inflating date");
            LayoutInflater inflater = (LayoutInflater) appContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = inflater.inflate(R.layout.date_breaker_list_item, null);
            ViewHolder holder = new ViewHolder();
            holder.date = (TextView) convertView.findViewById(R.id.date);
            convertView.setTag(holder);
        }
        return convertView;
    }

    private View getItemView(View convertView) {
        if (convertView == null || convertView.findViewById(R.id.session_room) == null) {
            Log.d(TAG, "inflating item.  Convert was " + convertView);
            LayoutInflater inflater = (LayoutInflater) appContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            convertView = inflater.inflate(R.layout.schedule_list_item, null);
            ViewHolder holder = new ViewHolder();
            holder.date = ((TextView)convertView.findViewById(R.id.start_time));
            holder.roomName = ((TextView)convertView.findViewById(R.id.session_room));
            holder.title = ((TextView)convertView.findViewById(R.id.session_title));
            convertView.setTag(holder);
        }
        return convertView;
    }

    private static class ViewHolder {
        private TextView date;
        private TextView title;
        private TextView roomName;
    }

}
