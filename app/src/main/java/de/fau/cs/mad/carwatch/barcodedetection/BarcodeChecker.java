package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.Set;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.subject.SubjectList;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.INVALID;
import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.VALID;

import android.util.Log;

public class BarcodeChecker {

    private static final String TAG = BarcodeChecker.class.getSimpleName();

    // 3 digits (msb)
    private static final int subjectRange = SubjectList.sSubjectList.size();
    // 2 digits (lsb)
    private static final int salivaRange = Constants.SALIVA_TIMES.length;
    private static final int dayRange = Constants.NUM_DAYS;

    public enum BarcodeCheckResult {
        VALID,
        INVALID,
        DUPLICATE_BARCODE
    }

    public static BarcodeCheckResult isValidBarcode(String barcode, Set<String> scannedBarcodes) {
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
