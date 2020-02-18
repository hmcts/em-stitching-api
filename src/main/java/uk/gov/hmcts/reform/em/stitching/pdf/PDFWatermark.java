package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentImage;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class PDFWatermark {

    public void processDocumentWatermark(File documentImageFile,
                                                              Map<BundleDocument, File> documents,
                                                              DocumentImage documentImage) throws IOException {
        for (Map.Entry<BundleDocument, File> entry : documents.entrySet()) {
            BundleDocument bundleDocument = entry.getKey();
            File documentFile = entry.getValue();
            PDDocument newDoc = PDDocument.load(documentFile);
            addDocumentWatermark(documentFile, documentImageFile, newDoc, documentImage);
        }
    }

    private void addDocumentWatermark(File documentFile,
                                      File documentImageFile,
                                      PDDocument document,
                                      DocumentImage documentImage) throws IOException {
        PDDocument overlayDocument = new PDDocument();
        PDPage overlayPage = new PDPage();
        overlayDocument.addPage(overlayPage);

        PDImageXObject pdImage = PDImageXObject.createFromFileByExtension(documentImageFile, overlayDocument);
        PDRectangle mediaBox = overlayPage.getMediaBox();

        documentImage.verifyCoordinates();
        double startX = (mediaBox.getWidth() * (documentImage.getCoordinateX() / 100.0)) - ((double) pdImage.getWidth() / 2);
        double startY = (mediaBox.getHeight() * (documentImage.getCoordinateY() / 100.0)) - ((double) pdImage.getHeight() / 2);

        try (PDPageContentStream contentStream = new PDPageContentStream(overlayDocument, overlayPage)) {
            contentStream.drawImage(pdImage, (float) startX, (float) startY);
        }

        try (Overlay overlay = new Overlay()) {
            overlay.setInputPDF(document);
            overlay.setOverlayPosition(documentImage.getImageRendering().getPosition());

            if (documentImage.getImageRenderingLocation() == ImageRenderingLocation.allPages) {
                overlay.setAllPagesOverlayPDF(overlayDocument);
            } else {
                overlay.setFirstPageOverlayPDF(overlayDocument);
            }

            HashMap<Integer, String> overlayMap = new HashMap<>();
            overlay.overlay(overlayMap);
            document.save(documentFile);
        }
    }
}
