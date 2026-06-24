package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.Set;

import androidx.collection.ArraySet;
import de.fau.cs.mad.carwatch.Constants;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.INVALID;
import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.VALID;

import android.content.SharedPreferences;

public class BarcodeChecker {

    public enum BarcodeCheckResult {
        VALID,
        INVALID,
        DUPLICATE_BARCODE
    }

    public static BarcodeCheckResult isValidBarcode(String barcode, SharedPreferences sharedPreferences) {
        Set<String> scannedBarcodes = sharedPreferences.getStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>());
        boolean checkDuplicates = sharedPreferences.getBoolean(Constants.PREF_CHECK_DUPLICATES, false);

        if (checkDuplicates && scannedBarcodes.contains(barcode))
            return BarcodeCheckResult.DUPLICATE_BARCODE;

        if (!checkDuplicates) {
            return VALID;
        }

        int numParticipants = sharedPreferences.getInt(Constants.PREF_NUM_PARTICIPANTS, 0);
        int numSamples = sharedPreferences.getInt(Constants.PREF_TOTAL_NUM_SAMPLES, 0);
        int numDays = sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0);

        int barcodeVal;
        try {
            barcodeVal = Integer.parseInt(barcode);
        } catch (NumberFormatException e) {
            return INVALID;
        }

        int participantId = (int) (barcodeVal / 1e4);
        int dayId = (int) (barcodeVal / 1e2) % 100;
        int salivaId = barcodeVal % 100;

        if (participantId <= numParticipants && dayId <= numDays && salivaId <= numSamples) {
            return VALID;
        }

        return BarcodeCheckResult.INVALID;
    }

    public static ParsedBarcode parseBarcodeValue(String barcode) {
        if (barcode == null || !barcode.matches("\\d{7}")) {
            return null;
        }

        return new ParsedBarcode(
                Integer.parseInt(barcode.substring(3, 5)),
                Integer.parseInt(barcode.substring(5, 7))
        );
    }

    public static BarcodeCheckResult isValidQrCode(QrCodeParser parser) {
        return parser.isValid() ? VALID : INVALID;
    }

    public static class ParsedBarcode {
        private final int dayId;
        private final int salivaId;

        private ParsedBarcode(int dayId, int salivaId) {
            this.dayId = dayId;
            this.salivaId = salivaId;
        }

        public int getDayId() {
            return dayId;
        }

        public int getSalivaId() {
            return salivaId;
        }
    }
}
