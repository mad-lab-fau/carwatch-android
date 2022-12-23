package de.fau.cs.mad.carwatch.ui.barcode;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.internal.Objects;
import com.google.android.material.chip.Chip;
import com.google.mlkit.vision.barcode.common.Barcode;

import org.json.JSONObject;

import java.io.IOException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraSource;
import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraSourcePreview;
import de.fau.cs.mad.carwatch.barcodedetection.camera.GraphicOverlay;
import de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

public abstract class BarcodeFragment extends Fragment implements View.OnClickListener, Observer<Barcode> {

    private static final String TAG = BarcodeFragment.class.getSimpleName();

    protected CameraSource cameraSource;
    protected CameraSourcePreview preview;
    protected GraphicOverlay graphicOverlay;
    protected Chip promptChip;
    protected AnimatorSet promptChipAnimator;

    protected WorkflowModel workflowModel;
    protected WorkflowState currentWorkflowState;

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
        workflowModel = new ViewModelProvider(this).get(WorkflowModel.class);

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel.workflowState.observe(
                getViewLifecycleOwner(),
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

        workflowModel.detectedBarcode.observe(getViewLifecycleOwner(), this);
    }


    protected abstract void showInvalidBarcodeDialog();

}