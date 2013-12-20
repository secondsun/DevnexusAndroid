package org.devnexus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.plus.PlusClient;

import org.devnexus.auth.GooglePlusAuthenticationModule;
import org.devnexus.fragments.CountDownFragment;
import org.devnexus.fragments.ScheduleFragment;
import org.devnexus.fragments.TracksFragment;
import org.devnexus.service.ScheduleSyncService;
import org.devnexus.util.AccountUtil;
import org.jboss.aerogear.android.Callback;
import org.jboss.aerogear.android.authentication.AuthenticationModule;
import org.jboss.aerogear.android.http.HeaderAndBody;

import java.util.HashMap;

public class MainActivity extends ActionBarActivity implements
        View.OnClickListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        AdapterView.OnItemClickListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
    private static final int REQUEST_CODE_RESOLVE_GPS = 10000;

    private static final String SCOPES = "https://www.googleapis.com/auth/plus.login "
            + "https://www.googleapis.com/auth/userinfo.email "
            + "https://www.googleapis.com/auth/userinfo.profile";
    private static final Integer REQUEST_AUTHORIZATION = 8000;
    private static final String[] NAVIGATION = new String[]{"Schedule", "Map", "Social"};

    private ProgressDialog mConnectionProgressDialog;
    private PlusClient mPlusClient;
    private ConnectionResult mConnectionResult;
    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private String drawerTitle = "Schedule";
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mPlusClient = new PlusClient.Builder(this, this, this)
                .setScopes(SCOPES)
                .build();
        // Progress bar to be displayed if the connection failure is not resolved.
        mConnectionProgressDialog = new ProgressDialog(this);
        mConnectionProgressDialog.setMessage("Signing in...");

        if (AccountUtil.hasConnected(getApplicationContext())) {
            showPager();
        } else {
            showSignIn();
        }

    }

    private void showSignIn() {
        setContentView(R.layout.sign_in_layout);
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                GooglePlayServicesUtil.getErrorDialog(status, this, REQUEST_CODE_RESOLVE_ERR).show();
            }
        }

    }

    private void showPager() {

        setContentView(R.layout.activity_main);

        showDrawer();

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.root_fragment, ScheduleFragment.newInstance())
                .commit();

    }

    private void showDrawer() {
        setContentView(R.layout.activity_main);


        title = drawerTitle = getTitle().toString();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        drawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.navigation_drawer_item, NAVIGATION) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.navigation_drawer_item, null);
                }

                ((TextView) convertView.findViewById(R.id.name)).setText(getItem(position));

                return convertView;
            }
        });
        // Set the list's click listener
        drawerList.setOnItemClickListener(this);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.open_drawer, R.string.close_drawer) {


            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        drawerList.setItemChecked(0, true);


        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);


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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Create a new fragment and specify the planet to show based on position
        Fragment fragment = getItem(position);
        Bundle args = new Bundle();
        fragment.setArguments(args);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.root_fragment, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        drawerList.setItemChecked(position, true);
        setTitle(NAVIGATION[position]);

        drawerLayout.closeDrawer(drawerList);

    }

    private Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ScheduleFragment();
            case 1:
                return new TracksFragment();
            case 2:
                return new CountDownFragment();
        }
        return null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        AccountUtil.setConnected(getApplicationContext(), false);
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
        } else if (requestCode == REQUEST_CODE_RESOLVE_GPS && responseCode == RESULT_OK) {

        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        final String accountName = mPlusClient.getAccountName();

        AuthenticationModule module = ((DevnexusApplication) getApplication()).getAuth(this);
        HashMap<String, String> loginParams = new HashMap<String, String>();
        loginParams.put(GooglePlusAuthenticationModule.ACCOUNT_NAME, accountName);
        loginParams.put(GooglePlusAuthenticationModule.ACCOUNT_ID, "");
        if (!module.isLoggedIn()) {
            module.login(loginParams, new Callback<HeaderAndBody>() {
                @Override
                public void onSuccess(HeaderAndBody data) {
                    if (!AccountUtil.hasConnected(getApplicationContext())) {
                        AccountUtil.setConnected(getApplicationContext(), true);
                        AccountUtil.setUsername(getApplicationContext(), accountName);

                        Intent syncConfigIntent = new Intent(getApplicationContext(), ScheduleSyncService.class);
                        startService(syncConfigIntent);

                        ((DevnexusApplication) getApplication()).registerForPush(accountName);

                        Intent i = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(i);


                        finish();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "disconnected");
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sign_in_button && !mPlusClient.isConnected()) {
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