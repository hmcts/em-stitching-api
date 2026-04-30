package uk.gov.hmcts.reform.em.stitching.domain.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallableEndpointValidatorTest {

    private CallableEndpointValidator validator;
    private ConstraintValidatorContext mockContext;

    private static final String PROD_URL = "PRODURL";
    private static final String PROD = "PROD";
    private static final String VALID_PROD_URL = "http://PRODURL:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";

    @BeforeEach
    void setUp() {
        validator = new CallableEndpointValidator("localhost", "DEV");
        mockContext = null;
    }

    @Test
    void isValidReturnsTrueForPerfectMatch() {
        String validUrl = "http://localhost.dummy:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        assertTrue(validator.isValid(validUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForWrongHost() {
        String invalidUrl = "https://wrong-host:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        assertFalse(validator.isValid(invalidUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForNullOrBlank() {
        assertFalse(validator.isValid(null, mockContext));
        assertFalse(validator.isValid("", mockContext));
        assertFalse(validator.isValid("   ", mockContext));
    }

    @Test
    void isValidReturnsTrueForProdPerfectMatch() {
        validator = getValidator(PROD_URL, PROD);
        assertTrue(validator.isValid(VALID_PROD_URL, mockContext));
    }

    @Test
    void isValidReturnsTrueForProdNonPerfectMatch() {
        validator = getValidator(PROD_URL, PROD);
        String validUrl = "http://PRODURLNONMATCH:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        assertFalse(validator.isValid(validUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseWhenUrlIsMalformed() {
        validator = getValidator("some-host", PROD);
        String malformedUrl = "http://[invalid-url"; // invalid URI that will throw
        assertFalse(validator.isValid(malformedUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseWhenHostIsBlank() {
        validator = getValidator(" ", PROD);
        assertFalse(validator.isValid(VALID_PROD_URL, mockContext));
    }

    @Test
    void isValidReturnsFalseWhenOneOfHostIsEmpty() {
        validator = getValidator("dummy, ", PROD);
        assertFalse(validator.isValid(VALID_PROD_URL, mockContext));
    }

    private static @NonNull CallableEndpointValidator getValidator(String hosts, String env) {
        return new CallableEndpointValidator(hosts, env);
    }
}