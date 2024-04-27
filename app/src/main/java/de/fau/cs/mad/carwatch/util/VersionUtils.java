package de.fau.cs.mad.carwatch.util;

public class VersionUtils {
    public static final String V_0_0_1 = "0.0.1";
    public static final String V_1_0_0 = "1.0.0";
    public static final String V_1_1_0 = "1.1.0";

    /**
     * Compares two version strings.
     *
     * @param version1 version 1 to compare
     * @param version2 version 2 to compare
     * @return 0 if the versions are equal, -1 if version1 < version2, 1 if version1 > version2
     */
    public static int compareVersions(String version1, String version2) {
        String[] splitVersion1 = version1.split("\\.");
        String[] splitVersion2 = version2.split("\\.");

        int minLength = Math.min(splitVersion1.length, splitVersion2.length);
        for (int i = 0; i < minLength; i++) {
            int part1 = Integer.parseInt(splitVersion1[i]);
            int part2 = Integer.parseInt(splitVersion2[i]);
            if (part1 < part2) {
                return -1;
            } else if (part1 > part2) {
                return 1;
            }
        }

        return Integer.compare(splitVersion1.length, splitVersion2.length);
    }
}
