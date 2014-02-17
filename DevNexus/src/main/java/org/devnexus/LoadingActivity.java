package org.devnexus;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import com.google.gson.Gson;

import org.devnexus.auth.DevNexusAuthenticator;
import org.devnexus.util.AccountUtil;
import org.devnexus.util.GsonUtils;
import org.devnexus.vo.UserCalendar;
import org.devnexus.vo.contract.UserCalendarContract;

import java.io.IOException;
import java.util.ArrayList;

public class LoadingActivity extends Activity {

    public static final int SIGN_IN = 0x100;
    private static final Gson GSON = GsonUtils.GSON;

    public LoadingActivity() {

    }

    private final Observer observer = new Observer(new Handler());

    private AsyncTask<Void, Void, Boolean> userCalendarCheckTask = new SyncCalendarTask();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SIGN_IN) {
            DevnexusApplication.CONTEXT.startUpSync();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Account[] accounts = AccountManager.get(this).getAccountsByType(DevNexusAuthenticator.ACCOUNT_TYPE);
        if (accounts.length == 0) {

            final AccountManagerFuture<Bundle> future = AccountManager.get(this).addAccount(DevNexusAuthenticator.ACCOUNT_TYPE, null, null, null, this, null, null);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Bundle result = future.getResult();
                        DevnexusApplication.CONTEXT.startUpSync();

                    } catch (OperationCanceledException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (AuthenticatorException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } else {
            AccountUtil.setUsername(getApplicationContext(), accounts[0].name);
            DevnexusApplication.CONTEXT.startUpSync();
        }
        getContentResolver().registerContentObserver(UserCalendarContract.URI, false, observer);
        userCalendarCheckTask = new SyncCalendarTask();
        userCalendarCheckTask.executeOnExecutor(DevnexusApplication.EXECUTORS);

    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(observer);
        userCalendarCheckTask.cancel(false);
    }


    @Override
    protected void onStop() {
        super.onStop();

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
                    Cursor cursor = getContentResolver().query(UserCalendarContract.URI, null, null, null, null);
                    ArrayList<UserCalendar> calendar = new ArrayList<UserCalendar>(cursor.getCount());
                    while (cursor.moveToNext()) {
                        calendar.add(GSON.fromJson(cursor.getString(0), UserCalendar.class));
                    }
                    return calendar;
                }

                @Override
                protected void onPostExecute(ArrayList<UserCalendar> userCalendars) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                }
            }.executeOnExecutor(DevnexusApplication.EXECUTORS);
        }
    }

    private class SyncCalendarTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            Cursor cursor = getContentResolver().query(UserCalendarContract.URI, null, null, null, null);
            return cursor != null && cursor.getCount() > 0;
        }

        @Override
        protected void onPostExecute(Boolean hasResults) {
            if (!isCancelled()) {
                if (hasResults) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                } else {

                }
            }
        }
    }

    ;

}
