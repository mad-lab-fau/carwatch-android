package de.fau.cs.mad.carwatch.subject;

public class SubjectIdCheck {

    public static boolean isValidSubjectId(String studyName, String subjectId) {
        //return subjectId.matches("[a-z]{2}\\d{2}[a-z]");
        if (studyName == null || subjectId == null) {
            return false;
        }
        return !subjectId.isEmpty() && !studyName.isEmpty();
        // TODO introduce Subject ID check?
        //return SubjectMap.sSubjectMap.containsKey(subjectId);
    }

}
