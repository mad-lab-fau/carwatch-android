package de.fau.cs.mad.carwatch.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class AlarmService extends JobIntentService {

    private Handler handler = new Handler();

    public static final int JOB_ID = 0xF00D;

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    /*public static final String ACTION_FOO = "de.fau.cs.mad.carwatch.service.action.FOO";
    public static final String ACTION_BAZ = "de.fau.cs.mad.carwatch.service.action.BAZ";

    public static final String EXTRA_PARAM1 = "de.fau.cs.mad.carwatch.service.extra.PARAM1";
    public static final String EXTRA_PARAM2 = "de.fau.cs.mad.carwatch.service.extra.PARAM2";*/

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, AlarmService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AlarmService.this, "WAKE UP!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
