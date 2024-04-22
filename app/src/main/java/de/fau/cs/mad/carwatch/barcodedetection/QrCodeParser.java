package de.fau.cs.mad.carwatch.barcodedetection;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.fau.cs.mad.carwatch.Constants;

/**
 * Helper Class to decode user data from study QR-Code.
 */
public class QrCodeParser {

    private static final String TAG = QrCodeParser.class.getSimpleName();

    private final Map<String, String> propertyMap = new HashMap<>();
    private boolean isValid = true;
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
    private boolean isGoogleFitEnabled;

    public QrCodeParser(String dataString) {
        String versionizedDataString = QrLegacy.adaptToCurrentVersion(dataString);
        parse(versionizedDataString);
    }

    public boolean isValid() {
        return isValid;
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

    public boolean isGoogleFitEnabled() {
        return isGoogleFitEnabled;
    }

    private void parse(String dataString) {
        String[] properties = dataString.split(Constants.QR_PARSER_SEPARATOR);

        if (properties.length < 1) {
            setInvalid("No QR-Code data found!");
            return;
        }

        if (!properties[0].equals(Constants.QR_PARSER_APP_ID)) {
            setInvalid("App ID not found. First QR code entry was: " + properties[0]);
            return;
        }

        for (String property : properties) {
            if (property.equals(Constants.QR_PARSER_APP_ID))
                continue;

            String[] pair = property.split(Constants.QR_PARSER_SPECIFIER);
            String key = pair[0];
            String value = pair.length > 1 ? pair[1] : "";
            propertyMap.put(key, value);
        }

        studyName = getStringProperty(Constants.QR_PARSER_PROPERTY_STUDY_NAME);
        salivaDistances = getStringProperty(Constants.QR_PARSER_PROPERTY_SALIVA_DISTANCES);
        salivaTimes = getStringProperty(Constants.QR_PARSER_PROPERTY_SALIVA_TIMES);
        startSample = getStringProperty(Constants.QR_PARSER_PROPERTY_START_SAMPLE);
        studyDays = getIntProperty(Constants.QR_PARSER_PROPERTY_STUDY_DAYS);
        numParticipants = getIntProperty(Constants.QR_PARSER_PROPERTY_NUM_PARTICIPANTS);
        hasEveningSample = getIntProperty(Constants.QR_PARSER_PROPERTY_EVENING) == 1;
        shareEmailAddress = getStringProperty(Constants.QR_PARSER_PROPERTY_CONTACT);
        isCheckDuplicatesEnabled = getIntProperty(Constants.QR_PARSER_PROPERTY_DUPLICATES) == 1;
        participantId = getStringProperty(Constants.QR_PARSER_PROPERTY_PARTICIPANT_ID, false);
        isGoogleFitEnabled = getIntProperty(Constants.QR_PARSER_PROPERTY_USE_GOOGLE_FIT) == 1;
    }

    private void setInvalid(String error) {
        isValid = false;
        Log.d(TAG, error);
    }

    private String getStringProperty(String key) {
        return getStringProperty(key, true);
    }

    private String getStringProperty(String key, boolean isMandatory) {
        if (propertyMap.containsKey(key))
            return propertyMap.get(key);

        if (isMandatory)
            setInvalid("Property " + key + " not found in QR-Code!");

        return "";
    }

    private int getIntProperty(String key) {
        if (!propertyMap.containsKey(key)) {
            setInvalid("Property " + key + " not found in QR-Code!");
            return 0;
        }

        try {
            return Integer.parseInt(Objects.requireNonNull(propertyMap.get(key)));
        } catch (NumberFormatException e) {
            setInvalid("Property " + key + " could not be parsed to int!");
            return 0;
        }
    }
}
