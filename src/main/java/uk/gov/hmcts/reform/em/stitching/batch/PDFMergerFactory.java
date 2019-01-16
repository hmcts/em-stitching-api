package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PDFMergerFactory {

    public PDFMergerUtility create() {
        String filename = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + "-stitched.pdf";
        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        pdfMergerUtility.setDestinationFileName(filename);

        return pdfMergerUtility;
    }

}
