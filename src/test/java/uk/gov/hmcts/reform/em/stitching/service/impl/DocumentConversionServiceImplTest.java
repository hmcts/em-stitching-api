package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.google.common.collect.Lists;
import okhttp3.MediaType;
import org.junit.Assert;
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
import uk.gov.hmcts.reform.em.stitching.conversion.PDFConverter;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.rest.TestSecurityConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
public class DocumentConversionServiceImplTest {

    @MockBean
    private PDFConverter pdfConverter;

    private DocumentConversionServiceImpl conversionService;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        conversionService = new DocumentConversionServiceImpl(
            Lists.newArrayList(pdfConverter)
        );
    }

    @Test(expected = IOException.class)
    public void noHandler() throws IOException {
        File file = new File("/tmp");
        BDDMockito.given(pdfConverter.accepts()).willReturn(Collections.emptyList());
        Pair<BundleDocument, FileAndMediaType> input = Pair.of(new BundleDocument(),
                new FileAndMediaType(file, MediaType.get("application/pdf")));

        conversionService.convert(input);
    }

    @Test
    public void converterFound() throws IOException {
        File inputFile = new File("/tmp");
        File expected = new File("/");
        BDDMockito.given(pdfConverter.accepts()).willReturn(Lists.newArrayList("application/pdf"));
        BDDMockito.given(pdfConverter.convert(inputFile)).willReturn(expected);
        Pair<BundleDocument, FileAndMediaType> input = Pair.of(new BundleDocument(),
                new FileAndMediaType(inputFile, MediaType.get("application/pdf")));
        Pair<BundleDocument, File> result = conversionService.convert(input);

        Assert.assertEquals(expected, result.getSecond());
    }
}
