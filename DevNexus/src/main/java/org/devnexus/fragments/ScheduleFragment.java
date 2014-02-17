package org.devnexus.fragments;

import android.accounts.Account;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;

import org.devnexus.DevnexusApplication;
import org.devnexus.adapters.ScheduleAdapter;
import org.devnexus.aerogear.SynchronizeEventListener;
import org.devnexus.auth.DevNexusAuthenticator;
import org.devnexus.sync.DevNexusSyncAdapter;
import org.devnexus.util.AccountUtil;
import org.devnexus.util.GsonUtils;
import org.devnexus.util.SessionPickerReceiver;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.contract.UserCalendarContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleFragment extends Fragment implements SessionPickerReceiver, SynchronizeEventListener<UserCalendar> {

    private static final String CONTEXT = "org.devnexus.fragments.ScheduleFragment.CONTEXT";
    private static final Gson GSON = GsonUtils.GSON;
    private static final String TAG = ScheduleFragment.class.getSimpleName();
    private ScheduleAdapter adapter;
    private ListView view;
    private final Observer observer = new Observer(new Handler());

    private DevnexusApplication application;

    private List<UserCalendar> calendar = new ArrayList<UserCalendar>();
    private AsyncTask<Void, Void, List<UserCalendar>> calendarLoaderTask;

    private final Receiver receiver;
    private ContentResolver resolver;

    public ScheduleFragment() {
        receiver = new Receiver();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        application = (DevnexusApplication) activity.getApplication();
        if (adapter == null) {
            adapter = new ScheduleAdapter(new Schedule(), new ArrayList<UserCalendar>(), activity.getApplicationContext());
        }
        resolver = getActivity().getContentResolver();

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
        getActivity().registerReceiver(receiver, new IntentFilter(DevNexusSyncAdapter.CALENDAR_SYNC_FINISH));
        resolver.registerContentObserver(UserCalendarContract.URI, false, observer);

        calendarLoaderTask = new AsyncTask<Void, Void, List<UserCalendar>>() {
            @Override
            protected synchronized List<UserCalendar> doInBackground(Void... params) {

                List<UserCalendar> toReturn = new ArrayList<UserCalendar>();
                Cursor cursor = null;
                try {
                    cursor = DevnexusApplication.CONTEXT.getContentResolver().query(UserCalendarContract.URI, null, null, null, null);

                    while (cursor != null && cursor.moveToNext()) {
                    toReturn.add(GSON.fromJson(cursor.getString(0), UserCalendar.class));
                }

                return toReturn;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }

            @Override
            protected synchronized void onPostExecute(List<UserCalendar> userCalendars) {
                if (!super.isCancelled()) {
                    dataUpdated(userCalendars);
                }
            }
        };

        calendarLoaderTask.executeOnExecutor(DevnexusApplication.EXECUTORS);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
        calendarLoaderTask.cancel(false);
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
    public void receiveSessionItem(final UserCalendar calendarItem, final ScheduleItem session) {

        calendarItem.item = session;
        List<UserCalendar> appCalendar = calendar;
        for (UserCalendar cal : appCalendar) {
            if (cal.getId().equals(calendarItem.getId())) {
                cal.item = calendarItem.item;
                adapter.update(appCalendar);
                break;
            }
        }

        new Updater(resolver, calendarItem).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    @Override
    public synchronized void dataUpdated(Collection<UserCalendar> newData) {
        calendar.clear();
        calendar.addAll(newData);
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

    private final class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //dataUpdated((List<UserCalendar>) intent.getSerializableExtra(DevNexusSyncAdapter.CALENDAR_DATA));
        }
    }

    private final class Observer extends ContentObserver {
        public Observer(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            new AsyncTask<Void, Void, ArrayList<UserCalendar>>() {
                @Override
                protected ArrayList<UserCalendar> doInBackground(Void... params) {
                    Cursor cursor = null;
                    try {
                        cursor = resolver.query(UserCalendarContract.URI, null, null, null, null);
                        ArrayList<UserCalendar> calendar = new ArrayList<UserCalendar>(cursor.getCount());
                        while (cursor != null && cursor.moveToNext()) {
                            calendar.add(GSON.fromJson(cursor.getString(0), UserCalendar.class));
                    }
                    return calendar;
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }

                @Override
                protected void onPostExecute(ArrayList<UserCalendar> userCalendars) {
                    dataUpdated(userCalendars);
                }
            }.executeOnExecutor(DevnexusApplication.EXECUTORS);
        }
    }

    private static class Updater extends AsyncTask<Void, Void, Void> {

        private final ContentResolver resolver;
        private final UserCalendar calendarItem;

        private Updater(ContentResolver resolver, UserCalendar calendarItem) {
            this.resolver = resolver;
            this.calendarItem = calendarItem;
        }

        @Override
        protected Void doInBackground(Void... params) {
            resolver.update(UserCalendarContract.URI, UserCalendarContract.valueize(calendarItem, true), null, new String[]{calendarItem.getId() + ""});
            Bundle settingsBundle = new Bundle();

            settingsBundle.putBoolean(
                    ContentResolver.SYNC_EXTRAS_EXPEDITED, true);


            ContentResolver.requestSync(new Account(AccountUtil.getUsername(DevnexusApplication.CONTEXT), DevNexusAuthenticator.ACCOUNT_TYPE),
                    "org.devnexus.sync", settingsBundle);
            return null;
        }
    }

}
