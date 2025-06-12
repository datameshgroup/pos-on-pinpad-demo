package au.com.dmg.terminalposdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SyncReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Sync Receiver", "Broadcast Received..." + intent.getStringExtra("message"));
    }
}
