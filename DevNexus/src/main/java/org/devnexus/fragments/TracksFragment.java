package org.devnexus.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.devnexus.GalleriaMapActivity;

/**
 * Created by summers on 11/13/13.
 */
public class TracksFragment extends Fragment {

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Button button = new Button(inflater.getContext());
        button.setText("Show Map");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(inflater.getContext(), GalleriaMapActivity.class);
                startActivity(i);
            }
        });
        return button;
    }
}
