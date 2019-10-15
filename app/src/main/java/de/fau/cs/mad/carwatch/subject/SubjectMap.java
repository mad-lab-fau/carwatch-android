package de.fau.cs.mad.carwatch.subject;

import java.util.HashMap;

import static de.fau.cs.mad.carwatch.subject.Condition.KNOWN_ALARM;
import static de.fau.cs.mad.carwatch.subject.Condition.SPONTANEOUS;
import static de.fau.cs.mad.carwatch.subject.Condition.UNKNOWN_ALARM;

public class SubjectMap {

    private static HashMap<String, Condition> sSubjectMap = new HashMap<>();

    static {
        sSubjectMap.put("TBED1A", KNOWN_ALARM);
        sSubjectMap.put("TBED1B", KNOWN_ALARM);
        sSubjectMap.put("TBED1C", KNOWN_ALARM);
        sSubjectMap.put("TBED2A", UNKNOWN_ALARM);
        sSubjectMap.put("TBED2B", UNKNOWN_ALARM);
        sSubjectMap.put("TBED2C", UNKNOWN_ALARM);
        sSubjectMap.put("TBED3A", SPONTANEOUS);
        sSubjectMap.put("TBED3B", SPONTANEOUS);
        sSubjectMap.put("TBED3C", SPONTANEOUS);
    }

    public static Condition getConditionForSubject(String subjectId) {
        return sSubjectMap.get(subjectId);
    }
}
