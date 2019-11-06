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

    // SUBJECT CODES!
    static {
        sSubjectMap.put("AB19E", KNOWN_ALARM);
        sSubjectMap.put("AB31R", KNOWN_ALARM);
        sSubjectMap.put("AC12E", KNOWN_ALARM);
        sSubjectMap.put("AH10Y", KNOWN_ALARM);
        sSubjectMap.put("AK12I", UNKNOWN_ALARM);
        sSubjectMap.put("AM21H", UNKNOWN_ALARM);
        sSubjectMap.put("AU09R", SPONTANEOUS);
        sSubjectMap.put("BC05R", KNOWN_ALARM);
        sSubjectMap.put("BM15R", UNKNOWN_ALARM);
        sSubjectMap.put("BR11A", KNOWN_ALARM);
        sSubjectMap.put("BU07E", KNOWN_ALARM);
        sSubjectMap.put("CC09K", UNKNOWN_ALARM);
        sSubjectMap.put("CC10A", KNOWN_ALARM);
        sSubjectMap.put("CC21W", KNOWN_ALARM);
        sSubjectMap.put("CE10B", SPONTANEOUS);
        sSubjectMap.put("CJ10Z", UNKNOWN_ALARM);
        sSubjectMap.put("CK11R", UNKNOWN_ALARM);
        sSubjectMap.put("CT29K", KNOWN_ALARM);
        sSubjectMap.put("CU24R", UNKNOWN_ALARM);
        sSubjectMap.put("DC28D", UNKNOWN_ALARM);
        sSubjectMap.put("DI21I", UNKNOWN_ALARM);
        sSubjectMap.put("DK11R", KNOWN_ALARM);
        sSubjectMap.put("DL16R", UNKNOWN_ALARM);
        sSubjectMap.put("DN28H", UNKNOWN_ALARM);
        sSubjectMap.put("DT04N", UNKNOWN_ALARM);
        sSubjectMap.put("EA09H", KNOWN_ALARM);
        sSubjectMap.put("EE08O", KNOWN_ALARM);
        sSubjectMap.put("EG25R", KNOWN_ALARM);
        sSubjectMap.put("EI14R", KNOWN_ALARM);
        sSubjectMap.put("EM07N", SPONTANEOUS);
        sSubjectMap.put("EM16N", KNOWN_ALARM);
        sSubjectMap.put("FA12L", UNKNOWN_ALARM);
        sSubjectMap.put("FA23L", KNOWN_ALARM);
        sSubjectMap.put("FB04R", UNKNOWN_ALARM);
        sSubjectMap.put("FK22R", KNOWN_ALARM);
        sSubjectMap.put("HE12B", UNKNOWN_ALARM);
        sSubjectMap.put("HT08S", SPONTANEOUS);
        sSubjectMap.put("II08L", UNKNOWN_ALARM);
        sSubjectMap.put("IM09E", KNOWN_ALARM);
        sSubjectMap.put("JA24N", SPONTANEOUS);
        sSubjectMap.put("JA26L", SPONTANEOUS);
        sSubjectMap.put("JC05N", KNOWN_ALARM);
        sSubjectMap.put("JE03S", KNOWN_ALARM);
        sSubjectMap.put("JE15A", SPONTANEOUS);
        sSubjectMap.put("JG04G", UNKNOWN_ALARM);
        sSubjectMap.put("JK12R", KNOWN_ALARM);
        sSubjectMap.put("JK20G", UNKNOWN_ALARM);
        sSubjectMap.put("JK25L", KNOWN_ALARM);
        sSubjectMap.put("JM07ß", UNKNOWN_ALARM);
        sSubjectMap.put("JM22C", UNKNOWN_ALARM);
        sSubjectMap.put("JS26S", SPONTANEOUS);
        sSubjectMap.put("JU01F", KNOWN_ALARM);
        sSubjectMap.put("KA17A", UNKNOWN_ALARM);
        sSubjectMap.put("KA19E", UNKNOWN_ALARM);
        sSubjectMap.put("KB16K", KNOWN_ALARM);
        sSubjectMap.put("KD16N", KNOWN_ALARM);
        sSubjectMap.put("KE17E", UNKNOWN_ALARM);
        sSubjectMap.put("KL19T", KNOWN_ALARM);
        sSubjectMap.put("KL24U", KNOWN_ALARM);
        sSubjectMap.put("KM23K", KNOWN_ALARM);
        sSubjectMap.put("KS11H", UNKNOWN_ALARM);
        sSubjectMap.put("KS28N", UNKNOWN_ALARM);
        sSubjectMap.put("KU08D", KNOWN_ALARM);
        sSubjectMap.put("LA09L", UNKNOWN_ALARM);
        sSubjectMap.put("LA23T", KNOWN_ALARM);
        sSubjectMap.put("LA26R", UNKNOWN_ALARM);
        sSubjectMap.put("LB21E", KNOWN_ALARM);
        sSubjectMap.put("LC05H", KNOWN_ALARM);
        sSubjectMap.put("LE04A", KNOWN_ALARM);
        sSubjectMap.put("LG27Z", SPONTANEOUS);
        sSubjectMap.put("LI20Z", KNOWN_ALARM);
        sSubjectMap.put("LJ16R", KNOWN_ALARM);
        sSubjectMap.put("LK24L", SPONTANEOUS);
        sSubjectMap.put("LM18T", UNKNOWN_ALARM);
        sSubjectMap.put("LP06T", UNKNOWN_ALARM);
        sSubjectMap.put("LP25R", UNKNOWN_ALARM);
        sSubjectMap.put("LS13L", KNOWN_ALARM);
        sSubjectMap.put("LW28E", UNKNOWN_ALARM);
        sSubjectMap.put("LW28R", UNKNOWN_ALARM);
        sSubjectMap.put("MC04A", UNKNOWN_ALARM);
        sSubjectMap.put("MD22R", KNOWN_ALARM);
        sSubjectMap.put("MD26R", UNKNOWN_ALARM);
        sSubjectMap.put("ME06K", KNOWN_ALARM);
        sSubjectMap.put("ME18L", UNKNOWN_ALARM);
        sSubjectMap.put("MG08K", KNOWN_ALARM);
        sSubjectMap.put("MH14A", KNOWN_ALARM);
        sSubjectMap.put("MH25K", UNKNOWN_ALARM);
        sSubjectMap.put("MI15D", UNKNOWN_ALARM);
        sSubjectMap.put("MS09E", KNOWN_ALARM);
        sSubjectMap.put("MS23B", SPONTANEOUS);
        sSubjectMap.put("MS28B", UNKNOWN_ALARM);
        sSubjectMap.put("NM11M", KNOWN_ALARM);
        sSubjectMap.put("OB20R", UNKNOWN_ALARM);
        sSubjectMap.put("PA13R", UNKNOWN_ALARM);
        sSubjectMap.put("PG11T", KNOWN_ALARM);
        sSubjectMap.put("PS06G", SPONTANEOUS);
        sSubjectMap.put("RA03A", UNKNOWN_ALARM);
        sSubjectMap.put("RF17R", UNKNOWN_ALARM);
        sSubjectMap.put("RM13N", KNOWN_ALARM);
        sSubjectMap.put("SB11Z", KNOWN_ALARM);
        sSubjectMap.put("SC10R", UNKNOWN_ALARM);
        sSubjectMap.put("SD30K", UNKNOWN_ALARM);
        sSubjectMap.put("SE06H", UNKNOWN_ALARM);
        sSubjectMap.put("SE12O", UNKNOWN_ALARM);
        sSubjectMap.put("SG29T", SPONTANEOUS);
        sSubjectMap.put("SK12K", UNKNOWN_ALARM);
        sSubjectMap.put("SL10R", UNKNOWN_ALARM);
        sSubjectMap.put("SR08R", SPONTANEOUS);
        sSubjectMap.put("SR19R", UNKNOWN_ALARM);
        sSubjectMap.put("SS11H", KNOWN_ALARM);
        sSubjectMap.put("SS21A", KNOWN_ALARM);
        sSubjectMap.put("SS22K", UNKNOWN_ALARM);
        sSubjectMap.put("SU29N", KNOWN_ALARM);
        sSubjectMap.put("SV03R", SPONTANEOUS);
        sSubjectMap.put("TB29R", KNOWN_ALARM);
        sSubjectMap.put("TM01F", UNKNOWN_ALARM);
        sSubjectMap.put("TM30L", UNKNOWN_ALARM);
        sSubjectMap.put("UH09L", KNOWN_ALARM);
        sSubjectMap.put("VA30T", KNOWN_ALARM);
        sSubjectMap.put("VE19A", UNKNOWN_ALARM);
        sSubjectMap.put("VS09S", UNKNOWN_ALARM);
        sSubjectMap.put("WM13K", UNKNOWN_ALARM);
    }

    public static Condition getConditionForSubject(String subjectId) {
        return sSubjectMap.get(subjectId.toUpperCase());
    }
}
