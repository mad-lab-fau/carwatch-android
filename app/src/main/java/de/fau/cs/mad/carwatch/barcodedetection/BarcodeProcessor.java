/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fau.cs.mad.carwatch.barcodedetection;

import android.animation.ValueAnimator;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.MainThread;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.List;

import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraReticleAnimator;
import de.fau.cs.mad.carwatch.barcodedetection.camera.FrameProcessorBase;
import de.fau.cs.mad.carwatch.barcodedetection.camera.GraphicOverlay;
import de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel;
import de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

/**
 * A processor to run the barcode detector.
 */
public class BarcodeProcessor extends FrameProcessorBase<List<FirebaseVisionBarcode>> {

    private static final String TAG = BarcodeProcessor.class.getSimpleName();

    private final FirebaseVisionBarcodeDetectorOptions options =
            new FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_EAN_8)
                    .build();

    private final FirebaseVisionBarcodeDetector detector =
            FirebaseVision.getInstance().getVisionBarcodeDetector(options);

    private final WorkflowModel workflowModel;
    private final CameraReticleAnimator cameraReticleAnimator;

    public BarcodeProcessor(GraphicOverlay graphicOverlay, WorkflowModel workflowModel) {
        this.workflowModel = workflowModel;
        this.cameraReticleAnimator = new CameraReticleAnimator(graphicOverlay);
    }

    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @MainThread
    @Override
    protected void onSuccess(FirebaseVisionImage image, List<FirebaseVisionBarcode> results, GraphicOverlay graphicOverlay) {
        if (!workflowModel.isCameraLive()) {
            return;
        }

        //Log.d(TAG, "Barcode result size: " + results.size());

        // Picks the barcode, if exists, that covers the center of graphic overlay.
        FirebaseVisionBarcode barcodeInCenter = null;
        for (FirebaseVisionBarcode barcode : results) {
            if (barcode.getBoundingBox() != null) {
                RectF box = graphicOverlay.translateRect(barcode.getBoundingBox());
                if (box.contains(graphicOverlay.getWidth() / 2f, graphicOverlay.getHeight() / 2f)) {
                    barcodeInCenter = barcode;
                    break;
                }
            }
        }

        graphicOverlay.clear();
        if (barcodeInCenter == null) {
            cameraReticleAnimator.start();
            graphicOverlay.add(new BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator));
            workflowModel.setWorkflowState(WorkflowState.DETECTING);

        } else {
            cameraReticleAnimator.cancel();
            ValueAnimator loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter);
            loadingAnimator.start();
            graphicOverlay.add(new BarcodeLoadingGraphic(graphicOverlay, loadingAnimator));
            workflowModel.setWorkflowState(WorkflowState.SEARCHING);

        }
        graphicOverlay.invalidate();
    }

    private ValueAnimator createLoadingAnimator(
            GraphicOverlay graphicOverlay, FirebaseVisionBarcode barcode) {
        float endProgress = 1.1f;
        ValueAnimator loadingAnimator = ValueAnimator.ofFloat(0f, endProgress);
        loadingAnimator.setDuration(2000);
        loadingAnimator.addUpdateListener(
                animation -> {
                    if (Float.compare((float) loadingAnimator.getAnimatedValue(), endProgress) >= 0) {
                        graphicOverlay.clear();
                        workflowModel.setWorkflowState(WorkflowState.SEARCHED);
                        workflowModel.detectedBarcode.setValue(barcode);
                    } else {
                        graphicOverlay.invalidate();
                    }
                });
        return loadingAnimator;
    }

    @Override
    protected void onFailure(Exception e) {
        Log.e(TAG, "Barcode detection failed!", e);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close barcode detector!", e);
        }
    }
}
