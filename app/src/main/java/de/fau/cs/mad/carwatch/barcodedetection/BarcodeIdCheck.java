package de.fau.cs.mad.carwatch.barcodedetection;

import de.fau.cs.mad.carwatch.Constants;

public class BarcodeIdCheck {

    public static boolean isValidBarcode(int barcode) {
        return (barcode >= Constants.BARCODE_RANGE[0] && barcode <= Constants.BARCODE_RANGE[1]);
    }
}
