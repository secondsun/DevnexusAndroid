package org.devnexus.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.devnexus.R;

import java.util.Calendar;

public class CountDownFragment extends Fragment {

    private final Calendar startDate = Calendar.getInstance();
    final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;


    private final int daysUntil;

    {
        startDate.set(Calendar.YEAR, 2014);
        startDate.set(Calendar.DATE, 17);
        startDate.set(Calendar.MONTH, Calendar.FEBRUARY);

        daysUntil = (int) ((startDate.getTime().getTime() - Calendar.getInstance().getTime().getTime())/ DAY_IN_MILLIS );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.countdown, null);
        TextView countdown = (TextView) view.findViewById(R.id.countdown);
        String text = "Too long...";
        if (daysUntil > 0) {
            text = String.format("%d days.", daysUntil);
        } else if (daysUntil< -1) {
            text="Right now!";
        }
        countdown.setText(text);
        return view;
    }
}
