package uk.gov.hmcts.reform.em.stitching.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentImage;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.em.stitching.pdf.PDFMergerTestUtil.createFlatTestBundle;

class PDFWatermarkTest {
    private static final File WATERMARK_FILE = new File(
            ClassLoader.getSystemResource("test-files/schmcts.png").getPath()
    );

    private static final File FILE_1 = new File(
            ClassLoader.getSystemResource("test-files/TEST_INPUT_FILE.pdf").getPath()
    );

    private Bundle bundle;
    private DocumentImage documentImage;

    @BeforeEach
    public void setUp() {
        bundle = createFlatTestBundle();
        documentImage = new DocumentImage();
        documentImage.setDocmosisAssetId("test-files/schmcts.png");
        documentImage.setImageRendering(ImageRendering.OPAQUE);
        documentImage.setImageRenderingLocation(ImageRenderingLocation.ALL_PAGES);
    }

    @Test
    void processDocumentWatermarkInvalidCoordinatesTest() {
        Pair<BundleDocument, File> document = Pair.of(bundle.getDocuments().get(0), FILE_1);
        documentImage.setCoordinateX(1000);
        documentImage.setCoordinateY(-1);

        PDFWatermark pdfWatermark = new PDFWatermark();
        pdfWatermark.processDocumentWatermark(WATERMARK_FILE, document, documentImage);

        assertEquals(Integer.valueOf(100), documentImage.getCoordinateX());
        assertEquals(Integer.valueOf(0), documentImage.getCoordinateY());
    }

    @Test
    void processDocumentWatermarkNullCoordinatesTest() {
        Pair<BundleDocument, File> document = Pair.of(bundle.getDocuments().get(0), FILE_1);

        PDFWatermark pdfWatermark = new PDFWatermark();
        pdfWatermark.processDocumentWatermark(WATERMARK_FILE, document, documentImage);

        assertNull(documentImage.getCoordinateX());
        assertNull(documentImage.getCoordinateY());
    }

    @Test
    void processDocumentWatermarkDocumentImageFileNull() {
        Pair<BundleDocument, File> document = Pair.of(bundle.getDocuments().get(0), FILE_1);

        PDFWatermark pdfWatermark = new PDFWatermark();
        Pair<BundleDocument, File> result = pdfWatermark.processDocumentWatermark(null, document, documentImage);

        assertEquals(document, result);
    }

    @Test
    void processDocumentWatermarkDocumentImageError() {
        Pair<BundleDocument, File> document = Pair.of(bundle.getDocuments().get(0), FILE_1);
        documentImage.setImageRenderingLocation(ImageRenderingLocation.FIRST_PAGE);

        PDFWatermark pdfWatermark = new PDFWatermark();
        Pair<BundleDocument, File> result =
                pdfWatermark.processDocumentWatermark(WATERMARK_FILE, document, documentImage);

        assertEquals(document, result);
    }

    @Test
    void processDocumentWatermarkError() {
        Pair<BundleDocument, File> document = Pair.of(bundle.getDocuments().get(0), FILE_1);
        DocumentImage image = mock(DocumentImage.class);
        when(image.getImageRenderingLocation()).thenThrow(NullPointerException.class);

        PDFWatermark pdfWatermark = new PDFWatermark();
        Pair<BundleDocument, File> result =
                pdfWatermark.processDocumentWatermark(WATERMARK_FILE, document, image);

        assertEquals(document, result);
    }

    @Test
    void processDocumentWatermarkDocumentImageFileError() {
        Pair<BundleDocument, File> document = Pair.of(bundle.getDocuments().get(0), new File(""));

        PDFWatermark pdfWatermark = new PDFWatermark();
        Pair<BundleDocument, File> result =
                pdfWatermark.processDocumentWatermark(WATERMARK_FILE, document, documentImage);

        assertEquals(document, result);
    }
}
