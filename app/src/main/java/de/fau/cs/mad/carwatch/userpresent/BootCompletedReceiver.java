package de.fau.cs.mad.carwatch.userpresent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.Logger;

import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = BootCompletedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (MainActivity.sAdapter == null) {
            MainActivity.sAdapter = new DiskLogAdapter(LoggerUtil.getFormatStrategy(context)) {
                @Override
                public boolean isLoggable(int priority, @Nullable String tag) {
                    return true;
                }
            };
            Logger.addLogAdapter(MainActivity.sAdapter);
        }

        LoggerUtil.log(Constants.LOGGER_ACTION_PHONE_BOOT_INIT, new JSONObject());

        switch (intent.getAction()) {
            case "android.intent.action.BOOT_COMPLETED":
            case "android.intent.action.QUICKBOOT_POWERON":
                BootService.enqueueWork(context);
                break;
        }
    }
}
