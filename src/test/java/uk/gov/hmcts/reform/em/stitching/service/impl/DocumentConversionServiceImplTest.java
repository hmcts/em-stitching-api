package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.Application;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DocumentConversionServiceImplTest {

    @Autowired
    DocumentConversionServiceImpl conversionService;

    @Test
    public void dontConvertPDFs() throws IOException {
        File input = new File("derp.pdf");
        File output = conversionService.convert(input);

        assertEquals(input, output);
    }
}