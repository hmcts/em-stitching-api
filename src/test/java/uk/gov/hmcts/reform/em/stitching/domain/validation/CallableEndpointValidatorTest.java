package uk.gov.hmcts.reform.em.stitching.domain.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallableEndpointValidatorTest {

    private CallableEndpointValidator validator;
    private ConstraintValidatorContext mockContext;

    @BeforeEach
    void setUp() {
        // use empty hostsCsv to keep backward-compatible single-host behaviour
        validator = new CallableEndpointValidator("http", "", "localhost", 8080);
        mockContext = null;
    }

    @Test
    void isValidReturnsTrueForPerfectMatch() {
        String validUrl = "http://localhost:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        assertTrue(validator.isValid(validUrl, mockContext));
    }

    @Test
    void isValidReturnsTrueWhenPortIsOmittedByConfig() {
        CallableEndpointValidator noPortValidator = new CallableEndpointValidator("https", "", "my-domain.com", -1);

        String validUrl = "https://my-domain.com/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        assertTrue(noPortValidator.isValid(validUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForInvalidCaseId() {
        String invalidUrl = "http://localhost:8080/api/stitching-complete-callback/123456789012345/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        assertFalse(validator.isValid(invalidUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForInvalidTriggerId() {
        String invalidUrl = "http://localhost:8080/api/stitching-complete-callback/1234567890123456/wrongTriggerId/123e4567-e89b-12d3-a456-426614174000";
        assertFalse(validator.isValid(invalidUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForInvalidBundleId() {
        String invalidUrl = "http://localhost:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/not-a-uuid";
        assertFalse(validator.isValid(invalidUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForWrongHostOrScheme() {
        String invalidUrl = "https://wrong-host:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        assertFalse(validator.isValid(invalidUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForAppendedPaths() {
        String invalidUrl = "http://localhost:8080/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000/extra";
        assertFalse(validator.isValid(invalidUrl, mockContext));
    }

    @Test
    void isValidReturnsFalseForNullOrBlank() {
        assertFalse(validator.isValid(null, mockContext));
        assertFalse(validator.isValid("", mockContext));
        assertFalse(validator.isValid("   ", mockContext));
    }

    @Test
    void isValidAcceptsPrHostsWithAnyDigits() {
        String hosts = "em-ccdorc-staging.aat.platform.hmcts.net,em-ccdorc-pr-{digits}.preview.platform.hmcts.net";
        CallableEndpointValidator multiHostValidator = new CallableEndpointValidator("https", hosts, "", -1);

        String url1 = "https://em-ccdorc-pr-1234.preview.platform.hmcts.net/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        String url2 = "https://em-ccdorc-pr-999.preview.platform.hmcts.net/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";
        String url3 = "https://em-ccdorc-staging.aat.platform.hmcts.net/api/stitching-complete-callback/1234567890123456/asyncStitchingComplete/123e4567-e89b-12d3-a456-426614174000";

        assertTrue(multiHostValidator.isValid(url1, null));
        assertTrue(multiHostValidator.isValid(url2, null));
        assertTrue(multiHostValidator.isValid(url3, null));
    }
}