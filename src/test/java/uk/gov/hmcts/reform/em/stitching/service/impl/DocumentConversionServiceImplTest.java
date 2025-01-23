package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.google.common.collect.Lists;
import okhttp3.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.conversion.PDFConverter;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.rest.TestSecurityConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class DocumentConversionServiceImplTest {

    @MockitoBean
    private PDFConverter pdfConverter;

    private DocumentConversionServiceImpl conversionService;

    @BeforeEach
    public void setup() {
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

        assertThrows(IOException.class, () -> conversionService.convert(input));
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
}
