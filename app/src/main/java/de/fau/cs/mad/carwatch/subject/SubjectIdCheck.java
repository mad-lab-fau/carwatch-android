package de.fau.cs.mad.carwatch.subject;

public class SubjectIdCheck {

    public static boolean isValidSubjectId(String subjectId) {
        return subjectId.matches("[A-Z]{2}\\d{2}[A-Z]");
    }

}
