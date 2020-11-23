package de.fau.cs.mad.carwatch.subject;

import java.util.HashMap;

import static de.fau.cs.mad.carwatch.subject.Condition.KNOWN_ALARM;
import static de.fau.cs.mad.carwatch.subject.Condition.SPONTANEOUS;
import static de.fau.cs.mad.carwatch.subject.Condition.UNKNOWN_ALARM;

public class SubjectMap {

    public static HashMap<String, Condition> sSubjectMap = new HashMap<>();


    // DEBUG CODES
    static {
        sSubjectMap.put("TB01A", KNOWN_ALARM);
        sSubjectMap.put("TB01B", KNOWN_ALARM);
        sSubjectMap.put("TB01C", KNOWN_ALARM);
        sSubjectMap.put("TB02A", UNKNOWN_ALARM);
        sSubjectMap.put("TB02B", UNKNOWN_ALARM);
        sSubjectMap.put("TB02C", UNKNOWN_ALARM);
        sSubjectMap.put("TB03A", SPONTANEOUS);
        sSubjectMap.put("TB03B", SPONTANEOUS);
        sSubjectMap.put("TB03C", SPONTANEOUS);
    }

    public static Condition getConditionForSubject(String subjectId) {
        return KNOWN_ALARM;
    }
}
