package de.fau.cs.mad.carwatch.ui.alarm;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;

/**
 * This class contains the functionalities of the alarm view.
 */
public class AlarmViewFunctionalities {
    public static void openScanner(Context context, Alarm alarm) {
        if (context == null || alarm == null)
            return;

        doOpenScanner(context, alarm);
    }

    /**
     * Shows confirmation dialog if the alarm is in the future, otherwise opens the barcode scanner.
     *
     * @param context The context.
     * @param alarm The alarm that belongs to the sample for which the scanner should be opened.
     */
    public static void requestOpenBarcodeScanner(Context context, Alarm alarm) {
        if (context == null || alarm == null)
            return;

        if (DateTime.now().isBefore(alarm.getTime()))
            showOpenScannerDialog(context, alarm);
        else
            doOpenScanner(context, alarm);
    }

    private static void showOpenScannerDialog(Context context, Alarm alarm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder
                .setTitle(R.string.warning_title)
                .setMessage(R.string.open_scanner_before_alarm_message)
                .setNegativeButton(R.string.no, (dialogInterface, i) -> {})
                .setPositiveButton(R.string.yes, (dialogInterface, which) -> doOpenScanner(context, alarm))
                .create();
        dialog.show();
    }

    private static void doOpenScanner(Context context, Alarm alarm) {
        Intent intent = new Intent(context, BarcodeActivity.class);
        intent.putExtra(Constants.EXTRA_ALARM_ID, alarm.getId());
        intent.putExtra(Constants.EXTRA_SALIVA_ID, alarm.getSalivaId());
        context.startActivity(intent);
    }
}
