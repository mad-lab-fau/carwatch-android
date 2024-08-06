package de.fau.cs.mad.carwatch.sleep;

import org.joda.time.DateTime;

public class SleepPhase {
    private String type;
    private DateTime start;
    private DateTime end;

    public SleepPhase(String type, DateTime start, DateTime end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DateTime getStart() {
        return start;
    }

    public void setStart(DateTime start) {
        this.start = start;
    }

    public DateTime getEnd() {
        return end;
    }

    public void setEnd(DateTime end) {
        this.end = end;
    }
}
