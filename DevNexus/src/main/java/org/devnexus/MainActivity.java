package org.devnexus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.plus.PlusClient;

import org.devnexus.auth.GooglePlusAuthenticationModule;
import org.devnexus.fragments.CountDownFragment;
import org.devnexus.fragments.ScheduleFragment;
import org.devnexus.fragments.TracksFragment;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.http.HeaderAndBody;

import java.util.HashMap;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener,
                                                               ViewPager.OnPageChangeListener,
                                                               View.OnClickListener,
                                                               GooglePlayServicesClient.ConnectionCallbacks,
                                                               GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;

    private static final String SCOPES = "https://www.googleapis.com/auth/plus.login "
            + "https://www.googleapis.com/auth/userinfo.email "
            + "https://www.googleapis.com/auth/userinfo.profile";
    private static final Integer REQUEST_AUTHORIZATION = 8000;

    private ProgressDialog mConnectionProgressDialog;
    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        mViewPager = (ViewPager) findViewById(R.id.pager);
        String homeScreenLabel;

        // Phone setup
        mViewPager.setAdapter(new HomePagerAdapter(getSupportFragmentManager()));
        mViewPager.setOnPageChangeListener(this);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.addTab(actionBar.newTab()
                .setText(R.string.title_schedule)
                .setTabListener(this));
        actionBar.addTab(actionBar.newTab()
                .setText(R.string.title_tracks)
                .setTabListener(this));
        actionBar.addTab(actionBar.newTab()
                .setText(R.string.title_social)
                .setTabListener(this));

        mPlusClient = new PlusClient.Builder(this, this, this)
                .setScopes(SCOPES)
                .build();
        // Progress bar to be displayed if the connection failure is not resolved.
        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage("Signing in...");




    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlusClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlusClient.disconnect();
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);

        int titleId = -1;
        switch (position) {
            case 0:
                titleId = R.string.title_schedule;
                break;
            case 1:
                titleId = R.string.title_tracks;
                break;
            case 2:
                titleId = R.string.title_social;
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    private class HomePagerAdapter extends FragmentPagerAdapter {
        public HomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new CountDownFragment();

                case 1:
                    return new TracksFragment();

                case 2:
                    return new ScheduleFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mConnectionProgressDialog.isShowing()) {
            // The user clicked the sign-in button already. Start to resolve
            // connection errors. Wait until onConnected() to dismiss the
            // connection dialog.
            if (result.hasResolution()) {
                try {
                    result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                } catch (IntentSender.SendIntentException e) {
                    mPlusClient.connect();
                }
            }
        }
        // Save the result and resolve the connection failure upon a user click.
        mConnectionResult = result;
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == REQUEST_CODE_RESOLVE_ERR && responseCode == RESULT_OK) {
            mConnectionResult = null;
            mPlusClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        String accountName = mPlusClient.getAccountName();

        Toast.makeText(this, accountName + " is connected.", Toast.LENGTH_LONG).show();


        AuthenticationModule module = ((DevnexusApplication) getApplication()).getAuth(this);
        HashMap<String, String> loginParams = new HashMap<String, String>();
        loginParams.put(GooglePlusAuthenticationModule.ACCOUNT_NAME, accountName);
        loginParams.put(GooglePlusAuthenticationModule.ACCOUNT_ID, "");
        module.login(loginParams, new Callback<HeaderAndBody>() {
            @Override
            public void onSuccess(HeaderAndBody data) {
                Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_LONG).show();
            }
        });


    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "disconnected");
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sign_up && !mPlusClient.isConnected()) {
            if (mConnectionResult == null) {
                mConnectionProgressDialog.show();
            } else {
                try {
                    mConnectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
                } catch (IntentSender.SendIntentException e) {
                    // Try connecting again.
                    mConnectionResult = null;
                    mPlusClient.connect();
                }
            }
        }

    }
}