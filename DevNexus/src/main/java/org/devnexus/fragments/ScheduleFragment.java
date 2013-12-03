package org.devnexus.fragments;

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

    private ScheduleAdapter adapter;

    public ScheduleFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }

    public void onStart(){
        super.onStart();
        ((DevnexusApplication)getActivity().getApplication()).getSchedule(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        adapter = new ScheduleAdapter(new Schedule(), new UserCalendarList(), inflater.getContext());

        ListView view = new ListView(inflater.getContext());

        view.setAdapter(adapter);

        return view;
    }

}
