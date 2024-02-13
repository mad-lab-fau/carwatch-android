package de.fau.cs.mad.carwatch.userpresent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class AppUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = AppUpdateReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED))
            return;

        Log.d(TAG, "App update complete");
        try {
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_CODE, BuildConfig.VERSION_CODE);
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_NAME, BuildConfig.VERSION_NAME);
            LoggerUtil.log(Constants.LOGGER_APP_UPDATE_COMPLETE, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        BootService.enqueueWork(context);
    }
}
