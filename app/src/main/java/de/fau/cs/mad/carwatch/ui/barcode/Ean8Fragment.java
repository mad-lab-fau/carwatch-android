package de.fau.cs.mad.carwatch.ui.barcode;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult;
import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArraySet;

import com.google.mlkit.vision.barcode.common.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeField;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeProcessor;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeResultFragment;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class Ean8Fragment extends BarcodeFragment implements DialogInterface.OnDismissListener {

    private static final String TAG = Ean8Fragment.class.getSimpleName();

    private int alarmId = Constants.EXTRA_ALARM_ID_INITIAL;
    private int salivaId = Constants.EXTRA_SALIVA_ID_INITIAL;

    private long alarmTime = 0;


    @Override
    public void onResume() {
        super.onResume();
        cameraSource.setFrameProcessor(new BarcodeProcessor(graphicOverlay, workflowModel, Barcode.FORMAT_EAN_8));
        workflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public void setSalivaId(int salivaId) {
        this.salivaId = salivaId;
    }

    private void cancelTimer(int alarmId, int salivaId, String barcodeValue) {
        if (getContext() == null) {
            return;
        }

        if (alarmId != Constants.EXTRA_ALARM_ID_INITIAL) {
            // create Json object and log information
            try {
                JSONObject json = new JSONObject();
                int dayId = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(Constants.PREF_DAY_COUNTER, 0);
                json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
                json.put(Constants.LOGGER_EXTRA_SALIVA_ID, dayId * 100 + salivaId);
                json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcodeValue);
                LoggerUtil.log(Constants.LOGGER_ACTION_BARCODE_SCANNED, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            TimerHandler.cancelTimer(getContext(), alarmId);
            if (alarmId != Constants.EXTRA_ALARM_ID_EVENING) {
                if (salivaId != Constants.SALIVA_TIMES.length - 1) {
                    alarmTime = TimerHandler.scheduleSalivaTimer(getContext(), alarmId, ++salivaId);
                } else {
                    TimerHandler.finishTimer(getContext());
                }
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (salivaId == 0) {
            // Show Reminder Dialog when scanning first saliva sample of the day
            showQuestionnaireReminderDialog();
        } else {
            finishActivity(this.alarmTime);
        }

    }

    @Override
    public void onChanged(Barcode mlKitBarcode) {
        if (mlKitBarcode != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

            BarcodeField barcode = new BarcodeField("Barcode", mlKitBarcode.getRawValue());
            Set<String> scannedBarcodes = sp.getStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>());

            Log.d(TAG, "Detected Barcode: " + barcode.getValue());
            Log.d(TAG, "Scanned Barcodes: " + scannedBarcodes);

            BarcodeCheckResult check = BarcodeChecker.isValidBarcode(barcode.getValue(), scannedBarcodes);

            Log.d(TAG, "Barcode scan: " + check);

            switch (check) {
                case DUPLICATE_BARCODE:
                    try {
                        JSONObject json = new JSONObject();
                        json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcode.getValue());
                        json.put(Constants.LOGGER_EXTRA_OTHER_BARCODES, scannedBarcodes);
                        LoggerUtil.log(Constants.LOGGER_ACTION_DUPLICATE_BARCODE_SCANNED, json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //showBarcodeAlreadyScannedDialog();
                    // call through
                case VALID:
                    scannedBarcodes.add(barcode.getValue());
                    sp.edit().putStringSet(Constants.PREF_SCANNED_BARCODES, scannedBarcodes).apply();

                    cancelTimer(alarmId, salivaId, barcode.getValue());
                    BarcodeResultFragment.show(getChildFragmentManager(), barcode, this);
                    break;
                case INVALID:
                    try {
                        JSONObject json = new JSONObject();
                        json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcode.getValue());
                        LoggerUtil.log(Constants.LOGGER_ACTION_INVALID_BARCODE_SCANNED, json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showInvalidBarcodeDialog();
                    break;
            }
        }
    }

    @Override
    protected void showInvalidBarcodeDialog() {
        if (getContext() == null) {
            return;
        }

        Drawable icon = getResources().getDrawable(R.drawable.ic_warning_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_barcode_invalid)
                .setIcon(icon)
                .setMessage(R.string.message_barcode_invalid)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> workflowModel.workflowState.setValue(WorkflowState.DETECTING)).show();
    }

    private void showQuestionnaireReminderDialog() {
        if (getContext() == null) {
            return;
        }

        Drawable icon = getResources().getDrawable(R.drawable.ic_help_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_reminder_questionnaire)
                .setMessage(R.string.message_reminder_questionnaire)
                .setIcon(icon)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> finishActivity(this.alarmTime)).show();
    }

    private void finishActivity(long alarmTime) {
        if (getActivity() != null) {
            Intent intent = new Intent();
            intent.putExtra(Constants.EXTRA_ALARM_TIME, alarmTime);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

}