package org.devnexus.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.jboss.aerogear.android.sync.*;
import org.devnexus.DevnexusApplication;
import org.devnexus.adapters.ScheduleAdapter;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;
import org.jboss.aerogear.android.impl.datamanager.SQLStore;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleFragment extends Fragment implements SessionPickerFragment.SessionPickerReceiver, SynchronizeEventListener<UserCalendar> {

    private static final String CONTEXT = "org.devnexus.fragments.ScheduleFragment.CONTEXT";
    private static final String TAG = ScheduleFragment.class.getSimpleName();
    private ScheduleAdapter adapter;
    private ListView view;

    private SQLStore<UserCalendar> calendarStore;
    private TwoWaySqlSynchronizer<UserCalendar> synchronizer;

    public ScheduleFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (adapter == null) {
            adapter = new ScheduleAdapter(new Schedule(), new ArrayList<UserCalendar>(), activity.getApplicationContext());
            ((DevnexusApplication)getActivity().getApplication()).getSchedule(adapter, this);
            calendarStore = ((DevnexusApplication) getActivity().getApplication()).getCalendarStore();
            synchronizer = ((DevnexusApplication) getActivity().getApplication()).getUserCalendarSync();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onStart(){
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        DevnexusApplication.CONTEXT.getUserCalendarSync().addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        DevnexusApplication.CONTEXT.getUserCalendarSync().removeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            view = new ListView(inflater.getContext());
            view.setAdapter(adapter);

        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (adapter.getItem(position) instanceof UserCalendar) {
                    UserCalendar item = (UserCalendar) adapter.getItem(position);
                    if (!item.fixed) {
                        if (item.fromTime == null) {
                            Log.e(TAG, "fromTime is null!");
                        }
                        SessionPickerFragment sessionPicker = SessionPickerFragment.newInstance(item.fromTime);
                        sessionPicker.setReceiver(ScheduleFragment.this);
                        sessionPicker.show(getActivity().getSupportFragmentManager(), TAG);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void receiveSessionItem(ScheduleItem session) {

        for (UserCalendar item : adapter.getCalendar()) {
            if (item.fromTime.equals(session.fromTime)) {
                item.item = session;
                calendarStore.remove(item.getId());
                calendarStore.save(item);
            }
        }

        adapter.update(new ArrayList<UserCalendar>(calendarStore.readAll()));
        adapter.notifyDataSetChanged();
        synchronizer.sync();

    }

    @Override
    public void dataUpdated(Collection<UserCalendar> newData) {
        adapter.update(new ArrayList<UserCalendar>(newData));
        adapter.notifyDataSetChanged();
    }

    @Override
    public UserCalendar resolveConflicts(UserCalendar clientData, UserCalendar serverData) {
        return null;
    }
}
