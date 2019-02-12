package uk.gov.hmcts.reform.em.stitching.conversion;

import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

/**
 * No-op. This converter does nothing as the document is already a PDF.
 */
public class PDFConverter implements FileToPDFConverter {

    @Override
    public List<String> accepts() {
        return Lists.newArrayList("application/pdf");
    }

    @Override
    public File convert(File file) {
        return file;
    }

}
