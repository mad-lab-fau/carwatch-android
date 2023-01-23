package de.fau.cs.mad.carwatch.ui.barcode;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult;
import static de.fau.cs.mad.carwatch.barcodedetection.camera.WorkflowModel.WorkflowState;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

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
import de.fau.cs.mad.carwatch.barcodedetection.QrCodeParser;

public class QrFragment extends BarcodeFragment {

    private static final String TAG = QrFragment.class.getSimpleName();

    private SharedPreferences sharedPreferences;

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

    public void setStudyData(QrCodeParser parser) {
        sharedPreferences.edit()
                .putString(Constants.PREF_STUDY_NAME, parser.studyName)
                .putStringSet(Constants.PREF_SUBJECT_LIST, parser.subjectList)
                .putString(Constants.PREF_SALIVA_TIMES, parser.salivaTimes)
                .putInt(Constants.PREF_NUM_DAYS, parser.studyDays)
                .putBoolean(Constants.PREF_HAS_EVENING, parser.hasEveningSalivette)
                .putString(Constants.PREF_SHARE_EMAIL_ADDRESS, parser.shareEmailAddress)
                .putBoolean(Constants.PREF_FIRST_RUN_QR, false)
                .apply();
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