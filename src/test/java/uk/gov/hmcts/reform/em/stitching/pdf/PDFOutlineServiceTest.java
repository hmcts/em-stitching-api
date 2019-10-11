package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.BundleTest;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.File;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class PDFOutlineServiceTest {

    @MockBean
    private DocumentTaskMapper documentTaskMapper;

    private static final String inputFileName = ClassLoader.getSystemResource("TEST_INPUT_FILE.pdf").getPath();
    private static final File INPUT_FILE = new File(inputFileName);
    private static Pair<BundleDocument, File> pair;
    private static String documentName;

    @Before
    public void setup() {
        Bundle bundle = BundleTest.getTestBundle();
        pair = Pair.of(bundle.getDocuments().get(0), INPUT_FILE);
        documentName = bundle.getDocuments().get(0).getDocTitle();
    }

    @Test
    public void getOutlines() throws Exception {
        PDDocument doc1 = PDDocument.load(INPUT_FILE);
        doc1.close();

//        documentFormatter.createOutline(doc1);
    }
}
