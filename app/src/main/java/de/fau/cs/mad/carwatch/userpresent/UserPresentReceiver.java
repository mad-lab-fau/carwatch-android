package de.fau.cs.mad.carwatch.userpresent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import org.json.JSONException;
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

        int brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        int nightMode = Settings.Secure.getInt(context.getContentResolver(), Constants.SETTINGS_NIGHT_DISPLAY_ACTIVATED, 0);

        JSONObject json = new JSONObject();
        try {
            json.put(Constants.LOGGER_EXTRA_SCREEN_BRIGHTNESS, brightness);
            json.put(Constants.LOGGER_EXTRA_DISPLAY_NIGHT_MODE, nightMode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (action) {
            case Intent.ACTION_SCREEN_OFF:
                LoggerUtil.log(Constants.LOGGER_ACTION_SCREEN_OFF, json);
                break;
            case Intent.ACTION_SCREEN_ON:
                LoggerUtil.log(Constants.LOGGER_ACTION_SCREEN_ON, json);
                break;
            case Intent.ACTION_USER_PRESENT:
                LoggerUtil.log(Constants.LOGGER_ACTION_USER_PRESENT, json);
                break;
        }
    }
}
