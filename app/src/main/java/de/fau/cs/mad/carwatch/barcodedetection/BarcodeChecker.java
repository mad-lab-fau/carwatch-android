package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.Set;

import de.fau.cs.mad.carwatch.Constants;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.INVALID;
import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.VALID;

import android.content.SharedPreferences;
import android.util.Log;

public class BarcodeChecker {

    private static final String TAG = BarcodeChecker.class.getSimpleName();

    public enum BarcodeCheckResult {
        VALID,
        INVALID,
        DUPLICATE_BARCODE
    }

    public static BarcodeCheckResult isValidBarcode(String barcode, Set<String> scannedBarcodes, SharedPreferences sharedPreferences) {
        int subjectRange = sharedPreferences.getStringSet(Constants.PREF_SUBJECT_LIST, null).size();
        int salivaRange = sharedPreferences.getString(Constants.PREF_SALIVA_TIMES, "")
                .split(Constants.QR_PARSER_LIST_SEPARATOR)
                .length + 1;
        int dayRange = sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0);

        if (scannedBarcodes.contains(barcode)) {
            return BarcodeCheckResult.DUPLICATE_BARCODE;
        }

        int barcodeVal = Integer.parseInt(barcode);
        int subjectId = (int) (barcodeVal / 1e4);
        int dayId = (int) (barcodeVal / 1e2) % 100;
        int salivaId = barcodeVal % 100;

        if (subjectId <= subjectRange) {
            if (dayId <= dayRange) {
                if (salivaId <= salivaRange) {
                    return VALID;
                }
            }
        }

        return BarcodeCheckResult.INVALID;
    }

    public static BarcodeCheckResult isValidQrCode(QrCodeParser parser) {
        try {
            parser.parse();
        } catch (RuntimeException e) {
            Log.d(TAG, "Error Encoding QR-Code: " + e.getMessage());
            return INVALID;
        }
        return VALID;
    }
}
