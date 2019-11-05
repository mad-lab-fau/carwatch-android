package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.Set;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.DUPLICATE_BARCODE;
import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.INVALID;
import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.VALID;

public class BarcodeChecker {

    private static final String TAG = BarcodeChecker.class.getSimpleName();

    // 3 digits (msb)
    private static final int subjectRange = 100;
    // 2 digits
    private static final int dayRange = 2;
    // 2 digits (lsb)
    private static final int salivaRange = 5;

    public enum BarcodeCheckResult {
        VALID,
        INVALID,
        DUPLICATE_BARCODE
    }

    public static BarcodeCheckResult isValidBarcode(String barcode, Set<String> scannedBarcodes) {
        if (scannedBarcodes.contains(barcode)) {
            return DUPLICATE_BARCODE;
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

        return INVALID;
    }
}
