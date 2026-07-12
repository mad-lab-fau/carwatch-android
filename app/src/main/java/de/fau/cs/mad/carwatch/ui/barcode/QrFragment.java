package de.fau.cs.mad.carwatch.ui.barcode;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult;
import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.mlkit.vision.barcode.common.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.collection.ArraySet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import de.fau.cs.mad.carwatch.ui.CarwatchDialogBuilder;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeField;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeProcessor;
import de.fau.cs.mad.carwatch.barcodedetection.QrCodeParser;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.onboarding.steps.WelcomeSlide;
import de.fau.cs.mad.carwatch.logger.MetadataLogger;
import de.fau.cs.mad.carwatch.debug.DemoStudyLoader;
import de.fau.cs.mad.carwatch.ui.MainActivity;


public class QrFragment extends BarcodeFragment implements WelcomeSlide {

    private static final String TAG = QrFragment.class.getSimpleName();

    private SharedPreferences sharedPreferences;
    private final ObservableBoolean isSkipButtonVisible = new ObservableBoolean(false);
    private final ObservableBoolean canShowNextSlide = new ObservableBoolean(false);
    private ScanSuccessListener scanSuccessListener;
    private boolean validQrCodeHandled = false;

    public interface ScanSuccessListener {
        void onQrCodeScanSuccessful();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        if (!DemoStudyLoader.isAvailable() || !(root instanceof ConstraintLayout rootLayout)) {
            return root;
        }

        MaterialButton demoButton = new MaterialButton(requireContext());
        demoButton.setId(View.generateViewId());
        demoButton.setText(R.string.menu_load_demo_study);
        demoButton.setIconResource(R.drawable.ic_school_24dp);
        demoButton.setOnClickListener(view -> {
            demoButton.setEnabled(false);
            DemoStudyLoader.load(requireActivity(), () -> {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.putExtra(Constants.EXTRA_TARGET_NAV_ELEMENT, R.id.navigation_alarm);
                startActivity(intent);
                requireActivity().finish();
            });
        });

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.demo_study_button_bottom_margin);
        rootLayout.addView(demoButton, params);
        return root;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ScanSuccessListener) {
            scanSuccessListener = (ScanSuccessListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        scanSuccessListener = null;
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
                    if (validQrCodeHandled) {
                        return;
                    }
                    validQrCodeHandled = true;
                    workflowModel.setWorkflowState(WorkflowState.SEARCHED);
                    setStudyData(parser);
                    canShowNextSlide.set(true);
                    canShowNextSlide.notifyChange();
                    if (scanSuccessListener != null) {
                        scanSuccessListener.onQrCodeScanSuccessful();
                    }
                    break;
                case INVALID:
                    try {
                        JSONObject json = new JSONObject();
                        json.put(Constants.LOGGER_EXTRA_BARCODE_VALUE, barcode.getValue());
                        LoggerUtil.log(Constants.LOGGER_ACTION_INVALID_BARCODE_SCANNED, json);
                    } catch (JSONException e) {
                        Log.e(TAG, "Could not log invalid QR code scan", e);
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

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_warning_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        }

        new CarwatchDialogBuilder(getContext())
                .setTitle(R.string.title_qr_code_invalid)
                .setIcon(icon)
                .setMessage(R.string.message_qr_code_invalid)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> workflowModel.workflowState.setValue(WorkflowState.DETECTING)).show();
    }

    @Override
    protected int getDetectingPromptStringRes() {
        return R.string.prompt_point_at_a_qr_code;
    }

    private void setStudyData(QrCodeParser parser) {
        String salivaDistances = parser.getSalivaDistances();
        String salivaTimes = parser.getSalivaTimes();

        int numEveningSamples = parser.hasEveningSample() ? 1 : 0;
        int numMorningSamples = AlarmHandler.countMorningSamples(salivaDistances);
        int numFixedSamples = salivaTimes.isEmpty() ? 0 : salivaTimes.split(",").length;
        int numSamples = numFixedSamples + numMorningSamples + numEveningSamples;
        int eveningSampleId = parser.hasEveningSample() ? numSamples - 1 : -1;
        sharedPreferences.edit()
                .putInt(Constants.PREF_DAY_COUNTER, 0)
                .putInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL)
                .putInt(Constants.PREF_CURRENT_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL + 1)
                .putBoolean(Constants.PREF_TIMER_NOTIFICATION_IS_SHOWN, false)
                .putBoolean(Constants.PREF_STUDY_DAY_MANUALLY_ADVANCED, false)
                .putStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>())
                .remove(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME)
                .remove(Constants.PREF_EVENING_TAKEN)
                .remove(Constants.PREF_WAKEUP_ALERT_TYPE)
                .remove(Constants.PREF_WAKEUP_DELAYED_SAMPLE_MINUTES)
                .remove(Constants.PREF_WAKEUP_SCAN_PENDING)
                .remove(Constants.PREF_WAKEUP_SCAN_PENDING_TIME)
                .remove(Constants.PREF_WAKEUP_SAMPLE_TAKEN_TIME)
                .remove(Constants.PREF_EVENING_REMINDER_TIME_MINUTES)
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
