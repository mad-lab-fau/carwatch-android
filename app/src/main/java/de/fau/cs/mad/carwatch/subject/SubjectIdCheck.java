package de.fau.cs.mad.carwatch.subject;

public class SubjectIdCheck {

    public static boolean isValidSubjectId(String subjectId) {
        return subjectId.matches("[a-z]{2}\\d{2}[a-z]");
    }

}
