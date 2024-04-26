package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.Set;

import androidx.collection.ArraySet;
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

    public static BarcodeCheckResult isValidBarcode(String barcode, SharedPreferences sharedPreferences) {
        Set<String> scannedBarcodes = sharedPreferences.getStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>());

        if (sharedPreferences.getBoolean(Constants.PREF_CHECK_DUPLICATES, false) && scannedBarcodes.contains(barcode))
            return BarcodeCheckResult.DUPLICATE_BARCODE;

        int numParticipants = sharedPreferences.getInt(Constants.PREF_NUM_PARTICIPANTS, 0);
        int numSamples = sharedPreferences.getInt(Constants.PREF_TOTAL_NUM_SAMPLES, 0);
        int numDays = sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0);

        int barcodeVal = Integer.parseInt(barcode);
        int participantId = (int) (barcodeVal / 1e4);
        int dayId = (int) (barcodeVal / 1e2) % 100;
        int salivaId = barcodeVal % 100;

        if (participantId <= numParticipants && dayId <= numDays && salivaId <= numSamples) {
            return VALID;
        }

        return BarcodeCheckResult.INVALID;
    }

    public static BarcodeCheckResult isValidQrCode(QrCodeParser parser) {
        return parser.isValid() ? VALID : INVALID;
    }
}
