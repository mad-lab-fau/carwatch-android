package de.fau.cs.mad.carwatch.userpresent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class UserPresentReceiver extends BroadcastReceiver {

    private static final String TAG = UserPresentReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case Intent.ACTION_SCREEN_OFF:
                LoggerUtil.log(Constants.LOGGER_ACTION_SCREEN_OFF, new JSONObject());
                break;
            case Intent.ACTION_SCREEN_ON:
                LoggerUtil.log(Constants.LOGGER_ACTION_SCREEN_ON, new JSONObject());
                break;
            case Intent.ACTION_USER_PRESENT:
                LoggerUtil.log(Constants.LOGGER_ACTION_USER_PRESENT, new JSONObject());
                break;
        }
    }
}
