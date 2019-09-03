package de.fau.cs.mad.carwatch.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //enqueueWork will pass service/work intent in the JobIntentService class
        AlarmService.enqueueWork(context, intent);
    }
}
