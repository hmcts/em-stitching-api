package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFCoversheetService;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class DocumentTaskServiceImplTest {

    @MockBean
    private DocumentTaskRepository documentTaskRepository;

    @MockBean
    private DocumentTaskMapper documentTaskMapper;

    @MockBean
    private DmStoreDownloader dmStoreDownloader;

    @MockBean
    private DmStoreUploader dmStoreUploader;

    @MockBean
    private DocumentConversionService documentConverter;

    @MockBean
    private PDFCoversheetService coversheetService;

    @MockBean
    private PDFMerger pdfMerger;

    private static final String PDF_FILENAME = "annotationTemplate.pdf";

    private DocumentTaskServiceImpl documentTaskService;

    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath()
    );

    private static final File FILE_2 = new File(
            ClassLoader.getSystemResource("annotationTemplate.pdf").getPath()
    );

    private List<Pair<BundleDocument, File>> documents;
    private Pair<BundleDocument, File> document1;
    private Pair<BundleDocument, File> document2;

    @Before
    public void setup() throws DocumentTaskProcessingException, IOException {
        MockitoAnnotations.initMocks(this);

        documentTaskService = new DocumentTaskServiceImpl(
            documentTaskRepository,
            documentTaskMapper,
            dmStoreDownloader,
            dmStoreUploader,
            documentConverter,
            coversheetService,
            pdfMerger
        );
    }

    @Test
    public void usesCoversheetService() throws IOException, DocumentTaskProcessingException {
        DocumentTask documentTaskWithCoversheet = new DocumentTask();
        documentTaskWithCoversheet.setTaskState(TaskState.NEW);

        Bundle testBundle = BundleTest.getTestBundle();
        testBundle.setHasCoversheets(true);

        documentTaskWithCoversheet.setBundle(testBundle);

        URL url = ClassLoader.getSystemResource(PDF_FILENAME);

        Pair<BundleDocument, File> mockPair = Pair.of(testBundle.getDocuments().get(0), new File(url.getFile()));

        BDDMockito.given(dmStoreDownloader.downloadFiles(any())).willReturn(Stream.of(mockPair));
        BDDMockito.given(documentConverter.convert(any())).willReturn(mockPair);

        documentTaskService.process(documentTaskWithCoversheet);

        verify(coversheetService, times(1)).addCoversheet(any());
    }

    @Test
    public void doesNotUseCoversheetService() throws IOException, DocumentTaskProcessingException {
        DocumentTask documentTaskWithCoversheet = new DocumentTask();
        documentTaskWithCoversheet.setTaskState(TaskState.NEW);

        Bundle testBundle = BundleTest.getTestBundle();
        testBundle.setHasCoversheets(false);

        documentTaskWithCoversheet.setBundle(testBundle);

        URL url = ClassLoader.getSystemResource(PDF_FILENAME);

        Pair<BundleDocument, File> mockPair = Pair.of(testBundle.getDocuments().get(0), new File(url.getFile()));

        BDDMockito.given(dmStoreDownloader.downloadFiles(any())).willReturn(Stream.of(mockPair));
        BDDMockito.given(documentConverter.convert(any())).willReturn(mockPair);

        documentTaskService.process(documentTaskWithCoversheet);
        verify(coversheetService, times(0)).addCoversheet(any());
    }

}
