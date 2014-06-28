package org.devnexus.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.devnexus.R;
import org.devnexus.adapters.PresentationAdapter;
import org.devnexus.vo.ScheduleItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by summers on 6/23/14.
 */
public class PresentationFragment extends Fragment {

    private static final String ITEMS_KEY = "PresentationFragment.items";
    private ListView gridView;
    private View view;
    private ArrayList<ScheduleItem> items;
    private PresentationAdapter adapter;

    public PresentationFragment() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        //this.items = getArguments().getParcelableArrayList(ITEMS_KEY);
        if (this.items == null) {
            this.items =  new ArrayList<ScheduleItem>();
        }

        this.adapter = new PresentationAdapter(getActivity().getApplicationContext(), R.layout.presentation_list_item, items);

        this.view = inflater.inflate(R.layout.presentations, null);
        this.gridView = (ListView) view.findViewById(R.id.grid);
        gridView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public static PresentationFragment newInstance(List<ScheduleItem> items) {
        Bundle arguments = new Bundle();
        PresentationFragment fragment = new PresentationFragment();
        arguments.putParcelableArrayList(ITEMS_KEY, new ArrayList<Parcelable>(items));
        fragment.setArguments(arguments);
        fragment.items = new ArrayList<ScheduleItem>(items);
        return fragment;
    }

    public void updateItems(List<ScheduleItem> scheduleItems) {
        this.items = new ArrayList<ScheduleItem>(scheduleItems);
        this.adapter.clear();
        this.adapter.addAll(items);
        this.adapter.notifyDataSetChanged();
    }
}
