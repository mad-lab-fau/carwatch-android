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

package de.fau.cs.mad.carwatch.barcodedetection.camera;

import android.util.Log;

import androidx.annotation.GuardedBy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;

/**
 * Abstract base class of {@link FrameProcessor}.
 */
public abstract class FrameProcessorBase<T> implements FrameProcessor {

    private static final String TAG = FrameProcessorBase.class.getSimpleName();

    // To keep the latest frame and its metadata.
    @GuardedBy("this")
    private ByteBuffer latestFrame;

    @GuardedBy("this")
    private FrameMetadata latestFrameMetaData;

    // To keep the frame and metadata in process.
    @GuardedBy("this")
    private ByteBuffer processingFrame;

    @GuardedBy("this")
    private FrameMetadata processingFrameMetaData;

    @Override
    public synchronized void process(
            ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay) {
        latestFrame = data;
        latestFrameMetaData = frameMetadata;
        if (processingFrame == null && processingFrameMetaData == null) {
            processLatestFrame(graphicOverlay);
        }
    }

    private synchronized void processLatestFrame(GraphicOverlay graphicOverlay) {
        processingFrame = latestFrame;
        processingFrameMetaData = latestFrameMetaData;
        latestFrame = null;
        latestFrameMetaData = null;
        if (processingFrame != null && processingFrameMetaData != null) {
            InputImage image = InputImage.fromByteBuffer(
                    processingFrame,
                    processingFrameMetaData.width,
                    processingFrameMetaData.height,
                    processingFrameMetaData.rotation,
                    InputImage.IMAGE_FORMAT_NV21
            );
            detectInImage(image)
                    .addOnSuccessListener(
                            results -> {
                                FrameProcessorBase.this.onSuccess(image, results, graphicOverlay);
                                processLatestFrame(graphicOverlay);
                            })
                    .addOnFailureListener(FrameProcessorBase.this::onFailure);
        }
    }

    protected abstract Task<T> detectInImage(InputImage image);

    /**
     * Be called when the detection succeeds.
     */
    protected abstract void onSuccess(
            InputImage image, T results, GraphicOverlay graphicOverlay);

    protected abstract void onFailure(Exception e);
}
