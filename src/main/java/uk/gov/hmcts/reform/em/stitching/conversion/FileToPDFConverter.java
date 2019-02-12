package uk.gov.hmcts.reform.em.stitching.conversion;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FileToPDFConverter {

    List<String> accepts();

    File convert(File file) throws IOException;

}
