package de.fau.cs.mad.carwatch.ui.barcode;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult;
import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.google.mlkit.vision.barcode.common.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeField;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeProcessor;
import de.fau.cs.mad.carwatch.barcodedetection.QrCodeParser;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeSlide;
import de.fau.cs.mad.carwatch.logger.MetadataLogger;


public class QrFragment extends BarcodeFragment implements WelcomeSlide {

    private static final String TAG = QrFragment.class.getSimpleName();

    private SharedPreferences sharedPreferences;
    private final ObservableBoolean isSkipButtonVisible = new ObservableBoolean(false);
    private final ObservableBoolean canShowNextSlide = new ObservableBoolean(false);


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraSource.setFrameProcessor(new BarcodeProcessor(graphicOverlay, workflowModel, Barcode.FORMAT_QR_CODE));
        workflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public ObservableBoolean getSkipButtonIsVisible() {
        return isSkipButtonVisible;
    }

    @Override
    public ObservableBoolean getCanShowNextSlide() {
        return canShowNextSlide;
    }

    @Override
    public ObservableBoolean getCanShowPreviousSlide() {
        return new ObservableBoolean(false);
    }

    @Override
    public void onSlideFinished() {
        // Do nothing
    }

    @Override
    public void onChanged(Barcode mlKitBarcode) {
        if (mlKitBarcode != null) {
            BarcodeField barcode = new BarcodeField(Constants.BARCODE_TYPE_QR, mlKitBarcode.getRawValue());

            Log.d(TAG, "Detected QR-Code: " + barcode.getValue());

            QrCodeParser parser = new QrCodeParser(barcode.getValue());
            BarcodeCheckResult check = BarcodeChecker.isValidQrCode(parser);

            Log.d(TAG, "QR-Code scan: " + check);

            switch (check) {
                case VALID:
                    setStudyData(parser);
                    canShowNextSlide.set(true);
                    canShowNextSlide.notifyChange();
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
                .setTitle(R.string.title_qr_code_invalid)
                .setIcon(icon)
                .setMessage(R.string.message_qr_code_invalid)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> workflowModel.workflowState.setValue(WorkflowState.DETECTING)).show();
    }

    private void setStudyData(QrCodeParser parser) {
        String salivaDistances = parser.getSalivaDistances();
        String salivaTimes = parser.getSalivaTimes();

        int numEveningSamples = parser.hasEveningSample() ? 1 : 0;
        int numMorningSamples = salivaDistances.isEmpty() ? 0 : salivaDistances.split(",").length;
        int numFixedSamples = salivaTimes.isEmpty() ? 0 : salivaTimes.split(",").length;
        int numSamples = numFixedSamples + numMorningSamples + numEveningSamples;
        int eveningSampleId = parser.hasEveningSample() ? numSamples - 1 : -1;
        sharedPreferences.edit()
                .putString(Constants.PREF_STUDY_NAME, parser.getStudyName())
                .putInt(Constants.PREF_NUM_PARTICIPANTS, parser.getNumParticipants())
                .putString(Constants.PREF_SALIVA_DISTANCES, salivaDistances)
                .putString(Constants.PREF_SALIVA_TIMES, salivaTimes)
                .putInt(Constants.PREF_TOTAL_NUM_SAMPLES, numSamples)
                .putInt(Constants.PREF_EVENING_SALIVA_ID, eveningSampleId)
                .putInt(Constants.PREF_NUM_DAYS, parser.getStudyDays())
                .putBoolean(Constants.PREF_HAS_EVENING, parser.hasEveningSample())
                .putString(Constants.PREF_SHARE_EMAIL_ADDRESS, parser.getShareEmailAddress())
                .putBoolean(Constants.PREF_CHECK_DUPLICATES, parser.isCheckDuplicatesEnabled())
                .putBoolean(Constants.PREF_FIRST_RUN_QR, false)
                .putString(Constants.PREF_START_SAMPLE, parser.getStartSample())
                .apply();

        String participantId = parser.getParticipantId();

        if (!participantId.isEmpty()) {
            sharedPreferences.edit()
                    .putString(Constants.PREF_PARTICIPANT_ID, participantId)
                    .putBoolean(Constants.PREF_PARTICIPANT_ID_WAS_SET, true)
                    .apply();

            // log metadata after participant id was set to ensure correct log filename
            MetadataLogger.logDeviceProperties();
            MetadataLogger.logAppMetadata();
            MetadataLogger.logStudyData(requireContext());
            MetadataLogger.logParticipantId(requireContext());
        }
    }
}