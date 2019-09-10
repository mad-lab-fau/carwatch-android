package de.fau.cs.mad.carwatch.subject;

import java.util.HashMap;

import static de.fau.cs.mad.carwatch.subject.Condition.UNKNOWN_ALARM;

public class SubjectMap {

    private static HashMap<String, Condition> sSubjectMap = new HashMap<>();

    static {
        sSubjectMap.put("0406HR94", UNKNOWN_ALARM);
    }

    public static Condition getConditionForSubject(String subjectId) {
        return sSubjectMap.get(subjectId);
    }
}
