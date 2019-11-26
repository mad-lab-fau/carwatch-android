package de.fau.cs.mad.carwatch.ui.barcode;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.internal.Objects;
import com.google.android.material.chip.Chip;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Set;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeField;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeProcessor;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeResultFragment;
import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraSource;
import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraSourcePreview;
import de.fau.cs.mad.carwatch.barcodedetection.camera.GraphicOverlay;
import de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult;
import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

public class BarcodeFragment extends Fragment implements View.OnClickListener, DialogInterface.OnDismissListener, Observer<FirebaseVisionBarcode> {

    private static final String TAG = BarcodeFragment.class.getSimpleName();

    private int alarmId = Constants.EXTRA_ALARM_ID_DEFAULT;
    private int salivaId = Constants.EXTRA_SALIVA_ID_DEFAULT;

    private CameraSource cameraSource;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private Chip promptChip;
    private AnimatorSet promptChipAnimator;

    private WorkflowModel workflowModel;
    private WorkflowState currentWorkflowState;

    private long alarmTime = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_barcode, container, false);

        preview = root.findViewById(R.id.camera_preview);
        graphicOverlay = root.findViewById(R.id.camera_preview_graphic_overlay);
        graphicOverlay.setOnClickListener(this);
        cameraSource = new CameraSource(graphicOverlay);

        promptChip = root.findViewById(R.id.bottom_prompt_chip);
        promptChipAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.bottom_prompt_chip_enter);
        promptChipAnimator.setTarget(promptChip);

        setUpWorkflowModel();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        workflowModel.markCameraFrozen();
        currentWorkflowState = WorkflowState.NOT_STARTED;
        cameraSource.setFrameProcessor(new BarcodeProcessor(graphicOverlay, workflowModel));
        workflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    @Override
    public void onPause() {
        super.onPause();
        currentWorkflowState = WorkflowState.NOT_STARTED;
        stopCameraPreview();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
            cameraSource = null;
        }
    }

    @Override
    public void onClick(View v) {

    }


    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public void setSalivaId(int salivaId) {
        this.salivaId = salivaId;
    }


    private void startCameraPreview() {
        if (!workflowModel.isCameraLive() && cameraSource != null) {
            try {
                workflowModel.markCameraLive();
                preview.start(cameraSource);

                JSONObject json = new JSONObject();
                LoggerUtil.log(Constants.LOGGER_ACTION_BARCODE_SCAN_INIT, json);

            } catch (IOException e) {
                Log.e(TAG, "Failed to start camera preview!", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    private void stopCameraPreview() {
        if (workflowModel.isCameraLive()) {
            workflowModel.markCameraFrozen();
            preview.stop();
        }
    }

    private void setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel.class);

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel.workflowState.observe(
                this,
                workflowState -> {
                    if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
                        return;
                    }

                    currentWorkflowState = workflowState;
                    Log.d(TAG, "Current workflow state: " + currentWorkflowState.name());

                    boolean wasPromptChipGone = (promptChip.getVisibility() == View.GONE);

                    switch (workflowState) {
                        case DETECTING:
                            promptChip.setVisibility(View.VISIBLE);
                            promptChip.setText(R.string.prompt_point_at_a_barcode);
                            startCameraPreview();
                            break;
                        case SEARCHING:
                            promptChip.setVisibility(View.VISIBLE);
                            promptChip.setText(R.string.prompt_scanning);
                            stopCameraPreview();
                            break;
                        case SEARCHED:
                            promptChip.setVisibility(View.GONE);
                            stopCameraPreview();
                            break;
                        default:
                            promptChip.setVisibility(View.GONE);
                            break;
                    }

                    boolean shouldPlayPromptChipEnteringAnimation =
                            wasPromptChipGone && (promptChip.getVisibility() == View.VISIBLE);
                    if (shouldPlayPromptChipEnteringAnimation && !promptChipAnimator.isRunning()) {
                        promptChipAnimator.start();
                    }
                });

        workflowModel.detectedBarcode.observe(this, this);
    }

    private void cancelTimer(int alarmId, int salivaId, String barcodeValue) {
        if (getContext() == null) {
            return;
        }

        if (alarmId != Constants.EXTRA_ALARM_ID_DEFAULT) {
            // create Json object and log information
            try {
                JSONObject json = new JSONObject();
                json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
                json.put(Constants.LOGGER_EXTRA_SALIVA_ID, salivaId);
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
    public void onChanged(FirebaseVisionBarcode firebaseVisionBarcode) {
        if (firebaseVisionBarcode != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());

            BarcodeField barcode = new BarcodeField("Barcode", firebaseVisionBarcode.getRawValue());
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

    private void showInvalidBarcodeDialog() {
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