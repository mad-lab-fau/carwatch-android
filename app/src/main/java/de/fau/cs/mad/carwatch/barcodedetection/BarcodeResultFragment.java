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

import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel;

import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

/**
 * Displays the bottom sheet to present barcode fields contained in the detected barcode.
 */
public class BarcodeResultFragment extends BottomSheetDialogFragment {

    private static final String TAG = BarcodeResultFragment.class.getSimpleName();
    private static final String ARG_BARCODE_FIELD = "arg_barcode_field";

    private final DialogInterface.OnDismissListener dismissListener;

    private BarcodeResultFragment(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;

        if (getActivity() == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getActivity().setShowWhenLocked(true);
            getActivity().setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(getActivity(), null);
            }
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getActivity().getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

    }

    public static void show(FragmentManager fragmentManager, BarcodeField
            barcodeField, DialogInterface.OnDismissListener dismissListener) {
        BarcodeResultFragment barcodeResultFragment = new BarcodeResultFragment(dismissListener);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_BARCODE_FIELD, barcodeField);
        barcodeResultFragment.setArguments(bundle);
        barcodeResultFragment.show(fragmentManager, TAG);
    }

    public static void dismiss(FragmentManager fragmentManager) {
        BarcodeResultFragment barcodeResultFragment =
                (BarcodeResultFragment) fragmentManager.findFragmentByTag(TAG);
        if (barcodeResultFragment != null) {
            barcodeResultFragment.dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup
            viewGroup, @Nullable Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.barcode_bottom_sheet, viewGroup);
        BarcodeField barcodeField;
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey(ARG_BARCODE_FIELD)) {
            barcodeField = arguments.getParcelable(ARG_BARCODE_FIELD);
        } else {
            Log.e(TAG, "No barcode field list passed in!");
            barcodeField = new BarcodeField("", "");
        }

        RecyclerView fieldRecyclerView = view.findViewById(R.id.barcode_field_recycler_view);
        fieldRecyclerView.setHasFixedSize(true);
        fieldRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        fieldRecyclerView.setAdapter(new BarcodeFieldAdapter(barcodeField));

        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialogInterface) {
        if (getActivity() != null) {
            // Back to working state after the bottom sheet is dismissed.
            ViewModelProviders.of(getActivity())
                    .get(WorkflowModel.class)
                    .setWorkflowState(WorkflowState.DETECTING);
        }
        dismissListener.onDismiss(getDialog());
        super.onDismiss(dialogInterface);
    }
}
