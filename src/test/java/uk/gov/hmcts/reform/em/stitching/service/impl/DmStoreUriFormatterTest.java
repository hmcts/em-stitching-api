package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DmStoreUriFormatterTest {

    private static final String mockBaseUri = "http://test-dm-store-uri";

    private final DmStoreUriFormatter dmStoreUriFormatter = new DmStoreUriFormatter(mockBaseUri);

    @Test
    void processesWhenDocumentsIsInString() {
        String mockDocumentDetails = "/documents/12345";
        String mockCorruptedDocumentUri = "http://test-dm-store-uri:443/documents/12345";
        String result = dmStoreUriFormatter.formatDmStoreUri(mockCorruptedDocumentUri);
        assertEquals(mockBaseUri + mockDocumentDetails, result);
    }

    @Test
    void doesNotProcessWhenDocumentsNotInString() {
        String mockUri = mockBaseUri + "/12345";
        assertEquals(mockUri, dmStoreUriFormatter.formatDmStoreUri(mockUri));
    }
}
