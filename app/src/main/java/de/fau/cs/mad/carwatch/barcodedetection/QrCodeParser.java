package de.fau.cs.mad.carwatch.barcodedetection;

import java.util.HashMap;
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
    public String salivaDistances; // will be parsed later, since Arrays can't be stored in SP
    public String salivaTimes; // will be parsed later, since Arrays can't be stored in SP
    public String startSample;
    public int studyDays;
    public int numParticipants;
    public String participantId = "";
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
                String[] pair = property.split(Constants.QR_PARSER_SPECIFIER);
                String key = pair[0];
                String value = pair.length > 1 ? pair[1] : "";
                propertyMap.put(key, value);
            }
        }

        try {
            studyName = propertyMap.get(Constants.QR_PARSER_PROPERTY_STUDY_NAME);
            salivaDistances = propertyMap.get(Constants.QR_PARSER_PROPERTY_SALIVA_DISTANCES);
            salivaTimes = propertyMap.get(Constants.QR_PARSER_PROPERTY_SALIVA_TIMES);
            startSample = propertyMap.get(Constants.QR_PARSER_PROPERTY_START_SAMPLE);
            studyDays = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_STUDY_DAYS))
            );
            numParticipants = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_NUM_PARTICIPANTS))
            );
            hasEveningSalivette = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_EVENING))
            ) == 1;
            shareEmailAddress = propertyMap.get(Constants.QR_PARSER_PROPERTY_CONTACT);
            checkDuplicates = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_DUPLICATES))
            ) == 1;
            manualScan = Integer.parseInt(Objects.requireNonNull(propertyMap.get(Constants.QR_PARSER_PROPERTY_MANUAL_SCAN))) == 1;
            if (propertyMap.containsKey(Constants.QR_PARSER_PROPERTY_PARTICIPANT_ID))
                participantId = propertyMap.get(Constants.QR_PARSER_PROPERTY_PARTICIPANT_ID);

        } catch (NullPointerException e) {
            throw new RuntimeException("QR-Code could not be parsed properly!");
        }
    }
}
