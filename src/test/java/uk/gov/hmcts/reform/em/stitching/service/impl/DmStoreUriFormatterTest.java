package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmStoreUriFormatterTest {

    private static final String MOCK_BASE_URI = "http://test-dm-store-uri";

    private final DmStoreUriFormatter dmStoreUriFormatter = new DmStoreUriFormatter(MOCK_BASE_URI);

    @Test
    void processesWhenDocumentsAndBinaryAreInString() {
        String mockDocumentId = UUID.randomUUID().toString();
        String mockDocumentDetails = "/documents/" + mockDocumentId + "/binary";
        String mockCorruptedDocumentUri = "http://test-dm-store-uri:443" + mockDocumentDetails;

        String result = dmStoreUriFormatter.formatDmStoreUri(mockCorruptedDocumentUri);

        assertEquals(MOCK_BASE_URI + mockDocumentDetails, result);
    }

    @Test
    void processesWhenDocumentsIsInString() {
        String mockDocumentId = UUID.randomUUID().toString();
        String mockDocumentDetails = "/documents/" + mockDocumentId;
        String mockCorruptedDocumentUri = "http://test-dm-store-uri:443" + mockDocumentDetails;

        String result = dmStoreUriFormatter.formatDmStoreUri(mockCorruptedDocumentUri);

        assertEquals(MOCK_BASE_URI + mockDocumentDetails, result);
    }

    @Test
    void processesWhenDocumentsNotInString() {
        String mockDocumentId = UUID.randomUUID().toString();
        String mockUri = MOCK_BASE_URI + "/" + mockDocumentId;

        String expectedUri = MOCK_BASE_URI + "/documents/" + mockDocumentId;

        String result = dmStoreUriFormatter.formatDmStoreUri(mockUri);

        assertEquals(expectedUri, result);
    }

    @Test
    void throwsIllegalArgumentExceptionWhenIdIsNotAValidUuid() {
        String mockCorruptedDocumentUri = "http://test-dm-store-uri:443/documents/12345";

        assertThrows(IllegalArgumentException.class, () ->
            dmStoreUriFormatter.formatDmStoreUri(mockCorruptedDocumentUri));
    }
}