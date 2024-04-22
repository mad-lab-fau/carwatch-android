package de.fau.cs.mad.carwatch.barcodedetection;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.util.VersionUtils;

public class QrLegacy {
    private static final String TAG = QrLegacy.class.getSimpleName();
    private static final String defaultVersion = VersionUtils.V_0_0_1;

    public static String adaptToCurrentVersion(String qrData) {
        if (qrData == null) {
            return null;
        }

        String webAppVersion = getVersion(qrData);

        if (VersionUtils.compareVersions(webAppVersion, VersionUtils.V_1_0_0) < 0) {
            return convertV0_0_1(qrData);
        } else if (VersionUtils.compareVersions(webAppVersion, VersionUtils.V_1_1_0) < 0) {
            return convertV1_0_0(qrData);
        }

        return qrData;
    }

    private static String getVersion(String qrData) {
        for (String part : qrData.split(Constants.QR_PARSER_SEPARATOR)) {
            String[] property = part.split(Constants.QR_PARSER_SPECIFIER);
            if (property.length != 2)
                continue;

            if (property[0].equals(Constants.QR_PARSER_PROPERTY_WEB_APP_VERSION)) {
                return property[1];
            }
        }

        return defaultVersion;
    }

    private static String convertV0_0_1(String qrData) {
        String originalQrData = qrData;

        if (propertyIsMissing(qrData, Constants.QR_PARSER_PROPERTY_SALIVA_TIMES)) {
            qrData += Constants.QR_PARSER_SEPARATOR + Constants.QR_PARSER_PROPERTY_SALIVA_TIMES + Constants.QR_PARSER_SPECIFIER;
        }

        if (propertyIsMissing(qrData, Constants.QR_PARSER_PROPERTY_USE_GOOGLE_FIT)) {
            qrData += Constants.QR_PARSER_SEPARATOR + Constants.QR_PARSER_PROPERTY_USE_GOOGLE_FIT + Constants.QR_PARSER_SPECIFIER + "0";
        }

        String oldNumParticipantsIdentifier = "S";
        qrData = replacePropertyIdentifier(qrData, oldNumParticipantsIdentifier, Constants.QR_PARSER_PROPERTY_NUM_PARTICIPANTS);
        LoggerUtil.log(TAG, "Converted QR code from version " + VersionUtils.V_0_0_1 + ": " + originalQrData + " -> " + qrData);
        return qrData;
    }

    private static String convertV1_0_0(String qrData) {
        String originalQrData = qrData;

        if (propertyIsMissing(qrData, Constants.QR_PARSER_PROPERTY_USE_GOOGLE_FIT)) {
            qrData += Constants.QR_PARSER_SEPARATOR + Constants.QR_PARSER_PROPERTY_USE_GOOGLE_FIT + Constants.QR_PARSER_SPECIFIER + "0";
        }

        LoggerUtil.log(TAG, "Converted QR code from version " + VersionUtils.V_1_0_0 + ": " + originalQrData + " -> " + qrData);
        return qrData;
    }

    private static boolean propertyIsMissing(String qrData, String property) {
        String regex = regexForIdentifier(property);
        return !qrData.matches(regex);
    }

    private static String replacePropertyIdentifier(String qrData, String oldIdentifier, String newIdentifier) {
        String regexOld = regexForIdentifier(oldIdentifier);
        String regexNew = "$1" + newIdentifier + "$2";  // $1 and $2 are the qrData parts before and after the identifier
        return qrData.replaceFirst(regexOld, regexNew);
    }

    private static String regexForIdentifier(String identifier) {
        return "((?:.*)(?:^|" + Constants.QR_PARSER_SEPARATOR + "))" + identifier + "(" + Constants.QR_PARSER_SPECIFIER + ".*)";
    }
}
