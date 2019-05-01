package uk.gov.hmcts.reform.em.stitching.batch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.info.BuildInfo;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFCoversheetService;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskServiceImpl;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;
import org.springframework.data.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DocumentTaskItemProcessorTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    @Mock
    DmStoreDownloader dmStoreDownloader;

    @Mock
    DmStoreUploader dmStoreUploader;

    @Mock
    DocumentTaskRepository documentTaskRepository;

    @Mock
    DocumentConversionService documentConverter;

    @Mock
    DocumentTaskMapper documentTaskMapper;

    @Mock
    BuildInfo buildInfo;

    private DocumentTaskItemProcessor itemProcessor;

    @Before
    public void setup() throws IOException {
        Mockito
            .when(documentConverter.convert(any()))
            .then((Answer) invocation -> invocation.getArguments()[0]);

        DocumentTaskService documentTaskService = new DocumentTaskServiceImpl(
                documentTaskRepository,
                documentTaskMapper,
                dmStoreDownloader,
                dmStoreUploader,
                documentConverter,
                new PDFCoversheetService(),
                new PDFMerger(),
                buildInfo
        );

        itemProcessor = new DocumentTaskItemProcessor(documentTaskService);
    }

    @Test
    public void testFailure() throws Exception {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());

        Mockito
            .when(dmStoreDownloader.downloadFiles(any()))
            .thenThrow(new DocumentTaskProcessingException("problem"));

        itemProcessor.process(documentTask);

        assertEquals("problem", documentTask.getFailureDescription());
        assertEquals(TaskState.FAILED, documentTask.getTaskState());
    }

    @Test
    public void testStitch() throws DocumentTaskProcessingException {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());

        URL url = ClassLoader.getSystemResource(PDF_FILENAME);

        Pair<BundleDocument, File> pair1 = Pair.of(documentTask.getBundle().getDocuments().get(0), new File(url.getFile()));
        Pair<BundleDocument, File> pair2 = Pair.of(documentTask.getBundle().getDocuments().get(1), new File(url.getFile()));
        Stream<Pair<BundleDocument, File>> files = Stream.of(pair1, pair2);

        Mockito
            .when(dmStoreDownloader.downloadFiles(any()))
            .thenReturn(files);

        Mockito
            .doAnswer(any -> {
                documentTask.getBundle().setStitchedDocumentURI("/derp");

                return documentTask;
            })
            .when(dmStoreUploader).uploadFile(any(), any());

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }


}
