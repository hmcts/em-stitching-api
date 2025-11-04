package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DmStoreUriFormatterTest {

    private static final String MOCK_BASE_URI = "http://test-dm-store-uri";

    private final DmStoreUriFormatter dmStoreUriFormatter = new DmStoreUriFormatter(MOCK_BASE_URI);

    @Test
    void processesWhenDocumentsIsInString() {
        String mockDocumentDetails = "/documents/12345";
        String mockCorruptedDocumentUri = "http://test-dm-store-uri:443/documents/12345";
        String result = dmStoreUriFormatter.formatDmStoreUri(mockCorruptedDocumentUri);
        assertEquals(MOCK_BASE_URI + mockDocumentDetails, result);
    }

    @Test
    void doesNotProcessWhenDocumentsNotInString() {
        String mockUri = MOCK_BASE_URI + "/12345";
        assertEquals(mockUri, dmStoreUriFormatter.formatDmStoreUri(mockUri));
    }
}
