package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.service.StringFormattingUtils;

public class StringFormattingUtilsTest {

    private final String suffix = ".suffix";

    @Test
    public void doesAppendSuffix() {
        String stringWithoutSuffix = "test_input_string_without_suffix";
        String result = StringFormattingUtils.ensureStringEndsWithSuffix(stringWithoutSuffix, suffix);
        Assert.assertEquals((stringWithoutSuffix + suffix), result);
    }

    @Test
    public void doesNotAppendSuffix() {
        String stringWithSuffix = "test_input_string_with_suffix.suffix";
        String result = StringFormattingUtils.ensureStringEndsWithSuffix(stringWithSuffix, suffix);
        Assert.assertEquals(stringWithSuffix, result);
    }

}
