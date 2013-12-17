package org.devnexus.fragments;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import org.devnexus.R;
import org.devnexus.util.CachingImageProvider;
import org.devnexus.util.ResourceUtils;
import org.devnexus.util.SessionPickerReceiver;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class SessionDetailFragment extends DialogFragment implements CachingImageProvider.ImageLoaded {

    private static final String USER_CALENDAR = "SessionDetailFragment.UserCalendar";
    private static final String SCHEDULE_ITEM = "SessionDetailFragment.ScheduleItem";
    private static final DateFormat FORMAT = new SimpleDateFormat("EEE MM dd hh:mm a");

    private View.OnClickListener addSession = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            receiver.receiveSessionItem(calendarSlot, scheduleItem);

            setText(R.id.add_to_schedule_button, "This session is on your calendar.");
            view.findViewById(R.id.add_to_schedule_button).setBackgroundResource(R.color.dn_blue);
            v.setOnClickListener(removeSession);
        }
    };


    private View.OnClickListener removeSession = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            receiver.receiveSessionItem(calendarSlot, null);
            setText(R.id.add_to_schedule_button, "Add to Schedule");
            view.findViewById(R.id.add_to_schedule_button).setBackgroundResource(R.color.dn_white);
            v.setOnClickListener(addSession);
        }
    };

    private View view;
    private UserCalendar calendarSlot;
    private ScheduleItem scheduleItem;
    private SessionPickerReceiver receiver;
    private AsyncTask<Void, Void, Bitmap> imageLoader;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        calendarSlot = (UserCalendar) getArguments().getSerializable(USER_CALENDAR);
        scheduleItem = (ScheduleItem) getArguments().getSerializable(SCHEDULE_ITEM);
        view = inflater.inflate(R.layout.schedule_detail_fragment, null);

        if (calendarSlot == null || calendarSlot.fixed) {
            hide(R.id.add_to_schedule_button);
        } else {

            if (calendarSlot.item != null && scheduleItem.id == calendarSlot.item.id) {
                setText(R.id.add_to_schedule_button, "This session is on your calendar.");
                view.findViewById(R.id.add_to_schedule_button).setBackgroundResource(R.color.dn_blue);
                addListener(R.id.add_to_schedule_button, removeSession);
            } else {
                addListener(R.id.add_to_schedule_button, addSession);
            }
        }

        if (scheduleItem.presentation != null) {
            view.findViewById(R.id.schedule_detail_header).setBackgroundResource(ResourceUtils.trackCSSToColor(scheduleItem.room.cssStyleName));
            setText(R.id.session_title, scheduleItem.presentation.title);
            setText(R.id.session_subtitle, String.format("%s in %s", FORMAT.format(scheduleItem.fromTime), scheduleItem.room.name));
            setText(R.id.session_description, scheduleItem.presentation.description);
            setText(R.id.speaker_name, scheduleItem.presentation.speaker.firstName + " " + scheduleItem.presentation.speaker.lastName);
            setText(R.id.speaker_bio, scheduleItem.presentation.speaker.bio);
        } else {
            view.findViewById(R.id.schedule_detail_header).setBackgroundResource(ResourceUtils.trackCSSToColor(scheduleItem.room.cssStyleName));
            setText(R.id.session_title, scheduleItem.title);
            setText(R.id.session_subtitle, String.format("%s in %s", FORMAT.format(scheduleItem.fromTime), scheduleItem.room.name));
            hide(R.id.session_description);
            hide(R.id.speaker_name);
            hide(R.id.speaker_img);
            hide(R.id.speaker_bio);
        }
        return view;
    }

    private void addListener(int viewId, View.OnClickListener onClickListener) {
        view.findViewById(viewId).setOnClickListener(onClickListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (scheduleItem != null && scheduleItem.presentation != null) {
            imageLoader = CachingImageProvider.getInstance().loadImage(Uri.parse("http://www.devnexus.com/s/speakers/" + scheduleItem.presentation.speaker.id + ".jpg"), this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (imageLoader != null) {
            imageLoader.cancel(true);
        }
    }

    private void hide(int viewId) {
        view.findViewById(viewId).setVisibility(View.GONE);
    }

    private void setText(int viewId, String text) {
        ((TextView) view.findViewById(viewId)).setText(text);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public static SessionDetailFragment newInstance(UserCalendar calendarSlot, ScheduleItem scheduleItem) {
        SessionDetailFragment fragment = new SessionDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(USER_CALENDAR, calendarSlot);
        args.putSerializable(SCHEDULE_ITEM, scheduleItem);
        fragment.setArguments(args);
        return fragment;
    }

    public void setReceiver(SessionPickerReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void onImageLoad(Bitmap bitmap) {
        ((ImageView) view.findViewById(R.id.speaker_img)).setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }
}

