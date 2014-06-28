package org.devnexus.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.devnexus.R;
import org.devnexus.util.CachingImageProvider;
import org.devnexus.util.ResourceUtils;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by summers on 6/23/14.
 */
public class PresentationAdapter extends ArrayAdapter<ScheduleItem> {

    private List<ScheduleItem> items;

    public PresentationAdapter(Context context, int resource, List<ScheduleItem> items) {
        super(context, resource, items);
        this.items = new ArrayList<ScheduleItem>(items);
    }


    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ScheduleItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        if (convertView == null) {
            convertView = makeView();
        }
        final ViewHolder holder = (ViewHolder) convertView.getTag();
        ScheduleItem item = ((ScheduleItem) getItem(position));

        if (item != null) {
            holder.description.setBackgroundResource(ResourceUtils.trackCSSToColor(item.room.cssStyleName));
            holder.gradient.setBackgroundResource(ResourceUtils.trackCSSToGradient(item.room.cssStyleName));
            if (item.presentation != null && item.presentation.speaker != null) {
                CachingImageProvider.getInstance().loadImage(Uri.parse("http://www.devnexus.com/s/speakers/" + item.presentation.speaker.id + ".jpg"), new CachingImageProvider.ImageLoaded() {

                    @Override
                    public void onImageLoad(Bitmap bitmap) {
                        holder.image.setImageBitmap(bitmap);
                    }
                });
            }
            if (item.presentation != null) {
                holder.title.setText(item.presentation.title);
            } else {
                holder.title.setText(item.title);
            }
        }
        holder.title.setSelected(true);
        convertView.invalidate();
        convertView.requestLayout();
        convertView.refreshDrawableState();

        return convertView;

    }

    private View makeView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.presentation_list_item, null);
        ViewHolder holder = new ViewHolder();

        holder.description = (TextView) view.findViewById(R.id.session_description);
        holder.gradient = view.findViewById(R.id.gradient);
        holder.title = (TextView) view.findViewById(R.id.session_title);
        holder.image = (ImageView) view.findViewById(R.id.header_image);

        view.setTag(holder);

        return view;
    }

    private static class ViewHolder {
        private TextView description;
        private View gradient;
        private TextView title;
        private ImageView image;
    }

}
