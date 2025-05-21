package uk.gov.hmcts.reform.em.stitching.template;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocmosisClientWatermarkTest {

    private DocmosisClient client;
    private static final String RENDERED_WATERMARK_PDF_RESOURCE = "test-files/watermark_rendered.pdf";
    private static final String MOCK_ASSET_ID = "test-watermark-asset-id";

    @BeforeEach
    void setup() {
        OkHttpClient okHttpClient = new OkHttpClient
            .Builder()
            .addInterceptor(DocmosisClientWatermarkTest::interceptSuccessfulPdfResponse)
            .build();

        client = new DocmosisClient(okHttpClient);
        ReflectionTestUtils.setField(client, "docmosisRenderEndpoint", "http://example.org");
        ReflectionTestUtils.setField(client, "docmosisAccessKey", "key");
    }

    private static Response interceptSuccessfulPdfResponse(Interceptor.Chain chain) throws IOException {
        InputStream fileStream = ClassLoader.getSystemResourceAsStream(RENDERED_WATERMARK_PDF_RESOURCE);
        if (fileStream == null) {
            throw new IOException("Test resource not found: " + RENDERED_WATERMARK_PDF_RESOURCE);
        }
        byte[] pdfBytes = IOUtils.toByteArray(fileStream);
        fileStream.close();

        return new Response.Builder()
            .body(ResponseBody.create(pdfBytes, MediaType.get("application/pdf")))
            .request(chain.request())
            .message("OK")
            .code(200)
            .protocol(Protocol.HTTP_2)
            .build();
    }

    @Test
    void getDocmosisImageExtractsImageSuccessfully() throws IOException, DocumentTaskProcessingException {
        try (MockedStatic<Loader> mockedLoader = Mockito.mockStatic(Loader.class)) {
            PDDocument mockDocument = mock(PDDocument.class);
            PDPage mockPage = mock(PDPage.class);
            PDResources mockResources = mock(PDResources.class);
            PDImageXObject mockImageXObject = mock(PDImageXObject.class);
            COSName cosName = COSName.getPDFName("Im0");
            BufferedImage tinyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

            mockedLoader.when(() -> Loader.loadPDF(any(File.class))).thenReturn(mockDocument);
            when(mockDocument.getNumberOfPages()).thenReturn(1);
            when(mockDocument.getPage(0)).thenReturn(mockPage);
            when(mockPage.getResources()).thenReturn(mockResources);
            when(mockResources.getXObjectNames()).thenReturn(Collections.singleton(cosName));
            when(mockResources.getXObject(cosName)).thenReturn(mockImageXObject);
            when(mockImageXObject.getImage()).thenReturn(tinyImage);

            File output = client.getDocmosisImage(MOCK_ASSET_ID);

            assertNotEquals(MOCK_ASSET_ID, output.getName());
            assertTrue(output.length() > 0, "Extracted image file should not be empty");
            assertTrue(output.getName().endsWith(".png"), "Output file should be a PNG");

            verify(mockImageXObject).getImage();
            verify(mockDocument).close();
        }
    }

    @Test
    void getDocmosisImageReturnsEmptyFileWhenXobjectIsNotImage() throws IOException, DocumentTaskProcessingException {
        try (MockedStatic<Loader> mockedLoader = Mockito.mockStatic(Loader.class)) {
            PDDocument mockDocument = mock(PDDocument.class);
            PDPage mockPage = mock(PDPage.class);
            PDResources mockResources = mock(PDResources.class);
            PDXObject mockNonImageXObject = mock(PDXObject.class);
            COSName cosName = COSName.getPDFName("XObj1");

            mockedLoader.when(() -> Loader.loadPDF(any(File.class))).thenReturn(mockDocument);
            when(mockDocument.getNumberOfPages()).thenReturn(1);
            when(mockDocument.getPage(0)).thenReturn(mockPage);
            when(mockPage.getResources()).thenReturn(mockResources);
            when(mockResources.getXObjectNames()).thenReturn(Collections.singleton(cosName));
            when(mockResources.getXObject(cosName)).thenReturn(mockNonImageXObject);

            File output = client.getDocmosisImage(MOCK_ASSET_ID);

            assertEquals(0, output.length(), "Watermark file should be empty if XObject is not an image");
            verify(mockDocument).close();
        }
    }
}