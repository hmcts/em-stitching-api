package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.StringFormattingUtils;

public class StringFormattingUtilsTest {

    private static final String suffix = ".suffix";

    @Test
    public void generateFileNameWithOutSuffix() {
        String stringWithoutSuffix = "test_input_string_without_suffix";
        String result = StringFormattingUtils.generateFileName(stringWithoutSuffix);
        Assert.assertEquals(stringWithoutSuffix + StringFormattingUtils.SUFFIX, result);
    }

    @Test
    public void generateFileNameWithSuffix() {
        String stringWithSuffix = "test_input_string_with_suffix.pdf";
        String result = StringFormattingUtils.generateFileName(stringWithSuffix);
        Assert.assertEquals(stringWithSuffix, result);
    }

    @Test
    public void generateFileNameWithOutInput() {
        String result = StringFormattingUtils.generateFileName(null);
        Assert.assertTrue(StringUtils.startsWithIgnoreCase(result, StringFormattingUtils.PREFIX));
        Assert.assertTrue(StringUtils.endsWithIgnoreCase(result, StringFormattingUtils.SUFFIX));
    }
}
