package de.fau.cs.mad.carwatch.ui.barcode;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult;
import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;

import de.fau.cs.mad.carwatch.ui.CarwatchDialogBuilder;
import androidx.collection.ArraySet;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.mlkit.vision.barcode.common.Barcode;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeField;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeProcessor;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class Ean8Fragment extends BarcodeFragment {

    private static final String TAG = Ean8Fragment.class.getSimpleName();

    private int alarmId = Constants.EXTRA_ALARM_ID_MANUAL;
    private int salivaId = Constants.EXTRA_SALIVA_ID_MANUAL;
    private boolean cancelAlarmAfterScan = true;
    private String endOfDayAlertType;

    @Override
    public void onResume() {
        super.onResume();
        cameraSource.setFrameProcessor(new BarcodeProcessor(graphicOverlay, workflowModel, Barcode.FORMAT_EAN_8));
        workflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    @Override
    public void onChanged(Barcode mlKitBarcode) {
        if (mlKitBarcode != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

            BarcodeField barcode = new BarcodeField(Constants.BARCODE_TYPE_EAN8, mlKitBarcode.getRawValue());
            Set<String> scannedBarcodes = new ArraySet<>(sharedPreferences.getStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>()));

            Log.d(TAG, "Detected Barcode: " + barcode.getValue());
            Log.d(TAG, "Scanned Barcodes: " + scannedBarcodes);

            BarcodeCheckResult check = BarcodeChecker.isValidBarcode(barcode.getValue(), sharedPreferences);

            Log.d(TAG, "Barcode scan: " + check);

            switch (check) {
                case DUPLICATE_BARCODE:
                    try {
                        JSONObject json = new JSONObject();
                        json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcode.getValue());
                        json.put(Constants.LOGGER_EXTRA_OTHER_BARCODES, scannedBarcodes);
                        LoggerUtil.log(Constants.LOGGER_ACTION_DUPLICATE_BARCODE_SCANNED, json);
                    } catch (JSONException e) {
                        Log.e(TAG, "Could not log duplicate barcode scan", e);
                    }
                    showBarcodeAlreadyScannedDialog();
                    break;
                case VALID:
                    if (shouldEnforceExpectedBarcodeId(sharedPreferences)
                            && !matchesExpectedSample(barcode.getValue(), sharedPreferences)) {
                        logInvalidBarcode(barcode.getValue());
                        showInvalidBarcodeDialog(getString(
                                R.string.message_barcode_wrong_sample,
                                getExpectedBarcodeId(sharedPreferences)
                        ));
                        break;
                    }
                    scannedBarcodes.add(barcode.getValue());
                    sharedPreferences.edit().putStringSet(Constants.PREF_SCANNED_BARCODES, scannedBarcodes).apply();
                    cancelAlarm();
                    cancelTimer(barcode.getValue());
                    markWakeupRecordedIfNeeded(sharedPreferences);
                    endOfDayAlertType = getEndOfDayAlertType(sharedPreferences);
                    finishScanningProcess();
                    break;
                case INVALID:
                    logInvalidBarcode(barcode.getValue());
                    showInvalidBarcodeDialog();
                    break;
            }
        }
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public void setSalivaId(int salivaId) {
        this.salivaId = salivaId;
    }

    public void setCancelAlarmAfterScan(boolean cancelAlarmAfterScan) {
        this.cancelAlarmAfterScan = cancelAlarmAfterScan;
    }

    @Override
    protected void showInvalidBarcodeDialog() {
        showInvalidBarcodeDialog(getString(R.string.message_barcode_invalid));
    }

    private void showInvalidBarcodeDialog(String message) {
        if (getContext() == null) {
            return;
        }

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }

        new CarwatchDialogBuilder(getContext())
                .setTitle(R.string.title_barcode_invalid)
                .setIcon(icon)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> workflowModel.workflowState.setValue(WorkflowState.DETECTING)).show();
    }

    private void cancelAlarm() {
        AlarmRepository repository = AlarmRepository.getInstance(getContext());
        Alarm alarm;

        try {
            alarm = repository.getAlarmById(alarmId);
            if (alarm != null) {
                alarm.setWasSampleTaken(true);
                if (cancelAlarmAfterScan) {
                    AlarmHandler.cancelAlarm(getContext(), alarm, null);
                    alarm.setActive(false);
                }
                repository.updateAndWait(alarm);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while getting alarm with id " + alarmId + " from database: "  + e.getMessage());
        }
    }

    private void cancelTimer(String barcodeValue) {
        if (getContext() == null) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        int idEveningSample = sharedPreferences.getInt(Constants.PREF_EVENING_SALIVA_ID, 1);
        int dayId = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 1);
        String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE);

        // create Json object and log information
        try {
            JSONObject json = new JSONObject();
            int startIndex = Integer.parseInt(startSample.substring(1));
            String samplePrefix = startSample.substring(0, 1);

            BarcodeChecker.ParsedBarcode parsedBarcode = BarcodeChecker.parseBarcodeValue(barcodeValue);
            String scannedSample = samplePrefix;
            int scannedDay = dayId;
            if (parsedBarcode == null) {
                scannedSample += barcodeValue;
            } else {
                scannedDay = parsedBarcode.getDayId();
                int scannedSampleId = parsedBarcode.getSalivaId();
                scannedSample += scannedSampleId == idEveningSample + startIndex
                        ? Constants.EXTRA_SALIVA_ID_EVENING
                        : scannedSampleId;
            }

            String expectedSample = samplePrefix;
            switch (alarmId) {
                case Constants.EXTRA_ALARM_ID_EVENING:
                    expectedSample += Constants.EXTRA_SALIVA_ID_EVENING;
                    break;
                case Constants.EXTRA_ALARM_ID_MANUAL:
                    expectedSample += Constants.EXTRA_SALIVA_ID_MANUAL_HR;
                    break;
                default:
                    expectedSample += salivaId + startIndex;
            }

            int salivaDayId = dayId * 100 + salivaId;
            if (alarmId == Constants.EXTRA_ALARM_ID_MANUAL) {
                salivaDayId = Constants.EXTRA_SALIVA_ID_MANUAL;
            }

            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
            json.put(Constants.LOGGER_EXTRA_SALIVA_ID, salivaDayId);
            json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcodeValue);
            json.put(Constants.LOGGER_EXTRA_SCANNED_DAY, scannedDay);
            json.put(Constants.LOGGER_EXTRA_EXPECTED_DAY, dayId);
            json.put(Constants.LOGGER_EXTRA_SCANNED_SAMPLE, scannedSample);
            json.put(Constants.LOGGER_EXTRA_EXPECTED_SAMPLE, expectedSample);
            LoggerUtil.log(Constants.LOGGER_ACTION_BARCODE_SCANNED, json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not log barcode scan", e);
        }

        TimerHandler.cancelTimer(getContext(), alarmId);

        markEveningSampleTakenIfNeeded(sharedPreferences);

        String alertType = getEndOfDayAlertType(sharedPreferences);
        if (Constants.END_OF_DAY_ALERT_DAY_FINISHED.equals(alertType)
                || Constants.END_OF_DAY_ALERT_STUDY_FINISHED.equals(alertType)) {
            TimerHandler.finishDay(getContext());
        }
    }

    private void markEveningSampleTakenIfNeeded(SharedPreferences sharedPreferences) {
        int eveningSampleId = sharedPreferences.getInt(Constants.PREF_EVENING_SALIVA_ID, -1);
        if (alarmId != Constants.EXTRA_ALARM_ID_EVENING && salivaId != eveningSampleId) {
            return;
        }

        DateTime time = LocalTime.MIDNIGHT.toDateTimeToday();
        if (LocalTime.now().isBefore(new LocalTime(5, 0))) {
            time = time.minusDays(1);
        }

        sharedPreferences.edit().putLong(Constants.PREF_EVENING_TAKEN, time.getMillis()).apply();
    }

    private void markWakeupRecordedIfNeeded(SharedPreferences sharedPreferences) {
        if (alarmId != Constants.EXTRA_ALARM_ID_INITIAL || salivaId != Constants.EXTRA_SALIVA_ID_INITIAL) {
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
            LoggerUtil.log(Constants.LOGGER_ACTION_SPONTANEOUS_AWAKENING, json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not log spontaneous awakening", e);
        }

        sharedPreferences.edit()
                .putLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, DateTime.now().getMillis())
                .putBoolean(Constants.PREF_WAKEUP_SCAN_PENDING, false)
                .remove(Constants.PREF_WAKEUP_SCAN_PENDING_TIME)
                .apply();
    }

    private String getEndOfDayAlertType(SharedPreferences sharedPreferences) {
        int dayId = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 1);
        boolean hasEveningSample = sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false);
        int eveningSampleId = sharedPreferences.getInt(Constants.PREF_EVENING_SALIVA_ID, -1);
        int numDays = sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0);
        int totalNumSamples = sharedPreferences.getInt(Constants.PREF_TOTAL_NUM_SAMPLES, 0);
        int regularSamplesPerDay = hasEveningSample ? totalNumSamples - 1 : totalNumSamples;
        int scannedRegularSamplesToday = countScannedSamplesForDay(sharedPreferences, dayId, eveningSampleId, false);
        int scannedEveningSamplesToday = countScannedSamplesForDay(sharedPreferences, dayId, eveningSampleId, true);
        boolean currentScanIsEveningSample = alarmId == Constants.EXTRA_ALARM_ID_EVENING || salivaId == eveningSampleId;

        if (hasEveningSample && scannedRegularSamplesToday >= regularSamplesPerDay && scannedEveningSamplesToday == 0) {
            return Constants.END_OF_DAY_ALERT_EVENING_REQUIRED;
        }

        boolean allSamplesForDayRecorded = hasEveningSample
                ? scannedRegularSamplesToday >= regularSamplesPerDay && (scannedEveningSamplesToday > 0 || currentScanIsEveningSample)
                : scannedRegularSamplesToday >= regularSamplesPerDay;

        if (!allSamplesForDayRecorded) {
            return null;
        }

        if (dayId >= numDays) {
            return Constants.END_OF_DAY_ALERT_STUDY_FINISHED;
        }

        return Constants.END_OF_DAY_ALERT_DAY_FINISHED;
    }

    private int countScannedSamplesForDay(SharedPreferences sharedPreferences, int dayId, int eveningSampleId, boolean countEvening) {
        int startIndex = getStartSampleIndex(sharedPreferences);
        Set<String> scannedBarcodes = sharedPreferences.getStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>());
        int count = 0;

        for (String barcode : scannedBarcodes) {
            if (barcode == null || barcode.length() < 7) {
                continue;
            }

            BarcodeChecker.ParsedBarcode parsedBarcode = BarcodeChecker.parseBarcodeValue(barcode);
            if (parsedBarcode == null) {
                continue;
            }

            int scannedDay = parsedBarcode.getDayId();
            int scannedSampleId = parsedBarcode.getSalivaId() - startIndex;
            if (scannedDay != dayId) {
                continue;
            }

            boolean isEveningSample = scannedSampleId == eveningSampleId;
            if (isEveningSample == countEvening) {
                count++;
            }
        }

        return count;
    }

    private boolean matchesExpectedSample(String barcodeValue, SharedPreferences sharedPreferences) {
        if (alarmId == Constants.EXTRA_ALARM_ID_MANUAL) {
            return true;
        }

        BarcodeChecker.ParsedBarcode parsedBarcode = BarcodeChecker.parseBarcodeValue(barcodeValue);
        if (parsedBarcode == null) {
            return false;
        }

        int expectedDayId = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 1);
        int expectedSampleId = salivaId + getStartSampleIndex(sharedPreferences);
        return parsedBarcode.getDayId() == expectedDayId
                && parsedBarcode.getSalivaId() == expectedSampleId;
    }

    private boolean shouldEnforceExpectedBarcodeId(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(Constants.PREF_CHECK_DUPLICATES, false);
    }

    private String getExpectedBarcodeId(SharedPreferences sharedPreferences) {
        String expectedSampleId = getExpectedSampleId(sharedPreferences);
        String participantId = sharedPreferences.getString(Constants.PREF_PARTICIPANT_ID, "");
        if (participantId == null || participantId.trim().isEmpty()) {
            return expectedSampleId;
        }

        int numDays = sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0);
        if (numDays > 1) {
            int dayId = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 1);
            return participantId + "_D" + dayId + "_" + expectedSampleId;
        }

        return participantId + "_" + expectedSampleId;
    }

    private String getExpectedSampleId(SharedPreferences sharedPreferences) {
        String samplePrefix = getSamplePrefix(sharedPreferences);
        if (alarmId == Constants.EXTRA_ALARM_ID_EVENING) {
            return samplePrefix + Constants.EXTRA_SALIVA_ID_EVENING;
        }
        if (alarmId == Constants.EXTRA_ALARM_ID_MANUAL) {
            return samplePrefix + Constants.EXTRA_SALIVA_ID_MANUAL_HR;
        }
        return samplePrefix + (salivaId + getStartSampleIndex(sharedPreferences));
    }

    private String getSamplePrefix(SharedPreferences sharedPreferences) {
        String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE);
        return !startSample.isEmpty() ? startSample.substring(0, 1) : Constants.DEFAULT_START_SAMPLE.substring(0, 1);
    }

    private void logInvalidBarcode(String barcodeValue) {
        try {
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcodeValue);
            LoggerUtil.log(Constants.LOGGER_ACTION_INVALID_BARCODE_SCANNED, json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not log invalid barcode scan", e);
        }
    }

    private int getStartSampleIndex(SharedPreferences sharedPreferences) {
        String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE);
        try {
            return Integer.parseInt(startSample.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showBarcodeAlreadyScannedDialog() {
        if (getContext() == null) {
            return;
        }

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }

        new CarwatchDialogBuilder(getContext())
                .setTitle(R.string.title_barcode_already_scanned)
                .setIcon(icon)
                .setMessage(R.string.message_barcode_already_scanned)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> workflowModel.workflowState.setValue(WorkflowState.DETECTING)).show();
    }

    private void finishScanningProcess() {
        if (getActivity() == null)
            return;

        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra(Constants.EXTRA_SHOW_BARCODE_SCANNED_MSG, true);
        intent.putExtra(Constants.EXTRA_TARGET_NAV_ELEMENT, R.id.navigation_alarm);
        if (endOfDayAlertType != null) {
            intent.putExtra(Constants.EXTRA_END_OF_DAY_ALERT_TYPE, endOfDayAlertType);
        }
        startActivity(intent);
        getActivity().finish();
    }
}
