package org.devnexus.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.devnexus.DevnexusApplication;
import org.devnexus.R;
import org.devnexus.adapters.SessionAdapter;
import org.devnexus.util.GsonUtils;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.contract.ScheduleContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by summers on 1/3/14.
 */
public class TrackViewFragment extends DialogFragment {

    private static final String ROOM_NAME = "TrackViewFragment.ROOM_NAME";
    private static final String TAG = TrackViewFragment.class.getSimpleName();

    private SessionAdapter adapter;

    private List<ScheduleItem> schedule;
    private ListView view;
    private static final Gson GSON = GsonUtils.GSON;

    public static TrackViewFragment newInstance(String roomName) {
        Bundle args = new Bundle();

        if (Lists.newArrayList("Ballroom C", "Ballroom D", "Ballroom F").contains(roomName)) {
            roomName = "Ballroom CDF";
        }

        args.putString(ROOM_NAME, roomName);
        TrackViewFragment frag = new TrackViewFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);


        if (adapter == null) {
            adapter = new SessionAdapter(activity.getApplicationContext(), R.layout.schedule_list_item);
        }

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... params) {

                Bundle args = getArguments();
                String trackName = args.getString(ROOM_NAME);

                if (schedule == null) {
                    Cursor cursor = getActivity().getContentResolver().query(ScheduleContract.URI, null, null, null, null);

                    if (cursor.moveToNext()) {
                        Schedule scheduleFromDb = GSON.fromJson(cursor.getString(0), Schedule.class);
                        schedule = new ArrayList<ScheduleItem>(10);
                        for (ScheduleItem scheduleItem : scheduleFromDb.scheduleItemList.scheduleItems) {
                            if (scheduleItem.room.name.equals(trackName)) {
                                schedule.add(scheduleItem);
                            }
                        }
                    } else {
                        //???
                    }

                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                adapter.clear();
                for (ScheduleItem item : schedule) {
                    adapter.add(item);
                }

                adapter.notifyDataSetChanged();
                if (view != null) {
                    view.requestLayout();
                    view.refreshDrawableState();
                }
            }
        }.executeOnExecutor(DevnexusApplication.EXECUTORS);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(getArguments().getString(ROOM_NAME));
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = new ListView(inflater.getContext());
        view.setAdapter(adapter);

        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserCalendar userCal = new UserCalendar();
                userCal.fixed = true;
                SessionDetailFragment.newInstance(userCal, adapter.getItem(position)).show(getActivity().getSupportFragmentManager(), TAG);
            }
        });


        return view;
    }
}
