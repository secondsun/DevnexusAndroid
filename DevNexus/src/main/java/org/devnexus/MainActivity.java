package org.devnexus;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.internal.is;

import org.devnexus.fragments.AboutFragment;
import org.devnexus.fragments.GalleriaMapFragment;
import org.devnexus.fragments.PresentationFragment;
import org.devnexus.fragments.ScheduleFragment;
import org.devnexus.fragments.SettingsFragment;
import org.devnexus.util.GsonUtils;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.contract.ScheduleContract;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity implements
        AdapterView.OnItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] NAVIGATION = new String[]{"Presentations", "Settings", "About"};

    private ListView drawerList;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private String drawerTitle = "Schedule";
    private List<ScheduleItem> scheduleItems = new ArrayList<ScheduleItem>();
    private PresentationFragment presentationFragment;
    private SessionLoader loaderTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.loaderTask = new SessionLoader();
        loaderTask.executeOnExecutor(DevnexusApplication.EXECUTORS);
        showPager();

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


        drawerTitle = getTitle().toString();
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
                ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
                switch (position) {
                    case 0:
                        icon.setImageResource(android.R.drawable.ic_menu_agenda);
                        break;
                    case 1:
                        icon.setImageResource(android.R.drawable.ic_menu_manage);
                        break;
                    case 2:
                        icon.setImageResource(android.R.drawable.ic_menu_info_details);
                        break;
                }
                ((TextView) convertView.findViewById(R.id.name)).setText(getItem(position));

                return convertView;
            }
        });
        // Set the list's click listener
        drawerList.setOnItemClickListener(this);
        drawerList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_drawer, R.string.open_drawer, R.string.close_drawer) {


            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(getTitle());
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        drawerList.setItemChecked(0, true);


        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle != null) {
            if (drawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Create a new fragment and specify the planet to show based on position
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getFragments().get(0) instanceof SettingsFragment && position == 1) {
            drawerLayout.closeDrawer(drawerList);
            return;
        } else if (fragmentManager.getFragments().get(0) instanceof PresentationFragment && position == 0) {
            drawerLayout.closeDrawer(drawerList);
            return;
        } else if (fragmentManager.getFragments().get(0) instanceof AboutFragment && position == 2) {
            drawerLayout.closeDrawer(drawerList);
            return;
        }

        Fragment fragment = getItem(position);
        Bundle args = new Bundle();
        fragment.setArguments(args);

        // Insert the fragment by replacing any existing fragment

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
                return (presentationFragment = PresentationFragment.newInstance(scheduleItems));
            case 1:
                return new SettingsFragment();
            case 2:
                return new AboutFragment();
        }
        return null;
    }

    public void updateScheduleItems() {
        if (presentationFragment != null) {
            presentationFragment.updateItems(scheduleItems);
        }
    }

    private class SessionLoader extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... params) {


            Cursor cursor = null;
            scheduleItems = new ArrayList<ScheduleItem>(10);
            try {
                cursor = getContentResolver().query(ScheduleContract.URI, null, null, null, null);

                if (cursor != null && cursor.moveToNext()) {
                    Schedule scheduleFromDb = GsonUtils.GSON.fromJson(cursor.getString(0), Schedule.class);
                    for (ScheduleItem scheduleItem : scheduleFromDb.scheduleItemList.scheduleItems) {
                        Log.d("Room", scheduleItem.room.name);
                        scheduleItems.add(scheduleItem);
                    }
                } else {
                    //???
                }

            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (!isCancelled()) updateScheduleItems();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.loaderTask.cancel(true);
    }
}