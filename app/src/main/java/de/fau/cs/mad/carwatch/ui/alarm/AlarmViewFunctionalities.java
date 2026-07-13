package de.fau.cs.mad.carwatch.ui.alarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.ui.CarwatchDialogBuilder;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;
import de.fau.cs.mad.carwatch.ui.MainActivity;

public final class AlarmViewFunctionalities {
    private AlarmViewFunctionalities() {
    }

    public static void requestOpenBarcodeScanner(Context context, Alarm alarm) {
        if (context == null || alarm == null)
            return;

        if (requiresWakeupConfirmation(context, alarm)) {
            showWakeupRequiredDialog(context, alarm);
            return;
        }

        if (DateTime.now().isBefore(alarm.getTime()))
            showOpenScannerDialog(context, alarm);
        else
            doOpenScanner(context, alarm);
    }

    private static boolean requiresWakeupConfirmation(Context context, Alarm alarm) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int eveningSampleId = preferences.getInt(Constants.PREF_EVENING_SALIVA_ID, -1);
        boolean exemptSample = alarm.getId() == Constants.EXTRA_ALARM_ID_INITIAL
                || alarm.getId() == Constants.FIRST_SAMPLE_ALARM_ID
                || alarm.getId() == Constants.EXTRA_ALARM_ID_EVENING
                || alarm.getSalivaId() == Constants.EXTRA_SALIVA_ID_INITIAL
                || alarm.getSalivaId() == eveningSampleId;
        if (exemptSample) {
            return false;
        }

        long wakeupMillis = preferences.getLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, 0);
        return wakeupMillis == 0
                || !new DateTime(wakeupMillis).toLocalDate().equals(DateTime.now().toLocalDate());
    }

    private static void showWakeupRequiredDialog(Context context, Alarm alarm) {
        new CarwatchDialogBuilder(context)
                .setIcon(R.drawable.ic_warning_24dp)
                .setTitle(R.string.title_wakeup_not_recorded)
                .setMessage(R.string.message_wakeup_not_recorded)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.button_record_wakeup_now, (dialog, which) -> {
                    Intent intent;
                    if (context instanceof MainActivity mainActivity) {
                        intent = mainActivity.getIntent();
                        addSampleAfterWakeupExtras(intent, alarm);
                        mainActivity.navigate(R.id.navigation_wakeup);
                    } else {
                        intent = new Intent(context, MainActivity.class);
                        intent.putExtra(Constants.EXTRA_TARGET_NAV_ELEMENT, R.id.navigation_wakeup);
                        addSampleAfterWakeupExtras(intent, alarm);
                        context.startActivity(intent);
                    }
                })
                .show();
    }

    private static void addSampleAfterWakeupExtras(Intent intent, Alarm alarm) {
        intent.putExtra(Constants.EXTRA_CONFIRM_WAKEUP_FOR_SAMPLE, true);
        intent.putExtra(Constants.EXTRA_SAMPLE_AFTER_WAKEUP_ALARM_ID, alarm.getId());
        intent.putExtra(Constants.EXTRA_SAMPLE_AFTER_WAKEUP_SALIVA_ID, alarm.getSalivaId());
        intent.putExtra(Constants.EXTRA_SAMPLE_AFTER_WAKEUP_CANCEL_ALARM,
                alarm.getId() != Constants.FIRST_SAMPLE_ALARM_ID);
    }

    private static void showOpenScannerDialog(Context context, Alarm alarm) {
        AlertDialog dialog = new CarwatchDialogBuilder(context)
                .setIcon(R.drawable.ic_warning_24dp)
                .setTitle(R.string.warning_title)
                .setMessage(R.string.open_scanner_before_alarm_message)
                .setNegativeButton(R.string.no, (dialogInterface, i) -> {})
                .setPositiveButton(R.string.yes, (dialogInterface, which) -> doOpenScanner(context, alarm))
                .create();
        dialog.show();
    }

    private static void doOpenScanner(Context context, Alarm alarm) {
        Intent intent = new Intent(context, BarcodeActivity.class);
        int alarmId = alarm.getId() == Constants.FIRST_SAMPLE_ALARM_ID
                ? Constants.EXTRA_ALARM_ID_INITIAL
                : alarm.getId();
        intent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        intent.putExtra(Constants.EXTRA_SALIVA_ID, alarm.getSalivaId());
        intent.putExtra(Constants.EXTRA_CANCEL_ALARM, alarm.getId() != Constants.FIRST_SAMPLE_ALARM_ID);
        context.startActivity(intent);
    }
}
