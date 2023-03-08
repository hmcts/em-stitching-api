package uk.gov.hmcts.reform.em.stitching.service;

import org.apache.commons.lang3.StringUtils;

public class StringFormattingUtils {

    public static final String SUFFIX = ".pdf";
    public static final String PREFIX = "stitched";

    public static String generateFileName(String fileName) {
        if (StringUtils.isNotBlank(fileName)) {
            return StringUtils.endsWithIgnoreCase(fileName, SUFFIX)
                    ? fileName : fileName + SUFFIX;
        } else {
            return PREFIX + System.currentTimeMillis() + SUFFIX;
        }
    }
}
