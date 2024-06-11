package de.fau.cs.mad.carwatch.userpresent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UserPresentStopReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        UserPresentService.stopService(context);
    }
}
