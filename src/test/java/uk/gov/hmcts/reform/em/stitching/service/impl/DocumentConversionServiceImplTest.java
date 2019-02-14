package uk.gov.hmcts.reform.em.stitching.service.impl;

import com.google.common.collect.Lists;
import org.apache.tika.Tika;
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
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.conversion.PDFConverter;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DocumentConversionServiceImplTest {

    @MockBean
    private PDFConverter pdfConverter;

    @MockBean
    private Tika tika;

    private DocumentConversionServiceImpl conversionService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        conversionService = new DocumentConversionServiceImpl(
            Lists.newArrayList(pdfConverter),
            tika
        );
    }

    @Test(expected = IOException.class)
    public void noHandler() throws IOException {
        File file = new File("/tmp");
        BDDMockito.given(tika.detect(file)).willReturn("application/pdf");
        BDDMockito.given(pdfConverter.accepts()).willReturn(Collections.emptyList());
        Pair<BundleDocument, File> input = Pair.of(new BundleDocument(), file);

        conversionService.convert(input);
    }

    @Test
    public void converterFound() throws IOException {
        File inputFile = new File("/tmp");
        File expected = new File("/");
        BDDMockito.given(tika.detect(inputFile)).willReturn("application/pdf");
        BDDMockito.given(pdfConverter.accepts()).willReturn(Lists.newArrayList("application/pdf"));
        BDDMockito.given(pdfConverter.convert(inputFile)).willReturn(expected);
        Pair<BundleDocument, File> input = Pair.of(new BundleDocument(), inputFile);
        Pair<BundleDocument, File> result = conversionService.convert(input);

        Assert.assertEquals(expected, result.getSecond());
    }
}
