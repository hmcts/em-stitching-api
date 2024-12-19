package uk.gov.hmcts.reform.em.stitching.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.EntityManager;
import okhttp3.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFWatermark;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DocumentTaskItemProcessorTest {

    private static final String PDF_FILENAME = "test-files/annotationTemplate.pdf";
    private static final String COVER_PAGE_TEMPLATE = "test-files/FL-FRM-GOR-ENG-12345.pdf";

    @Mock
    DmStoreDownloader dmStoreDownloader;

    @Mock
    DmStoreUploader dmStoreUploader;

    @Mock
    DocumentConversionService documentConverter;

    @Mock
    DocumentTaskMapper documentTaskMapper;

    @Mock
    CdamService cdamService;

    @MockBean
    private PDFMerger pdfMerger;

    @MockBean
    private DocmosisClient docmosisClient;

    @MockBean
    private PDFWatermark pdfWatermark;

    @Mock
    EntityManager entityManager;

    StoreDocumentTaskRetryCount storeDocumentTaskRetryCount;

    private DocumentTaskItemProcessor itemProcessor;

    @BeforeEach
    public void setup() throws IOException {
        MockitoAnnotations.openMocks(this);
        Mockito
            .when(documentConverter.convert(any()))
            .then((Answer) invocation -> invocation.getArguments()[0]);

        Mockito
                .when(entityManager.merge(any()))
                .then((Answer) invocation -> invocation.getArguments()[0]);

        storeDocumentTaskRetryCount  = new StoreDocumentTaskRetryCount(entityManager);

        itemProcessor = new DocumentTaskItemProcessor(
                dmStoreDownloader,
                dmStoreUploader,
                documentConverter,
                pdfMerger,
                docmosisClient,
                pdfWatermark,
                cdamService,
                storeDocumentTaskRetryCount
        );
    }

    @Test
    void usesCoverPageRender() throws IOException, DocumentTaskProcessingException {
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

        when(dmStoreDownloader.downloadFiles(any())).thenReturn(Stream.of(mockPair));
        when(documentConverter.convert(any())).thenReturn(convertedMockPair);
        when(docmosisClient.renderDocmosisTemplate(COVER_PAGE_TEMPLATE,
                coverPageData)).thenReturn(coverPageFile);

        itemProcessor.process(documentTaskWithCoversheet);
        verify(entityManager).merge(documentTaskWithCoversheet);
        verify(entityManager).flush();
        verify(docmosisClient, times(1)).renderDocmosisTemplate(COVER_PAGE_TEMPLATE, coverPageData);
    }

    @Test
    void testFailure() throws Exception {
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
    void testStitch() throws DocumentTaskProcessingException, IOException {
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

        when(documentConverter.convert(pair1)).thenReturn(convertedMockPair1);
        when(documentConverter.convert(pair2)).thenReturn(convertedMockPair2);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(
                        any(),
                        eq(convertedMockPair1),
                        eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair1);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(
                        any(),
                        eq(convertedMockPair2),
                        eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair2);

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }

    @Test
    void testCdamStitch() throws DocumentTaskProcessingException, IOException {
        DocumentTask documentTask = new DocumentTask();
        documentTask.setBundle(BundleTest.getTestBundle());
        documentTask.setJurisdictionId("PUBLICLAW");
        documentTask.setCaseTypeId("DUMMY");

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
            .when(cdamService.downloadFiles(any()))
            .thenReturn(files);

        Mockito
            .doAnswer(any -> {
                documentTask.getBundle().setStitchedDocumentURI("/derp");
                documentTask.getBundle().setHashToken("XYZ");
                return documentTask;
            })
            .when(cdamService).uploadDocuments(any(), any());

        when(documentConverter.convert(pair1)).thenReturn(convertedMockPair1);
        when(documentConverter.convert(pair2)).thenReturn(convertedMockPair2);

        Mockito
            .when(pdfWatermark.processDocumentWatermark(
                    any(),
                    eq(convertedMockPair1),
                    eq(documentTask.getBundle().getDocumentImage())))
            .thenReturn(convertedMockPair1);

        Mockito
            .when(pdfWatermark.processDocumentWatermark(
                    any(),
                    eq(convertedMockPair2),
                    eq(documentTask.getBundle().getDocumentImage())))
            .thenReturn(convertedMockPair2);

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }

    @Test
    void testStitchDocumentImageNull() throws DocumentTaskProcessingException, IOException {
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

        when(documentConverter.convert(pair1)).thenReturn(convertedMockPair1);
        when(documentConverter.convert(pair2)).thenReturn(convertedMockPair2);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(
                        any(),
                        eq(convertedMockPair1),
                        eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair1);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(
                        any(),
                        eq(convertedMockPair2),
                        eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair2);

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }

    @Test
    void testStitchDocumentImageAssetIdNull() throws DocumentTaskProcessingException, IOException {
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
                .when(docmosisClient.getDocmosisImage(
                        documentTask.getBundle().getDocumentImage().getDocmosisAssetId()))
                .thenReturn(file);

        when(documentConverter.convert(pair1)).thenReturn(convertedMockPair1);
        when(documentConverter.convert(pair2)).thenReturn(convertedMockPair2);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(
                        any(),
                        eq(convertedMockPair1),
                        eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair1);

        Mockito
                .when(pdfWatermark.processDocumentWatermark(
                        any(),
                        eq(convertedMockPair2),
                        eq(documentTask.getBundle().getDocumentImage())))
                .thenReturn(convertedMockPair2);

        itemProcessor.process(documentTask);

        assertNull(documentTask.getFailureDescription());
        assertNotEquals(null, documentTask.getBundle().getStitchedDocumentURI());
        assertEquals(TaskState.DONE, documentTask.getTaskState());
    }
}
