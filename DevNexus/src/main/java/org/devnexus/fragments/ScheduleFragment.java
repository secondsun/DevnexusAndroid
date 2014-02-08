package org.devnexus.fragments;

import android.accounts.Account;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.devnexus.DevnexusApplication;
import org.devnexus.adapters.ScheduleAdapter;
import org.devnexus.aerogear.SynchronizeEventListener;
import org.devnexus.auth.DevNexusAuthenticator;
import org.devnexus.sync.DevNexusSyncAdapter;
import org.devnexus.util.AccountUtil;
import org.devnexus.util.SessionPickerReceiver;
import org.devnexus.vo.Schedule;
import org.devnexus.vo.ScheduleItem;
import org.devnexus.vo.UserCalendar;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by summers on 11/13/13.
 */
public class ScheduleFragment extends Fragment implements SessionPickerReceiver, SynchronizeEventListener<UserCalendar> {

    private static final String CONTEXT = "org.devnexus.fragments.ScheduleFragment.CONTEXT";
    private static final String TAG = ScheduleFragment.class.getSimpleName();
    private ScheduleAdapter adapter;
    private ListView view;

    private static final Gson GSON;

    static {

        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(Date.class, new JsonDeserializer() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return new Date(json.getAsJsonPrimitive().getAsLong());
            }
        });

        builder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                    context) {
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        });

        GSON = builder.create();
    }

    private DevnexusApplication application;

    private final Receiver receiver;

    public ScheduleFragment() {
        receiver = new Receiver();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        application = (DevnexusApplication) activity.getApplication();
        if (adapter == null) {
            adapter = new ScheduleAdapter(new Schedule(), new ArrayList<UserCalendar>(), activity.getApplicationContext());
            adapter.update(application.getCalendar());

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
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(receiver, new IntentFilter(DevNexusSyncAdapter.CALENDAR_SYNC_FINISH));
        dataUpdated(application.getCalendar());
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiver);
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
        List<UserCalendar> appCalendar = application.getCalendar();
        for (UserCalendar cal : appCalendar) {
            if (cal.getId().equals(calendarItem.getId())) {
                cal.item = calendarItem.item;
                adapter.update(appCalendar);
                break;
            }
        }

        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        settingsBundle.putString(
                DevNexusSyncAdapter.CALENDAR_DATA, GSON.toJson(calendarItem));


        ContentResolver.requestSync(new Account(AccountUtil.getUsername(DevnexusApplication.CONTEXT), DevNexusAuthenticator.ACCOUNT_TYPE),
                "org.devnexus.sync", settingsBundle);

    }

    @Override
    public void dataUpdated(Collection<UserCalendar> newData) {
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
            dataUpdated((List<UserCalendar>) intent.getSerializableExtra(DevNexusSyncAdapter.CALENDAR_DATA));
        }
    }

}
