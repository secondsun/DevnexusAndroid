package org.devnexus.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.devnexus.R;
import org.devnexus.util.ResourceUtils;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleFragment extends Fragment {

    private static final int DATE_TYPE = 0;
    private static final int ITEM_TYPE = 1;

    public ScheduleFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView view = new ListView(inflater.getContext());
        final Schedule schedule = getSchedule();
        final Date dayOne = schedule.scheduleItemList.days.get(0);
        final Date dayTwo = schedule.scheduleItemList.days.get(1);

        int dayOneSizeTemp = 0;
        int dayTwoSizeTemp = 0;

        for (ScheduleItem item : schedule.scheduleItemList.scheduleItems) {
            if (item.fromTime.after(dayTwo)) {
                dayTwoSizeTemp++;
            } else {
                dayOneSizeTemp++;
            }
        }

        final int dayOneSize = dayOneSizeTemp;
        final int dayTwoSize = dayTwoSizeTemp;

        view.setAdapter(new BaseAdapter() {

            final DateFormat format = new SimpleDateFormat("h:mm a");

            @Override
            public int getCount() {
                return schedule.scheduleItemList.numberOfSessions + schedule.scheduleItemList.days.size();
            }

            @Override
            public Object getItem(int position) {
                if (position == 0 ) {
                    return dayOne;
                } else if (position == dayOneSize + 1) {
                    return dayTwo;
                } else if (position > dayOneSize) {
                    return schedule.scheduleItemList.scheduleItems.get(position - 2); //Offset for two date header items
                } else {
                    return schedule.scheduleItemList.scheduleItems.get(position - 1); //Offset for one date header item
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
                        ScheduleItem item = (ScheduleItem) getItem(position);
                        holder.date.setText(format.format(item.fromTime));
                        holder.date.setBackgroundResource(ResourceUtils.trackCSSToColor(item.room.cssStyleName));
                        holder.roomName.setText(item.room.name);
                        if (item.presentation != null) {
                            holder.title.setText(item.presentation.title);
                        } else {
                            holder.title.setText(item.title);
                        }
                        return convertView;
                }
                return null;
            }

            private String getDateHeader(int position) {
                return position == 0 ? "Day 0" : "Day 1";
            }

            private View getDateView(View convertView) {
                if (convertView == null) {
                    convertView = getLayoutInflater(null).inflate(R.layout.date_breaker_list_item, null);
                    ViewHolder holder = new ViewHolder();
                    holder.date = (TextView) convertView.findViewById(R.id.date);
                    convertView.setTag(holder);
                }
                return convertView;
            }

            private View getItemView(View convertView) {
                if (convertView == null) {
                    convertView = getLayoutInflater(null).inflate(R.layout.schedule_list_item, null);
                    ViewHolder holder = new ViewHolder();
                    holder.date = ((TextView)convertView.findViewById(R.id.start_time));
                    holder.roomName = ((TextView)convertView.findViewById(R.id.session_room));
                    holder.title = ((TextView)convertView.findViewById(R.id.session_title));
                    convertView.setTag(holder);
                }
                return convertView;
            }

        });
        return view;
    }

    Schedule getSchedule() {

        // Creates the json object which will manage the information received
        GsonBuilder builder = new GsonBuilder();
        // Register an adapter to manage the date types as long values
        builder.registerTypeAdapter(Date.class, new JsonDeserializer() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        return builder.create().fromJson(new InputStreamReader(getResources().openRawResource(R.raw.schedule)), Schedule.class);
    }

    private static class ViewHolder {
        private TextView date;
        private TextView title;
        private TextView roomName;
    }

}
