package org.devnexus;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import org.devnexus.auth.DevNexusAuthenticator;
import org.devnexus.sync.DevNexusSyncAdapter;
import org.devnexus.util.AccountUtil;

import java.io.IOException;

public class LoadingActivity extends Activity {

    public static final int SIGN_IN = 0x100;

    private final BroadcastReceiver receiver;

    public LoadingActivity() {
        receiver = new Receiver(this);
    }

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
        registerReceiver(receiver, new IntentFilter(DevNexusSyncAdapter.CALENDAR_SYNC_FINISH));

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

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    @Override
    protected void onStop() {
        super.onStop();

    }

    private static class Receiver extends BroadcastReceiver {

        private final LoadingActivity activity;

        private Receiver(LoadingActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            activity.startActivity(new Intent(activity, MainActivity.class));
            activity.finish();
        }
    }

}
