package de.fau.cs.mad.carwatch.subject;

import java.util.HashMap;

import static de.fau.cs.mad.carwatch.subject.Condition.KNOWN_ALARM;
import static de.fau.cs.mad.carwatch.subject.Condition.SPONTANEOUS;
import static de.fau.cs.mad.carwatch.subject.Condition.UNKNOWN_ALARM;

public class SubjectMap {

    private static HashMap<String, Condition> sSubjectMap = new HashMap<>();


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
        sSubjectMap.put("FD31L", SPONTANEOUS);
        sSubjectMap.put("FM13R", SPONTANEOUS);
        sSubjectMap.put("NH21N", SPONTANEOUS);
        sSubjectMap.put("KH13H", SPONTANEOUS);
        sSubjectMap.put("JA09B", SPONTANEOUS);
        sSubjectMap.put("JS26R", SPONTANEOUS);
        sSubjectMap.put("FK22R", SPONTANEOUS);
        sSubjectMap.put("CE10B", SPONTANEOUS);
        sSubjectMap.put("BU07E", SPONTANEOUS);
        sSubjectMap.put("KS11H", SPONTANEOUS);
        sSubjectMap.put("BM15R", SPONTANEOUS);
        sSubjectMap.put("DT04N", SPONTANEOUS);
        sSubjectMap.put("JU27L", SPONTANEOUS);
        sSubjectMap.put("MB25L", SPONTANEOUS);
        sSubjectMap.put("SA18O", SPONTANEOUS);
        sSubjectMap.put("LA26R", SPONTANEOUS);
        sSubjectMap.put("EM16N", SPONTANEOUS);
        sSubjectMap.put("CK11R", SPONTANEOUS);
        sSubjectMap.put("AC12E", SPONTANEOUS);
        sSubjectMap.put("LT21N", SPONTANEOUS);
        sSubjectMap.put("CC21W", SPONTANEOUS);
        sSubjectMap.put("MI15D", SPONTANEOUS);
        sSubjectMap.put("JU01F", SPONTANEOUS);
        sSubjectMap.put("LB21E", SPONTANEOUS);
        sSubjectMap.put("FA12L", SPONTANEOUS);
        sSubjectMap.put("VE19A", SPONTANEOUS);
        sSubjectMap.put("CT29K", SPONTANEOUS);
        sSubjectMap.put("BC05R", SPONTANEOUS);
        sSubjectMap.put("CC09K", SPONTANEOUS);
        sSubjectMap.put("SV03R", SPONTANEOUS);
        sSubjectMap.put("JM22C", SPONTANEOUS);
        sSubjectMap.put("MG08K", SPONTANEOUS);
        sSubjectMap.put("LG27Z", SPONTANEOUS);
        sSubjectMap.put("PS06G", SPONTANEOUS);
        sSubjectMap.put("MC04A", SPONTANEOUS);
        sSubjectMap.put("MH17F", SPONTANEOUS);
        sSubjectMap.put("UH09L", KNOWN_ALARM);
        sSubjectMap.put("PG11T", KNOWN_ALARM);
        sSubjectMap.put("DL13A", KNOWN_ALARM);
        sSubjectMap.put("SE12H", KNOWN_ALARM);
        sSubjectMap.put("LH11G", KNOWN_ALARM);
        sSubjectMap.put("VE08R", KNOWN_ALARM);
        sSubjectMap.put("JG04G", KNOWN_ALARM);
        sSubjectMap.put("ME06K", KNOWN_ALARM);
        sSubjectMap.put("TA31S", KNOWN_ALARM);
        sSubjectMap.put("KM23K", KNOWN_ALARM);
        sSubjectMap.put("MK25L", KNOWN_ALARM);
        sSubjectMap.put("EG09H", KNOWN_ALARM);
        sSubjectMap.put("MK25S", KNOWN_ALARM);
        sSubjectMap.put("AB19E", KNOWN_ALARM);
        sSubjectMap.put("CM27J", KNOWN_ALARM);
        sSubjectMap.put("KL19T", KNOWN_ALARM);
        sSubjectMap.put("OB20R", KNOWN_ALARM);
        sSubjectMap.put("KE17E", KNOWN_ALARM);
        sSubjectMap.put("DC28D", KNOWN_ALARM);
        sSubjectMap.put("JC05N", KNOWN_ALARM);
        sSubjectMap.put("WM13K", KNOWN_ALARM);
        sSubjectMap.put("KD16N", KNOWN_ALARM);
        sSubjectMap.put("FB04R", KNOWN_ALARM);
        sSubjectMap.put("FS11N", KNOWN_ALARM);
        sSubjectMap.put("KS25S", KNOWN_ALARM);
        sSubjectMap.put("SR19R", UNKNOWN_ALARM);
        sSubjectMap.put("JM07ß", KNOWN_ALARM);
        sSubjectMap.put("TB29R", KNOWN_ALARM);
        sSubjectMap.put("HE12B", KNOWN_ALARM);
        sSubjectMap.put("EU25L", KNOWN_ALARM);
        sSubjectMap.put("JK20G", KNOWN_ALARM);
        sSubjectMap.put("LU16N", KNOWN_ALARM);
        sSubjectMap.put("JU22I", KNOWN_ALARM);
        sSubjectMap.put("PB16D", KNOWN_ALARM);
        sSubjectMap.put("MD09H", UNKNOWN_ALARM);
        sSubjectMap.put("AC29R", UNKNOWN_ALARM);
        sSubjectMap.put("SL10R", UNKNOWN_ALARM);
        sSubjectMap.put("LA09L", UNKNOWN_ALARM);
        sSubjectMap.put("MA04N", UNKNOWN_ALARM);
        sSubjectMap.put("KC031", UNKNOWN_ALARM);
        sSubjectMap.put("PE16R", UNKNOWN_ALARM);
        sSubjectMap.put("DI21I", UNKNOWN_ALARM);
        sSubjectMap.put("SD30K", UNKNOWN_ALARM);
        sSubjectMap.put("LP06T", UNKNOWN_ALARM);
        sSubjectMap.put("SE12O", UNKNOWN_ALARM);
        sSubjectMap.put("SS22K", UNKNOWN_ALARM);
        sSubjectMap.put("RA03A", UNKNOWN_ALARM);
        sSubjectMap.put("CU24R", UNKNOWN_ALARM);
        sSubjectMap.put("DN28H", UNKNOWN_ALARM);
        sSubjectMap.put("II08L", UNKNOWN_ALARM);
        sSubjectMap.put("TM30L", UNKNOWN_ALARM);
        sSubjectMap.put("DL16R", UNKNOWN_ALARM);
        sSubjectMap.put("MD26R", UNKNOWN_ALARM);
        sSubjectMap.put("RF17R", UNKNOWN_ALARM);
        sSubjectMap.put("DS18L", UNKNOWN_ALARM);
        sSubjectMap.put("FA27L", UNKNOWN_ALARM);
        sSubjectMap.put("MH25K", UNKNOWN_ALARM);
        sSubjectMap.put("ME18L", UNKNOWN_ALARM);
        sSubjectMap.put("KS28N", UNKNOWN_ALARM);
        sSubjectMap.put("LS13L", UNKNOWN_ALARM);
        sSubjectMap.put("MS28B", UNKNOWN_ALARM);
        sSubjectMap.put("JH08K", UNKNOWN_ALARM);
        sSubjectMap.put("MB14C", UNKNOWN_ALARM);
        sSubjectMap.put("SR08R", UNKNOWN_ALARM);
        sSubjectMap.put("EK27R", UNKNOWN_ALARM);
        sSubjectMap.put("MD22R", UNKNOWN_ALARM);
        sSubjectMap.put("EA09H", UNKNOWN_ALARM);
        sSubjectMap.put("Sk12k", UNKNOWN_ALARM);
        sSubjectMap.put("JE15A", UNKNOWN_ALARM);
        sSubjectMap.put("KA19E", KNOWN_ALARM);
        sSubjectMap.put("AS03C", SPONTANEOUS);
        sSubjectMap.put("KL24U", UNKNOWN_ALARM);
        sSubjectMap.put("AE01U", UNKNOWN_ALARM);
        sSubjectMap.put("IM09E", UNKNOWN_ALARM);
        sSubjectMap.put("RB14R", KNOWN_ALARM);
        sSubjectMap.put("SE06H", KNOWN_ALARM);
        sSubjectMap.put("LW28E", KNOWN_ALARM);
        sSubjectMap.put("KA17A", UNKNOWN_ALARM);
        sSubjectMap.put("JA24N", SPONTANEOUS);
    }

    public static Condition getConditionForSubject(String subjectId) {
        return sSubjectMap.get(subjectId.toUpperCase());
    }
}
