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
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.util.Utils;

public class Ean8Fragment extends BarcodeFragment implements DialogInterface.OnDismissListener {

    private static final String TAG = Ean8Fragment.class.getSimpleName();

    private int alarmId = Constants.EXTRA_ALARM_ID_MANUAL;
    private int salivaId = Constants.EXTRA_SALIVA_ID_MANUAL;

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

        SharedPreferences sp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext());
        String encodedSalivaTimes = sp.getString(Constants.PREF_SALIVA_DISTANCES, "");
        int lastSalivaSample = Utils.decodeArrayFromString(encodedSalivaTimes).length - 1;

        if (alarmId != Constants.EXTRA_ALARM_ID_INITIAL) {
            // create Json object and log information
            try {
                JSONObject json = new JSONObject();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                int dayId = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 0);
                String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE);
                int startIndex = Integer.parseInt(startSample.substring(1));
                String samplePrefix = startSample.substring(0, 1);

                // human-readable data from scanned barcode
                int scannedDay = Integer.parseInt(barcodeValue.substring(3, 5));
                int scannedSample = Integer.parseInt(barcodeValue.substring(5, 7));
                String hrScannedSample = samplePrefix;
                // evening barcode was scanned
                if (scannedSample == lastSalivaSample + 1){
                    hrScannedSample += Constants.EXTRA_SALIVA_ID_EVENING;
                } else {
                    int hrScannedSampleId = startIndex + scannedSample;
                    hrScannedSample = samplePrefix + hrScannedSampleId;
                }

                // human-readable data from expected sample
                String hrExpectedSample = samplePrefix;
                if (alarmId == Constants.EXTRA_ALARM_ID_EVENING) {
                    hrExpectedSample += Constants.EXTRA_SALIVA_ID_EVENING;
                }else if(alarmId == Constants.EXTRA_ALARM_ID_MANUAL){
                   hrExpectedSample += Constants.EXTRA_SALIVA_ID_MANUAL_HR;
                } else {
                    int hrExpectedSampleId = salivaId + startIndex;
                    hrExpectedSample += hrExpectedSampleId;
                }

                int salivaDayId  = dayId * 100 + salivaId;
                if(alarmId == Constants.EXTRA_ALARM_ID_MANUAL){
                    salivaDayId = Constants.EXTRA_SALIVA_ID_MANUAL;
                }

                json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
                json.put(Constants.LOGGER_EXTRA_SALIVA_ID, salivaDayId);
                json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcodeValue);
                json.put(Constants.LOGGER_EXTRA_SCANNED_DAY, scannedDay);
                json.put(Constants.LOGGER_EXTRA_EXPECTED_DAY, dayId + 1);
                json.put(Constants.LOGGER_EXTRA_SCANNED_SAMPLE, hrScannedSample);
                json.put(Constants.LOGGER_EXTRA_EXPECTED_SAMPLE, hrExpectedSample);
                LoggerUtil.log(Constants.LOGGER_ACTION_BARCODE_SCANNED, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            TimerHandler.cancelTimer(getContext(), alarmId);
            if (alarmId != Constants.EXTRA_ALARM_ID_EVENING && alarmId != Constants.EXTRA_ALARM_ID_MANUAL) {
                if (salivaId != lastSalivaSample) {
                    alarmTime = TimerHandler.scheduleSalivaTimer(getContext(), alarmId, ++salivaId);
                } else {
                    TimerHandler.finishTimer(getContext());
                }
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (salivaId == Constants.EXTRA_SALIVA_ID_INITIAL) {
            finishActivity(this.alarmTime);
        } else if (salivaId == Constants.EXTRA_SALIVA_ID_MANUAL) {
            switchFragment();
        } else {
            finishActivity(this.alarmTime);
        }

    }

    @Override
    public void onChanged(Barcode mlKitBarcode) {
        if (mlKitBarcode != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

            BarcodeField barcode = new BarcodeField(Constants.BARCODE_TYPE_EAN8, mlKitBarcode.getRawValue());
            Set<String> scannedBarcodes = sp.getStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>());

            Log.d(TAG, "Detected Barcode: " + barcode.getValue());
            Log.d(TAG, "Scanned Barcodes: " + scannedBarcodes);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            BarcodeCheckResult check = BarcodeChecker.isValidBarcode(barcode.getValue(), scannedBarcodes, sharedPreferences);

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
                    showBarcodeAlreadyScannedDialog();
                    break;
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

    private void showBarcodeAlreadyScannedDialog() {
        if (getContext() == null) {
            return;
        }

        Drawable icon = getResources().getDrawable(R.drawable.ic_warning_24dp);
        icon.setTint(getResources().getColor(R.color.colorPrimary));

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_barcode_already_scanned)
                .setIcon(icon)
                .setMessage(R.string.message_barcode_already_scanned)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> workflowModel.workflowState.setValue(WorkflowState.DETECTING)).show();
    }

    private void finishActivity(long alarmTime) {
        if (getActivity() != null) {
            Intent intent = new Intent();
            intent.putExtra(Constants.EXTRA_ALARM_TIME, alarmTime);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    private void switchFragment() {
        // called when Fragment is part of MainActivity
        // check if current activity is MainActivity
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.navigate();
        }
    }

}