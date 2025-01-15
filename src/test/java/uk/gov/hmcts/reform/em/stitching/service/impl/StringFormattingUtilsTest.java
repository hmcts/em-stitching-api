package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.stitching.service.StringFormattingUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringFormattingUtilsTest {

    private static final String suffix = ".suffix";

    @Test
    void generateFileNameWithOutSuffix() {
        String stringWithoutSuffix = "test_input_string_without_suffix";
        String result = StringFormattingUtils.generateFileName(stringWithoutSuffix);
        assertEquals(stringWithoutSuffix + StringFormattingUtils.SUFFIX, result);
    }

    @Test
    void generateFileNameWithSuffix() {
        String stringWithSuffix = "test_input_string_with_suffix.pdf";
        String result = StringFormattingUtils.generateFileName(stringWithSuffix);
        assertEquals(stringWithSuffix, result);
    }

    @Test
    void generateFileNameWithOutInput() {
        String result = StringFormattingUtils.generateFileName(null);
        assertTrue(StringUtils.startsWithIgnoreCase(result, StringFormattingUtils.PREFIX));
        assertTrue(StringUtils.endsWithIgnoreCase(result, StringFormattingUtils.SUFFIX));
    }
}
