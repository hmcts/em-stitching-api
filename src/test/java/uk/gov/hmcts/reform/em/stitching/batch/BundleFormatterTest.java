package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

import java.io.File;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.hmcts.reform.em.stitching.batch.BundleFormatter.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class BundleFormatterTest {

    private static final File TEST_FILE= new File("TEST_FILE.pdf");
    private static PDDocument document;

    @Before
    public void setup() throws IOException {
        document = PDDocument.load(TEST_FILE);

    }



    @Test
    public void addEmptyLastPagePageCountTest() {
        int beforeCount = document.getNumberOfPages();
        Assert.assertEquals(beforeCount, 2);
        addEmptyLastPage(document);
        int afterCount = document.getNumberOfPages();
        Assert.assertEquals(beforeCount + 1, afterCount);
    }

    @Test
    public void moveLastPageToFirstTest() throws Exception {
        addEmptyLastPage(document);
        moveLastPageToFirst(document);

        File SAVE_FILE= File.createTempFile("test_coversheet", ".pdf");
        document.save(SAVE_FILE);
        document.close();
    }

    @Test
    public void addCoversheetTextToFirstPageTest() throws Exception {
        addEmptyLastPage(document);
        moveLastPageToFirst(document);
        addCoversheetTextToFirstPage(document, "test_document's name");

        File SAVE_FILE= File.createTempFile("test_coversheet", ".pdf");
        document.save(SAVE_FILE);
        document.close();
    }

}