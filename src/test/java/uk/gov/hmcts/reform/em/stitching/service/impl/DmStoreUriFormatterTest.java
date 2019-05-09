package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.Assert;
import org.junit.Test;

public class DmStoreUriFormatterTest {

    private  String MOCK_BASE_URI = "http://test-dm-store-uri";

    private  String MOCK_DOC_DETAILS = "/documents/12345";

    private  String BINARY = "/binary";

    private  String MOCK_CORRUPTED_DOC_URI = "http://test-dm-store-uri:443/documents/12345";

    private DmStoreUriFormatter dmStoreUriFormatter = new DmStoreUriFormatter(MOCK_BASE_URI);

    @Test
    public void doesAppendBinary() {
        String result = dmStoreUriFormatter.formatDmStoreUri(MOCK_CORRUPTED_DOC_URI);
        Assert.assertEquals(MOCK_BASE_URI.concat(MOCK_DOC_DETAILS).concat(BINARY), result);
    }

    @Test
    public void doesNotAppendBinaryWhenAlreadyPresent() {
        String result = dmStoreUriFormatter.formatDmStoreUri(MOCK_CORRUPTED_DOC_URI.concat(BINARY));
        Assert.assertEquals(MOCK_BASE_URI.concat(MOCK_DOC_DETAILS).concat(BINARY), result);
    }

    @Test
    public void doesNotAppendBinaryWhenNoDocumentsInString() {
        String mockUri = MOCK_BASE_URI.concat("/12345");
        Assert.assertEquals(mockUri, dmStoreUriFormatter.formatDmStoreUri(mockUri));
    }
}
