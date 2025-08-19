package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.google.common.collect.Lists;
import okhttp3.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.conversion.FileToPDFConverter;
import uk.gov.hmcts.reform.em.stitching.conversion.PDFConverter;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.exception.DocmosisConversionException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentConversionServiceImplTest {

    @Mock
    private PDFConverter pdfConverter;

    private DocumentConversionServiceImpl conversionService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        conversionService = new DocumentConversionServiceImpl(
            Lists.newArrayList(pdfConverter)
        );
    }

    @Test
    void testConvertWithMissingHandlerAndThrowsIOException() {
        File file = new File("/tmp");
        BDDMockito.given(pdfConverter.accepts()).willReturn(Collections.emptyList());
        Pair<BundleDocument, FileAndMediaType> input = Pair.of(new BundleDocument(),
                new FileAndMediaType(file, MediaType.get("application/pdf")));

        assertThrows(DocmosisConversionException.class, () -> conversionService.convert(input));
    }

    @Test
    void converterFound() throws IOException {
        File inputFile = new File("/tmp");
        File expected = new File("/");
        BDDMockito.given(pdfConverter.accepts()).willReturn(Lists.newArrayList("application/pdf"));
        BDDMockito.given(pdfConverter.convert(inputFile)).willReturn(expected);
        Pair<BundleDocument, FileAndMediaType> input = Pair.of(new BundleDocument(),
                new FileAndMediaType(inputFile, MediaType.get("application/pdf")));
        Pair<BundleDocument, File> result = conversionService.convert(input);

        assertEquals(expected, result.getSecond());
    }

    @Test
    void shouldThrowIOExceptionWithExpectedMessage() {
        // Arrange
        FileToPDFConverter mockConverter = mock(FileToPDFConverter.class);
        when(mockConverter.accepts()).thenReturn(Lists.newArrayList("application/pdf"));
        DocumentConversionServiceImpl service =
                new DocumentConversionServiceImpl(Collections.singletonList(mockConverter));

        BundleDocument bundleDocument = mock(BundleDocument.class);
        when(bundleDocument.getDocTitle()).thenReturn("TestDoc");
        FileAndMediaType fileAndMediaType = new FileAndMediaType(new File("test.txt"), MediaType.get("text/plain"));
        Pair<BundleDocument, FileAndMediaType> input = Pair.of(bundleDocument, fileAndMediaType);

        // Act & Assert
        assertThatThrownBy(() -> service.convert(input))
                .isInstanceOf(DocmosisConversionException.class)
                .hasMessageContaining("Error converting document: TestDoc with file type: text/plain");
    }
}
