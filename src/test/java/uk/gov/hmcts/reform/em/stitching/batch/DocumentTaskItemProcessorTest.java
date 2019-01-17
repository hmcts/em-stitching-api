package uk.gov.hmcts.reform.em.stitching.batch;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DocumentTaskItemProcessorTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    @Mock
    DmStoreDownloader dmStoreDownloader;

    @Mock
    DmStoreUploader dmStoreUploader;

    private PDFMergerFactory pdfMergerFactory = new PDFMergerFactory();

    @Test
    public void testFailure() throws Exception {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());

        Mockito
            .when(dmStoreDownloader.downloadFiles(documentTask.getBundle().getDocuments()))
            .thenThrow(new DocumentTaskProcessingException("problem"));

        DocumentTaskItemProcessor documentTaskItemProcessor = new DocumentTaskItemProcessor(dmStoreDownloader, null, pdfMergerFactory);

        documentTaskItemProcessor.process(documentTask);

        Assert.assertEquals("problem", documentTask.getFailureDescription());
        Assert.assertEquals(TaskState.FAILED, documentTask.getTaskState());
    }

    @Test
    public void testStitch() throws DocumentTaskProcessingException {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());

        List<BundleDocument> documents = documentTask.getBundle().getDocuments();
        URL url = ClassLoader.getSystemResource(PDF_FILENAME);
        Stream<File> files = Stream.of(new File(url.getFile()), new File(url.getFile()));

        Mockito
            .when(dmStoreDownloader.downloadFiles(documents))
            .thenReturn(files);

        Mockito
            .doNothing()
            .when(dmStoreUploader).uploadFile(any(), any());

        DocumentTaskItemProcessor itemProcessor = new DocumentTaskItemProcessor(dmStoreDownloader, dmStoreUploader, pdfMergerFactory);
        itemProcessor.process(documentTask);

        Assert.assertEquals(null, documentTask.getFailureDescription());
        Assert.assertNotEquals(null, documentTask.getOutputDocumentId());
        Assert.assertEquals(TaskState.DONE, documentTask.getTaskState());
        Assert.assertTrue(new File(documentTask.getOutputDocumentId()).exists());
    }


}