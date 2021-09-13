package uk.gov.hmcts.reform.em.stitching.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import okhttp3.MediaType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.info.BuildInfo;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFWatermark;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.service.CdamService;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.stitching.service.impl.FileAndMediaType;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;
import uk.gov.hmcts.reform.em.stitching.template.DocmosisClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class DocumentTaskItemProcessorTest {

    private static final String PDF_FILENAME = "annotationTemplate.pdf";
    private static final String COVER_PAGE_TEMPLATE = "FL-FRM-GOR-ENG-12345.pdf";

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

    @Mock
    CdamService cdamService;

    @MockBean
    private PDFMerger pdfMerger;

    @MockBean
    private DocmosisClient docmosisClient;

    @MockBean
    private PDFWatermark pdfWatermark;

    private DocumentTaskItemProcessor itemProcessor;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        Mockito
            .when(documentConverter.convert(any()))
            .then((Answer) invocation -> invocation.getArguments()[0]);

        itemProcessor = new DocumentTaskItemProcessor(
            dmStoreDownloader,
            dmStoreUploader,
            documentConverter,
            pdfMerger,
            docmosisClient,
            pdfWatermark,
            cdamService
        );
    }

    @Test
    public void usesCoverPageRender() throws IOException, DocumentTaskProcessingException {
        final File coverPageFile = new File(ClassLoader.getSystemResource(COVER_PAGE_TEMPLATE).getPath());
        final JsonNode coverPageData = JsonNodeFactory.instance.objectNode().put("caseNo", "12345");

        DocumentTask documentTaskWithCoversheet = new DocumentTask();
        documentTaskWithCoversheet.setTaskState(TaskState.NEW);

        Bundle testBundle = BundleTest.getTestBundle();
        testBundle.setHasCoversheets(true);
        testBundle.setCoverpageTemplate(COVER_PAGE_TEMPLATE);
        testBundle.setCoverpageTemplateData(coverPageData);

        documentTaskWithCoversheet.setBundle(testBundle);

        URL url = ClassLoader.getSystemResource(PDF_FILENAME);

        File file = new File(url.getFile());

        Pair<BundleDocument, FileAndMediaType> mockPair =
                Pair.of(testBundle.getDocuments().get(0),
                        new FileAndMediaType(file, MediaType.get("application/pdf")));

        Pair<BundleDocument, File> convertedMockPair = Pair.of(testBundle.getDocuments().get(0), file);

        BDDMockito.given(dmStoreDownloader.downloadFiles(any())).willReturn(Stream.of(mockPair));
        BDDMockito.given(documentConverter.convert(any())).willReturn(convertedMockPair);
        BDDMockito.given(docmosisClient.renderDocmosisTemplate(eq(COVER_PAGE_TEMPLATE), eq(coverPageData))).willReturn(coverPageFile);

        itemProcessor.process(documentTaskWithCoversheet);

        verify(docmosisClient, times(1)).renderDocmosisTemplate(eq(COVER_PAGE_TEMPLATE), eq(coverPageData));
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
    public void testStitch() throws DocumentTaskProcessingException, IOException {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());

        File file = mock(File.class);

        URL url = ClassLoader.getSystemResource(PDF_FILENAME);

        Pair<BundleDocument, FileAndMediaType> pair1 = Pair.of(documentTask.getBundle().getDocuments().get(0),
                new FileAndMediaType(new File(url.getFile()), MediaType.get("application/pdf")));
        Pair<BundleDocument, FileAndMediaType> pair2 = Pair.of(documentTask.getBundle().getDocuments().get(1),
                new FileAndMediaType(new File(url.getFile()), MediaType.get("application/pdf")));
        Stream<Pair<BundleDocument, FileAndMediaType>> files = Stream.of(pair1, pair2);

        Pair<BundleDocument, File> convertedMockPair1 = Pair.of(documentTask.getBundle().getDocuments().get(0), file);
        Pair<BundleDocument, File> convertedMockPair2 = Pair.of(documentTask.getBundle().getDocuments().get(1), file);

        Mockito
            .when(dmStoreDownloader.downloadFiles(any()))
            .thenReturn(files);

        Mockito
            .doAnswer(any -> {
                documentTask.getBundle().setStitchedDocumentURI("/derp");

                return documentTask;
            })
            .when(dmStoreUploader).uploadFile(any(), any());

        BDDMockito.given(documentConverter.convert(eq(pair1))).willReturn(convertedMockPair1);
        BDDMockito.given(documentConverter.convert(eq(pair2))).willReturn(convertedMockPair2);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(any(), eq(convertedMockPair1), eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair1);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(any(), eq(convertedMockPair2), eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair2);

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }

    @Test
    public void testStitchDocumentImageNull() throws DocumentTaskProcessingException, IOException {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());
        documentTask.getBundle().setDocumentImage(null);

        File file = mock(File.class);

        URL url = ClassLoader.getSystemResource(PDF_FILENAME);

        Pair<BundleDocument, FileAndMediaType> pair1 = Pair.of(documentTask.getBundle().getDocuments().get(0),
                new FileAndMediaType(new File(url.getFile()), MediaType.get("application/pdf")));
        Pair<BundleDocument, FileAndMediaType> pair2 = Pair.of(documentTask.getBundle().getDocuments().get(1),
                new FileAndMediaType(new File(url.getFile()), MediaType.get("application/pdf")));
        Stream<Pair<BundleDocument, FileAndMediaType>> files = Stream.of(pair1, pair2);

        Pair<BundleDocument, File> convertedMockPair1 = Pair.of(documentTask.getBundle().getDocuments().get(0), file);
        Pair<BundleDocument, File> convertedMockPair2 = Pair.of(documentTask.getBundle().getDocuments().get(1), file);

        Mockito
                .when(dmStoreDownloader.downloadFiles(any()))
                .thenReturn(files);

        Mockito
                .doAnswer(any -> {
                    documentTask.getBundle().setStitchedDocumentURI("/derp");

                    return documentTask;
                })
                .when(dmStoreUploader).uploadFile(any(), any());

        BDDMockito.given(documentConverter.convert(eq(pair1))).willReturn(convertedMockPair1);
        BDDMockito.given(documentConverter.convert(eq(pair2))).willReturn(convertedMockPair2);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(any(), eq(convertedMockPair1), eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair1);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(any(), eq(convertedMockPair2), eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair2);

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }

    @Test
    public void testStitchDocumentImageAssetIdNull() throws DocumentTaskProcessingException, IOException {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());
        documentTask.getBundle().getDocumentImage().setDocmosisAssetId(null);

        File file = mock(File.class);

        URL url = ClassLoader.getSystemResource(PDF_FILENAME);

        Pair<BundleDocument, FileAndMediaType> pair1 = Pair.of(documentTask.getBundle().getDocuments().get(0),
                new FileAndMediaType(new File(url.getFile()), MediaType.get("application/pdf")));
        Pair<BundleDocument, FileAndMediaType> pair2 = Pair.of(documentTask.getBundle().getDocuments().get(1),
                new FileAndMediaType(new File(url.getFile()), MediaType.get("application/pdf")));
        Stream<Pair<BundleDocument, FileAndMediaType>> files = Stream.of(pair1, pair2);

        Pair<BundleDocument, File> convertedMockPair1 = Pair.of(documentTask.getBundle().getDocuments().get(0), file);
        Pair<BundleDocument, File> convertedMockPair2 = Pair.of(documentTask.getBundle().getDocuments().get(1), file);

        Mockito
                .when(dmStoreDownloader.downloadFiles(any()))
                .thenReturn(files);

        Mockito
                .doAnswer(any -> {
                    documentTask.getBundle().setStitchedDocumentURI("/derp");

                    return documentTask;
                })
                .when(dmStoreUploader).uploadFile(any(), any());

        Mockito
                .when(docmosisClient.getDocmosisImage(eq(documentTask.getBundle().getDocumentImage().getDocmosisAssetId())))
                .thenReturn(file);

        BDDMockito.given(documentConverter.convert(eq(pair1))).willReturn(convertedMockPair1);
        BDDMockito.given(documentConverter.convert(eq(pair2))).willReturn(convertedMockPair2);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(any(), eq(convertedMockPair1), eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair1);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(any(), eq(convertedMockPair2), eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair2);

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }
}
