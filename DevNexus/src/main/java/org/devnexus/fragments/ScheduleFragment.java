package org.devnexus.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.devnexus.DevnexusApplication;
import org.devnexus.adapters.ScheduleAdapter;
import org.devnexus.util.SessionPickerReceiver;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;
import org.jboss.aerogear.android.impl.datamanager.SQLStore;
import org.jboss.aerogear.android.sync.SynchronizeEventListener;
import org.jboss.aerogear.android.sync.TwoWaySqlSynchronizer;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleFragment extends Fragment implements SessionPickerReceiver, SynchronizeEventListener<UserCalendar> {

    private static final String CONTEXT = "org.devnexus.fragments.ScheduleFragment.CONTEXT";
    private static final String TAG = ScheduleFragment.class.getSimpleName();
    private ScheduleAdapter adapter;
    private ListView view;

    private DevnexusApplication application;

    private SQLStore<UserCalendar> calendarStore;
    private TwoWaySqlSynchronizer<UserCalendar> calendarSync;

    public ScheduleFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        application = (DevnexusApplication) activity.getApplication();
        if (adapter == null) {
            adapter = new ScheduleAdapter(new Schedule(), new ArrayList<UserCalendar>(), activity.getApplicationContext());
            adapter.update(application.getCalendar());
            calendarStore = ((DevnexusApplication) getActivity().getApplication()).getCalendarStore();
            calendarSync = ((DevnexusApplication) getActivity().getApplication()).getUserCalendarSync();
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
        calendarSync.addListener(this);
        adapter.update(application.getCalendar());
    }

    @Override
    public void onPause() {
        super.onPause();
        calendarSync.removeListener(this);
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

                    if (item.item != null) {
                        SessionDetailFragment sessionDetailFragment = SessionDetailFragment.newInstance(item, item.item);
                        sessionDetailFragment.setReceiver(ScheduleFragment.this);
                        sessionDetailFragment.show(getActivity().getSupportFragmentManager(), TAG);
                    } else {
                        SessionPickerFragment sessionPicker = SessionPickerFragment.newInstance(item);
                        sessionPicker.setReceiver(ScheduleFragment.this);
                        sessionPicker.show(getActivity().getSupportFragmentManager(), TAG);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void receiveSessionItem(UserCalendar calendarItem, ScheduleItem session) {

        calendarItem.item = session;
        calendarStore.remove(calendarItem.getId());
        calendarStore.save(calendarItem);

        adapter.update(new ArrayList<UserCalendar>(calendarStore.readAll()));
        adapter.notifyDataSetChanged();
        calendarSync.sync();

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

    public static Fragment newInstance() {
        return new ScheduleFragment();
    }
}
