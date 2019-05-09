package uk.gov.hmcts.reform.em.stitching.service;

public class StringFormattingUtils {

    public static String ensureStringEndsWithSuffix(String string, String suffix) {
        return string.endsWith(suffix) ? string : string + suffix;
    }
}
