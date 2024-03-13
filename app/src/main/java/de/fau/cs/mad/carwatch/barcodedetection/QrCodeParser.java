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

    private final String dataString;
    private String studyName;
    private String salivaDistances; // will be parsed later, since Arrays can't be stored in SP
    private String salivaTimes; // will be parsed later, since Arrays can't be stored in SP
    private String startSample;
    private int studyDays;
    private int numParticipants;
    private String participantId = "";
    private boolean hasEveningSample;
    private String shareEmailAddress;
    private boolean isCheckDuplicatesEnabled;
    private boolean isManualScanEnabled;

    public QrCodeParser(String dataString) {
        this.dataString = dataString;
    }

    public String getStudyName() {
        return studyName;
    }

    /**
     * Returns the distances between the saliva samples as a string in the format "x,y,z"
     * where x, y, z are the distances in minutes.
     * @return saliva distances
     */
    public String getSalivaDistances() {
        return salivaDistances;
    }

    /**
     * Returns the times of the saliva samples as a string in the format "x,y,z".
     * where x, y, z are the times in the format "HHmm".
     * @return saliva times
     */
    public String getSalivaTimes() {
        return salivaTimes;
    }

    public String getStartSample() {
        return startSample;
    }

    public int getStudyDays() {
        return studyDays;
    }

    public int getNumParticipants() {
        return numParticipants;
    }

    public String getParticipantId() {
        return participantId;
    }

    public boolean hasEveningSample() {
        return hasEveningSample;
    }

    public String getShareEmailAddress() {
        return shareEmailAddress;
    }

    public boolean isCheckDuplicatesEnabled() {
        return isCheckDuplicatesEnabled;
    }

    public boolean isManualScanEnabled() {
        return isManualScanEnabled;
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
            hasEveningSample = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_EVENING))
            ) == 1;
            shareEmailAddress = propertyMap.get(Constants.QR_PARSER_PROPERTY_CONTACT);
            isCheckDuplicatesEnabled = Integer.parseInt(Objects.requireNonNull(
                    propertyMap.get(Constants.QR_PARSER_PROPERTY_DUPLICATES))
            ) == 1;
            isManualScanEnabled = Integer.parseInt(Objects.requireNonNull(propertyMap.get(Constants.QR_PARSER_PROPERTY_MANUAL_SCAN))) == 1;
            if (propertyMap.containsKey(Constants.QR_PARSER_PROPERTY_PARTICIPANT_ID))
                participantId = propertyMap.get(Constants.QR_PARSER_PROPERTY_PARTICIPANT_ID);

        } catch (NullPointerException e) {
            throw new RuntimeException("QR-Code could not be parsed properly!");
        }
    }
}
