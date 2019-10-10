package de.fau.cs.mad.carwatch.ui.scanner;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.internal.Objects;
import com.google.android.material.chip.Chip;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.TimerHandler;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeField;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeProcessor;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeResultFragment;
import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraSource;
import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraSourcePreview;
import de.fau.cs.mad.carwatch.barcodedetection.camera.GraphicOverlay;
import de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

public class ScannerFragment extends Fragment implements View.OnClickListener, DialogInterface.OnDismissListener {

    private static final String TAG = ScannerFragment.class.getSimpleName();

    private int alarmId = Constants.EXTRA_ALARM_ID_DEFAULT;
    private int salivaId = Constants.EXTRA_SALIVA_ID_DEFAULT;

    private CameraSource cameraSource;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private Chip promptChip;
    private AnimatorSet promptChipAnimator;

    private WorkflowModel workflowModel;
    private WorkflowState currentWorkflowState;

    long alarmTime = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_scanner, container, false);

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

        workflowModel.detectedBarcode.observe(
                this,
                barcode -> {
                    if (barcode != null) {

                        ArrayList<BarcodeField> barcodeFieldList = new ArrayList<>();
                        barcodeFieldList.add(new BarcodeField("Raw Value", barcode.getRawValue()));
                        Log.d(TAG, "Detected Barcodes: " + barcodeFieldList);
                        BarcodeResultFragment.show(getChildFragmentManager(), barcodeFieldList, this);
                        // TODO check if correct barcode
                        cancelTimer(alarmId, salivaId, barcode.getRawValue());

                    }
                });
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
        if (getActivity() != null) {
            Intent intent = new Intent();
            intent.putExtra(Constants.EXTRA_ALARM_TIME, alarmTime);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }
}