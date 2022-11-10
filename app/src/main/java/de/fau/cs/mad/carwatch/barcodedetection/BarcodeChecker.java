package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.Set;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.subject.SubjectMap;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheckResult.VALID;

public class BarcodeChecker {

    private static final String TAG = BarcodeChecker.class.getSimpleName();

    // 3 digits (msb)
    private static final int subjectRange = SubjectMap.sSubjectMap.size();
    // 2 digits (lsb)
    private static final int salivaRange = Constants.SALIVA_TIMES.length;

    public enum BarcodeCheckResult {
        VALID,
        INVALID,
        DUPLICATE_BARCODE
    }

    public static BarcodeCheckResult isValidBarcode(String barcode, Set<String> scannedBarcodes) {
        return VALID;
        /*if (scannedBarcodes.contains(barcode)) {
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

        return INVALID;*/
    }
}
