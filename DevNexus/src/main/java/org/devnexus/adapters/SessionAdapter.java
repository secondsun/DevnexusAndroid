package org.devnexus.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.devnexus.R;
import org.devnexus.util.ResourceUtils;
import org.devnexus.vo.ScheduleItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by summers on 1/3/14.
 */
public class SessionAdapter extends ArrayAdapter<ScheduleItem> {

    private static final DateFormat format = new SimpleDateFormat("h:mm a");


    private static final String TAG = SessionAdapter.class.getSimpleName();

    public SessionAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = getItemView(convertView);

        ViewHolder holder = (ViewHolder) convertView.getTag();

        ScheduleItem item = getItem(position);


        holder.date.setBackgroundResource(ResourceUtils.trackCSSToColor(item.room.cssStyleName));
        if (item.room != null && item.room.name != null)
            holder.roomName.setText(item.room.name);
        if (item.presentation != null) {
            holder.title.setText(item.presentation.title);
        } else {
            holder.title.setText(item.title);
        }
        holder.date.setText(format.format(item.fromTime));
        holder.sessionId = item.id;

        return convertView;

    }

    private View getItemView(View convertView) {
        if (convertView == null || convertView.findViewById(R.id.session_room) == null) {
            Log.d(TAG, "inflating item.  Convert was " + convertView);
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.schedule_list_item, null);
            ViewHolder holder = new ViewHolder();
            holder.date = ((TextView) convertView.findViewById(R.id.start_time));
            holder.roomName = ((TextView) convertView.findViewById(R.id.session_room));
            holder.title = ((TextView) convertView.findViewById(R.id.session_title));
            convertView.setTag(holder);
        }
        return convertView;
    }


    private static class ViewHolder {
        private TextView date;
        private TextView title;
        private TextView roomName;
        private Integer sessionId;
    }

}