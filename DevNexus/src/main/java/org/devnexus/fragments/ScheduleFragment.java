package org.devnexus.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.devnexus.DevnexusApplication;
import org.devnexus.adapters.ScheduleAdapter;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.UserCalendarList;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleFragment extends Fragment {

    private static final String CONTEXT = "org.devnexus.fragments.ScheduleFragment.CONTEXT";
    private ScheduleAdapter adapter;
    private ListView view;

    public ScheduleFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (adapter == null) {
            adapter = new ScheduleAdapter(new Schedule(), new UserCalendarList(), activity.getApplicationContext());
            ((DevnexusApplication)getActivity().getApplication()).getSchedule(adapter, this);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            view = new ListView(inflater.getContext());
            view.setAdapter(adapter);
        return view;
    }

}
