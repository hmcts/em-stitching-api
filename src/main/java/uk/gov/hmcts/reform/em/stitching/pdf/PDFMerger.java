package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static uk.gov.hmcts.reform.em.stitching.pdf.PDFUtility.*;

@Service
public class PDFMerger {

    public File merge(Bundle bundle, List<Pair<BundleDocument, File>> documents) throws IOException {
        StatefulPDFMerger statefulPDFMerger = new StatefulPDFMerger(bundle.hasTableOfContents(), bundle.hasCoversheets(), bundle.getFileName());

        return statefulPDFMerger.merge(bundle, documents);
    }

    private class StatefulPDFMerger {
        private final PDFMergerUtility merger = new PDFMergerUtility();
        private final PDDocument document = new PDDocument();
        private final PDPage firstPage = new PDPage();
        private int currentPageNumber = 1;
        private final boolean createTableOfContents;
        private final boolean createCoversheet;
        private String fileName;
        private boolean duplicateName = false;

        public StatefulPDFMerger(boolean createTableOfContents, boolean createCoversheet, String fileName) {
            this.createTableOfContents = createTableOfContents;
            this.createCoversheet = createCoversheet;
            this.fileName = fileName;
        }

        public File merge(Bundle bundle, List<Pair<BundleDocument, File>> documents) throws IOException {

            if (createTableOfContents) {
                setupFirstPage(bundle);
            }

            int documentIndex = 1;

            for (Pair<BundleDocument, File> pair : documents) {
                PDDocument d = PDDocument.load(pair.getSecond());
                add(d, pair.getFirst().getDocTitle(), documentIndex++);
                d.close();
            }

            File file = new File(endNameInPdf(fileName));
            if (!file.createNewFile()) {
                uniqueFileCreator(appendBracketsToName(fileName), 1);
            }

            document.save(file);
            document.close();

            return file;
        }

        private String endNameInPdf(String currentFileName) {
         // TODO use ternary
            if (!currentFileName.endsWith(".pdf")) {
                currentFileName = currentFileName + ".pdf";
            }
            return currentFileName;
        }

        private String appendBracketsToName(String currentFileName) {
            return currentFileName.substring(0, currentFileName.length()-4) + "(1).pdf";
        }

        private void uniqueFileCreator(String currentFileName, int count) throws IOException {
            // TODO account for length(count) > 1 in 1st segment
            currentFileName =
                    currentFileName.substring(0, currentFileName.length()-6)
                    + count + currentFileName.substring(currentFileName.length()-5);

            File newFile = new File(currentFileName);

            if (!newFile.createNewFile()) {
                duplicateName = true;
                uniqueFileCreator(currentFileName, count+1);
            }
        }

        // TODO Implement this
//        private String avoidBadFileNameEncoding(String uncheckedFileName) {
//            String checkedFileName = uncheckedFileName;
//            return sanitisedFileName;
//        }


        private void setupFirstPage(Bundle bundle) throws IOException {
            document.addPage(firstPage);
            addCenterText(document, firstPage, bundle.getBundleTitle());
            addText(document, firstPage, bundle.getDescription(), 80);
            addCenterText(document, firstPage, "Contents", 100);
        }

        private void add(PDDocument newDoc, String title, int documentIndex) throws IOException {
            merger.appendDocument(document, newDoc);

            if (createTableOfContents) {
                addTableOfContentsItem(title, documentIndex);
                if (createCoversheet) {
                    addBackToTopLink();
                }
            }

            currentPageNumber += newDoc.getNumberOfPages();
        }

        private void addTableOfContentsItem(String documentTitle, int documentIndex) throws IOException {
            final float yOffset = 150f + documentIndex * LINE_HEIGHT;
            final PDPage destination = document.getPage(currentPageNumber);
            final String text = documentTitle + ", p" + (currentPageNumber + 1);

            addLink(document, firstPage, destination, text, yOffset);
        }

        private void addBackToTopLink() throws IOException {
            final float yOffset = 120f;
            final PDPage from = document.getPage(currentPageNumber);

            addLink(document, from, firstPage, "Back to top", yOffset);
        }
    }
}
