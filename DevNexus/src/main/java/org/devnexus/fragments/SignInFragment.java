package org.devnexus.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.SignInButton;

import org.devnexus.R;

/**
 * Created by summers on 12/9/13.
 */
public class SignInFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_in_fragment, null);
        SignInButton signinButtonView = (SignInButton) view.findViewById(R.id.sign_in_button);
        signinButtonView.setSize(SignInButton.SIZE_WIDE);
        return view;
    }
}
