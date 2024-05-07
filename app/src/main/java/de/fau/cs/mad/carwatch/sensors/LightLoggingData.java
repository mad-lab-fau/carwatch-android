package de.fau.cs.mad.carwatch.sensors;

import de.fau.cs.mad.carwatch.util.Utils;

public class LightLoggingData {
    private long timestamp;
    private float lightIntensity;
    private boolean isObjectClose;

    public LightLoggingData(long timestamp, float lightIntensity, boolean isObjectClose) {
        this.timestamp = timestamp;
        this.lightIntensity = lightIntensity;
        this.isObjectClose = isObjectClose;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTranslatedTimestamp() {
        return Utils.translateTimestamp(timestamp);
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Float getLightIntensity() {
        return lightIntensity;
    }

    public void setLightIntensity(float lightIntensity) {
        this.lightIntensity = lightIntensity;
    }

    public boolean isObjectClose() {
        return isObjectClose;
    }

    public void setObjectClose(boolean isObjectClose) {
        this.isObjectClose = isObjectClose;
    }
}
