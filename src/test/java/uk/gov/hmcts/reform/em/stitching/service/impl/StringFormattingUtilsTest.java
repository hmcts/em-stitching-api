package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.stitching.service.StringFormattingUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringFormattingUtilsTest {

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

    @Test
    void testPrivateConstructorThrowsException() throws Exception {
        Constructor<StringFormattingUtils> constructor = StringFormattingUtils.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
        constructor.setAccessible(true);
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, constructor::newInstance, "InvocationTargetException should be thrown");
        Throwable cause = thrown.getCause();
        assertNotNull(cause, "Cause should not be null");
        assertInstanceOf(UnsupportedOperationException.class, cause, "Cause should be UnsupportedOperationException");
        assertEquals("This is a utility class and cannot be instantiated", cause.getMessage());
    }
}
