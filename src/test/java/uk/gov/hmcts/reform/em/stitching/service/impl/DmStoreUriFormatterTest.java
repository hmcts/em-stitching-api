package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.Assert;
import org.junit.Test;

public class DmStoreUriFormatterTest {

    private final String mockBaseUri = "http://test-dm-store-uri";

    private final String mockDocumentDetails = "/documents/12345";

    private final String binary = "/binary";

    private final String mockCorruptedDocumentUri = "http://test-dm-store-uri:443/documents/12345";

    private DmStoreUriFormatter dmStoreUriFormatter = new DmStoreUriFormatter(mockBaseUri);

    @Test
    public void doesAppendBinary() {
        String result = dmStoreUriFormatter.formatDmStoreUri(mockCorruptedDocumentUri);
        Assert.assertEquals((mockBaseUri + mockDocumentDetails + binary), result);
    }

    @Test
    public void doesNotAppendBinaryWhenAlreadyPresent() {
        String result = dmStoreUriFormatter.formatDmStoreUri(mockCorruptedDocumentUri + binary);
        Assert.assertEquals((mockBaseUri + mockDocumentDetails + binary), result);
    }

    @Test
    public void doesNotAppendBinaryWhenNoDocumentsInString() {
        String mockUri = (mockBaseUri + "/12345");
        Assert.assertEquals(mockUri, dmStoreUriFormatter.formatDmStoreUri(mockUri));
    }
}
