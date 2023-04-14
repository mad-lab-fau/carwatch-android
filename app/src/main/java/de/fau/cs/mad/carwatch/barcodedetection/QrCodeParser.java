package de.fau.cs.mad.carwatch.barcodedetection;

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
    public String startSample;
    public int studyDays;
    public boolean hasEveningSalivette;
    public String shareEmailAddress;
    public boolean checkDuplicates;
    public boolean manualScan;

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
            subjectList = parseStringAsSet(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_PARTICIPANTS))
            );
            salivaTimes = propertyMap.get(Constants.QR_PARSER_PROPERTY_SALIVA_TIMES);
            // add offset of initial saliva sample
            salivaTimes = Constants.FIRST_SALIVA_SAMPLE_OFFSET + Constants.QR_PARSER_LIST_SEPARATOR + salivaTimes;
            startSample = propertyMap.get(Constants.QR_PARSER_PROPERTY_START_SAMPLE);
            studyDays = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_STUDY_DAYS))
            );
            hasEveningSalivette = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_EVENING))
            ) == 1;
            shareEmailAddress = propertyMap.get(Constants.QR_PARSER_PROPERTY_CONTACT);
            checkDuplicates = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_DUPLICATES))
            ) == 1;
            manualScan = Integer.parseInt(Objects.requireNonNull(propertyMap.get(Constants.QR_PARSER_PROPERTY_MANUAL_SCAN))) == 1;

        } catch (NullPointerException e) {
            throw new RuntimeException("QR-Code could not be parsed properly!");
        }
    }

    private HashSet<String> parseStringAsSet(String encodedList) {
        String[] result = encodedList.split(Constants.QR_PARSER_LIST_SEPARATOR);
        return new HashSet<>(Arrays.asList(result));
    }
}
