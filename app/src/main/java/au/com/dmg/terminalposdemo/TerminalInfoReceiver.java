package au.com.dmg.terminalposdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import au.com.dmg.fusion.Message;
import au.com.dmg.fusion.response.terminalinformationresponse.TerminalInformationResponse;

public class TerminalInfoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TerminalPOSDemo", "Broadcast Received...");
    }
}
