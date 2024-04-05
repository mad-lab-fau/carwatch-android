package de.fau.cs.mad.carwatch.userpresent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.logger.MetadataLogger;
import de.fau.cs.mad.carwatch.ui.MainActivity;

/**
 * Receiver for the MY_PACKAGE_REPLACED intent, which is sent after an app update.
 */
public class MyPackageReplacedReceiver extends BroadcastReceiver {
    private static final String TAG = MyPackageReplacedReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))
            return;

        MainActivity.initializeLoggingUtil(context);

        Log.d(TAG, "App update to version " + BuildConfig.VERSION_NAME + " completed");
        LoggerUtil.log(Constants.LOGGER_APP_UPDATE_COMPLETE, "");
        MetadataLogger.logAppMetadata();
        BootService.enqueueWork(context);
    }
}
