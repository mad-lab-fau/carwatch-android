package de.fau.cs.mad.carwatch.barcodedetection;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import de.fau.cs.mad.carwatch.Constants;

/**
 * Helper Class to decode user data from study QR-Code.
 */
public class QrCodeParser {

    private static final String TAG = QrCodeParser.class.getSimpleName();

    public String dataString;
    public String studyName;
    public HashSet<String> subjectList;
    public String salivaTimes; // will be parsed later, since Arrays can't be stored in SP
    public int studyDays;
    public boolean hasEveningSalivette;
    public String shareEmailAddress;

    public QrCodeParser(String dataString) {
        this.dataString = dataString;
    }

    public void parse() {
        String[] properties = dataString.split(Constants.QR_PARSER_SEPARATOR);
        Map<String, String> propertyMap = new HashMap<>();

        for (String property : properties) {
            // first element needs to be app id
            if (property.equals(properties[0])) {
                if (!properties[0].equals(Constants.QR_PARSER_APP_ID)) {
                    throw new RuntimeException("Invalid QR-Code!");
                }
            } else {
                propertyMap.put(
                        property.split(Constants.QR_PARSER_SPECIFIER)[0],
                        property.split(Constants.QR_PARSER_SPECIFIER)[1]
                );
            }
        }

        try {
            studyName = propertyMap.get(Constants.QR_PARSER_PROPERTY_STUDY_NAME);
            Log.d(TAG, "Parsed study name: " + studyName);

            subjectList = parseStringAsSet(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_PARTICIPANTS))
            );
            Log.d(TAG, "Parsed subject list: " + subjectList);

            salivaTimes = propertyMap.get(Constants.QR_PARSER_PROPERTY_SALIVA_TIMES);
            Log.d(TAG, "Parsed saliva times: " + salivaTimes);

            studyDays = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_STUDY_DAYS))
            );
            Log.d(TAG, "Parsed study days: " + studyDays);

            hasEveningSalivette = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_EVENING))
            ) == 1;
            Log.d(TAG, "Parsed has evening salivette: " + hasEveningSalivette);

            shareEmailAddress = propertyMap.get(Constants.QR_PARSER_PROPERTY_CONTACT);
            Log.d(TAG, "Parsed share contact: " + shareEmailAddress);
        } catch (NullPointerException e) {
            throw new RuntimeException("QR-Code could not be parsed properly!");
        }
    }

    private HashSet<String> parseStringAsSet(String encodedList) {
        String[] result = encodedList.split(Constants.QR_PARSER_LIST_SEPARATOR);
        return new HashSet<>(Arrays.asList(result));
    }
}
