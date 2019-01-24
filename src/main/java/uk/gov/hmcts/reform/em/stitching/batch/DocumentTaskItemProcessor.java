package uk.gov.hmcts.reform.em.stitching.batch;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
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
            final File tableOfContents = createTableOfContents(item.getBundle().getSortedItems());
            merger.addSource(tableOfContents);

            dmStoreDownloader
                .downloadFiles(item.getBundle().getSortedItems())
                .map(ThrowingFunction.unchecked(documentConverter::convert))
                .forEachOrdered(ThrowingConsumer.unchecked(merger::addSource));

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

    private File createTableOfContents(Stream<BundleDocument> sortedItems) throws IOException {
        Document document = new Document();
        Paragraph paragraph = new Paragraph();
        paragraph.addMarkup("Contents\n\n", 24, BaseFont.Helvetica);

        for (BundleDocument doc : sortedItems.collect(Collectors.toList())) {
            String content = String.format("{color:#ff5000}{link[#%s]}%s{link}{color:#000000}.%n", doc.getDocumentId(), doc.getDocTitle());
            paragraph.addMarkup(content, 14, BaseFont.Helvetica);
        }

        document.add(paragraph);

        final File tocFile = File.createTempFile("table-of-contents", ".pdf");
        final OutputStream outputStream = new FileOutputStream(tocFile);

        document.save(outputStream);

        return tocFile;
    }

}
