package de.fau.cs.mad.carwatch.ui.barcode;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult;
import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.mlkit.vision.barcode.common.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeField;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeProcessor;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;

public class QrFragment extends BarcodeFragment {

    private static final String TAG = QrFragment.class.getSimpleName();

    @Override
    public void onResume() {
        super.onResume();
        cameraSource.setFrameProcessor(new BarcodeProcessor(graphicOverlay, workflowModel, Barcode.FORMAT_QR_CODE));
        workflowModel.setWorkflowState(WorkflowState.DETECTING);
    }

    public void setStudyData(String barcodeValue) {
        // TODO add all values
    }

    @Override
    public void onChanged(Barcode mlKitBarcode) {
        if (mlKitBarcode != null) {
            BarcodeField barcode = new BarcodeField("Barcode", mlKitBarcode.getRawValue());

            Log.d(TAG, "Detected Barcode: " + barcode.getValue());

            BarcodeCheckResult check = BarcodeChecker.isValidQrCode(barcode.getValue());

            Log.d(TAG, "Barcode scan: " + check);

            switch (check) {
                case VALID:
                    setStudyData(barcode.getValue());
                    finishActivity();
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

    private void finishActivity() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

}