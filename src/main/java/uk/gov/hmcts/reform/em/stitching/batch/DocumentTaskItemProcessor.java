package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import pl.touk.throwing.ThrowingConsumer;
import pl.touk.throwing.ThrowingFunction;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.text.BaseFont;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rst.pdfbox.layout.elements.Document;

public class DocumentTaskItemProcessor implements ItemProcessor<DocumentTask, DocumentTask> {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskItemProcessor.class);
    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;
    private final DocumentConversionService documentConverter;
    private final PDFMergerFactory pdfMergerFactory;

    public DocumentTaskItemProcessor(DmStoreDownloader dmStoreDownloader,
                                     DmStoreUploader dmStoreUploader,
                                     DocumentConversionService documentConverter,
                                     PDFMergerFactory pdfMergerFactory) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.documentConverter = documentConverter;
        this.pdfMergerFactory = pdfMergerFactory;
    }

    @Override
    public DocumentTask process(DocumentTask item) {
        final PDFMergerUtility merger = pdfMergerFactory.create();

        try {
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();
            doc.addPage(page);
            int i = 1;

            dmStoreDownloader
                .downloadFiles(item.getBundle().getSortedItems())
                .map(ThrowingFunction.unchecked(documentConverter::convert))
                .forEachOrdered(docFile -> {
                    PDDocument newDoc = PDDocument.load(docFile);
                    merger.appendDocument(doc, newDoc);
                    addTableOfContentsItem(page, i);


                    i += newDoc.getNumberOfPages();
                });

            merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
            final File outputFile = new File(merger.getDestinationFileName());

            dmStoreUploader.uploadFile(outputFile, item);

            item.setTaskState(TaskState.DONE);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);

            item.setTaskState(TaskState.FAILED);
            item.setFailureDescription(e.getMessage());
        }

        return item;
    }

    private void addTableOfContentsItem(PDPage page, int pageNumber) throws IOException {
        PDPageXYZDestination dest = new PDPageXYZDestination();
        dest.setPageNumber(pageNumber);
        dest.setLeft(0);
        dest.setTop(0);

        PDActionGoTo action = new PDActionGoTo();
        action.setDestination(dest);

        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(72);
        rect.setLowerLeftY(600);
        rect.setUpperRightX(144);
        rect.setUpperRightY(620);

        PDAnnotationLink link = new PDAnnotationLink();
        link.setAction(action);
        link.setDestination(dest);
        link.setRectangle(rect);

        PDPageContentStream stream = new PDPageContentStream(doc, page, true, true);
        stream.beginText();
        stream.setNonStrokingColor(0,0,0);
        stream.setFont(PDType1Font.HELVETICA, 8);
        stream.newLineAtOffset(50,220);
        stream.showText("Website: google.com");
        stream.endText();
    }

}
