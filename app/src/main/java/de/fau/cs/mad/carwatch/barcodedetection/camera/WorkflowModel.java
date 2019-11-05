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

import android.app.Application;

import androidx.annotation.MainThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

/**
 * View model for handling application workflow based on camera preview.
 */
public class WorkflowModel extends AndroidViewModel {

    /**
     * State set of the application workflow.
     */
    public enum WorkflowState {
        NOT_STARTED,
        DETECTING,
        SEARCHING,
        SEARCHED
    }

    public final MutableLiveData<WorkflowState> workflowState = new MutableLiveData<>();
    public final MutableLiveData<FirebaseVisionBarcode> detectedBarcode = new MutableLiveData<>();

    private boolean isCameraLive = false;

    public WorkflowModel(Application application) {
        super(application);
    }

    @MainThread
    public void setWorkflowState(WorkflowState workflowState) {
        this.workflowState.setValue(workflowState);
    }

    public void markCameraLive() {
        isCameraLive = true;
    }

    public void markCameraFrozen() {
        isCameraLive = false;
    }

    public boolean isCameraLive() {
        return isCameraLive;
    }

}
