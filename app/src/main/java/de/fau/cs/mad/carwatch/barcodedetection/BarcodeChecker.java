package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.Set;

import de.fau.cs.mad.carwatch.Constants;

import static de.fau.cs.mad.carwatch.barcodedetection.BarcodeChecker.BarcodeCheck.*;

public class BarcodeChecker {

    public enum BarcodeCheck {
        VALID,
        INVALID,
        DUPLICATE_BARCODE
    }

    public static BarcodeCheck isValidBarcode(String barcode, Set<String> scannedBarcodes) {
        if (scannedBarcodes.contains(barcode)) {
            return DUPLICATE_BARCODE;
        }

        int barcodeVal = Integer.parseInt(barcode);
        if (barcodeVal >= Constants.BARCODE_RANGE[0] && barcodeVal <= Constants.BARCODE_RANGE[1]) {
            return VALID;
        }
        return INVALID;
    }
}
