package uk.gov.hmcts.reform.em.stitching.pdf;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;

@Service
public class PDFCoversheetService {

    public Pair<BundleDocument, File> addCoversheet(Pair<BundleDocument, File> pair) throws IOException {
        PDDocument document = PDDocument.load(pair.getSecond());
        PDPage coversheet = new PDPage();

        document.addPage(coversheet);
        addCenterText(document, coversheet, pair.getFirst().getDocTitle());
        addText(document, coversheet, pair.getFirst().getDocDescription(), 80, PDType1Font.HELVETICA_BOLD,13);
        moveLastPageToFirst(document);

        File convertedFile = File.createTempFile(pair.getFirst().getDocTitle(), ".pdf");
        document.save(convertedFile);
        document.close();

        return Pair.of(pair.getFirst(), convertedFile);
    }

    private void moveLastPageToFirst(PDDocument document) {
        PDPageTree allPages = document.getDocumentCatalog().getPages();
        if (allPages.getCount() > 1) {
            PDPage lastPage = allPages.get(allPages.getCount() - 1);
            allPages.remove(allPages.getCount() - 1);
            PDPage firstPage = allPages.get(0);
            allPages.insertBefore(lastPage, firstPage);
        }
    }

}
