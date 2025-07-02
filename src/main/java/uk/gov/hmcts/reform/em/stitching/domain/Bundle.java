package uk.gov.hmcts.reform.em.stitching.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.hibernate.annotations.Type;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PageNumberFormat;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "bundle")
public class Bundle extends AbstractAuditingEntity implements SortableBundleItem, Serializable, BundleContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Size(max = 255, message = "Bundle Title can not be more than 255 Chars")
    private String bundleTitle;
    @Size(max = 1000, message = "Bundle Description can not be more than 1000 Chars")
    private String description;
    private String stitchedDocumentURI;
    private String stitchStatus;
    @Size(max = 255, message = "File Name can not be more than 255 Chars")
    private String fileName;
    @Size(max = 255, message = "File Name Identifier can not be more than 255 Chars")
    private String fileNameIdentifier;
    private String coverpageTemplate;
    private PageNumberFormat pageNumberFormat;
    private boolean hasTableOfContents;
    private boolean hasCoversheets;
    private boolean hasFolderCoversheets;
    private PaginationStyle paginationStyle;
    private Boolean enableEmailNotification;

    @Column(name = "hash_token", length = 5000)
    private String hashToken;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private DocumentImage documentImage;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode coverpageTemplateData;

    @OneToMany(cascade = CascadeType.ALL)
    private List<BundleFolder> folders = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    private List<BundleDocument> documents = new ArrayList<>();

    public String getHashToken() {
        return hashToken;
    }

    public void setHashToken(String hashToken) {
        this.hashToken = hashToken;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBundleTitle() {
        return bundleTitle;
    }

    public void setBundleTitle(String bundleTitle) {
        this.bundleTitle = bundleTitle;
    }

    @Override
    @Transient
    public String getTitle() {
        return getBundleTitle();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @Transient
    public Stream<SortableBundleItem> getSortedItems() {
        return Stream
            .<SortableBundleItem>concat(documents.stream().sorted(), folders.stream())
            .filter(i -> i.getSortedDocuments().count() > 0)
            .sorted(Comparator.comparingInt(SortableBundleItem::getSortIndex));
    }

    public String getStitchedDocumentURI() {
        return stitchedDocumentURI;
    }

    public void setStitchedDocumentURI(String stitchedDocumentURI) {
        this.stitchedDocumentURI = stitchedDocumentURI;
    }

    public String getStitchStatus() {
        return stitchStatus;
    }

    public void setStitchStatus(String stitchStatus) {
        this.stitchStatus = stitchStatus;
    }

    public List<BundleFolder> getFolders() {
        return folders;
    }

    public void setFolders(List<BundleFolder> folders) {
        this.folders = folders;
    }

    public List<BundleDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<BundleDocument> documents) {
        this.documents = documents;
    }

    public String getFileName() {
        return fileName == null || fileName.isEmpty() ? bundleTitle : fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileNameIdentifier() {
        return fileNameIdentifier;
    }

    public void setFileNameIdentifier(String fileNameIdentifier) {
        this.fileNameIdentifier = fileNameIdentifier;
    }

    public String getCoverpageTemplate() {
        return coverpageTemplate;
    }

    public void setCoverpageTemplate(String coverpageTemplate) {
        this.coverpageTemplate = coverpageTemplate;
    }

    public boolean hasTableOfContents() {
        return hasTableOfContents;
    }

    public void setHasTableOfContents(boolean hasTableOfContents) {
        this.hasTableOfContents = hasTableOfContents;
    }

    public boolean hasCoversheets() {
        return hasCoversheets;
    }

    public void setHasCoversheets(boolean hasCoversheets) {
        this.hasCoversheets = hasCoversheets;
    }

    @Override
    public int getSortIndex() {
        return 0;
    }

    @Override
    @Transient
    public Stream<BundleDocument> getSortedDocuments() {
        return getSortedItems().flatMap(SortableBundleItem::getSortedDocuments);
    }

    public boolean hasFolderCoversheets() {
        return hasFolderCoversheets;
    }

    public void setHasFolderCoversheets(boolean hasFolderCoversheets) {
        this.hasFolderCoversheets = hasFolderCoversheets;
    }

    public PaginationStyle getPaginationStyle() {
        return paginationStyle == null ? PaginationStyle.off : paginationStyle;
    }

    public void setPaginationStyle(PaginationStyle paginationStyle) {
        this.paginationStyle = paginationStyle;
    }

    public JsonNode getCoverpageTemplateData() {
        return coverpageTemplateData;
    }

    public void setCoverpageTemplateData(JsonNode coverpageTemplateData) {
        this.coverpageTemplateData = coverpageTemplateData;
    }

    public PageNumberFormat getPageNumberFormat() {
        return pageNumberFormat == null ? PageNumberFormat.NUMBER_OF_PAGES : pageNumberFormat;
    }

    public void setPageNumberFormat(PageNumberFormat pageNumberFormat) {
        this.pageNumberFormat = pageNumberFormat;
    }

    public Boolean isEnableEmailNotification() {
        return enableEmailNotification;
    }

    public void setEnableEmailNotification(Boolean enableEmailNotification) {
        this.enableEmailNotification = enableEmailNotification;
    }

    public DocumentImage getDocumentImage() {
        return documentImage;
    }

    public void setDocumentImage(DocumentImage documentImage) {
        this.documentImage = documentImage;
    }

    public String toString() {
        return "Bundle(id=" + this.getId() + ", bundleTitle=" + this.getBundleTitle()
                + ", description=" + this.getDescription() + ", stitchedDocumentURI=" + this.getStitchedDocumentURI()
                + ", stitchStatus=" + this.getStitchStatus()
                + ", fileName=" + this.getFileName() + ", hasTableOfContents="
                + this.hasTableOfContents + ", hasCoversheets=" + this.hasCoversheets + ", hasFolderCoversheets="
                + this.hasFolderCoversheets + ")";
    }

    @Transient
    public Integer getNumberOfSubtitles(SortableBundleItem container,
                                        Map<BundleDocument, File> documentBundledFilesRef) {
        if (container.getSortedDocuments().count() == documentBundledFilesRef.size()) {
            List<PDDocument> docsToClose = new ArrayList<>();
            int subtitles = extractDocumentOutlineStream(container, documentBundledFilesRef, docsToClose)
                .mapToInt(pdDocumentOutline -> getItemsFromOutline.apply(pdDocumentOutline)).sum();
            closeDocuments(docsToClose);
            return subtitles;
        } else {
            return 0;
        }

    }

    @Transient
    public List<String> getSubtitles(SortableBundleItem container, Map<BundleDocument, File> documentBundledFilesRef) {
        if (container.getSortedDocuments().count() == documentBundledFilesRef.size()) {
            List<PDDocument> docsToClose = new ArrayList<>();
            List<String> subtitles = extractDocumentOutlineStream(container, documentBundledFilesRef, docsToClose)
                .map(pdDocumentOutline -> getItemTitlesFromOutline.apply(pdDocumentOutline))
                .flatMap(List::stream)
                .collect(Collectors.toList());
            closeDocuments(docsToClose);
            return subtitles;
        }
        return new ArrayList<>();
    }

    private Stream<PDDocumentOutline> extractDocumentOutlineStream(SortableBundleItem container,
                                                                   Map<BundleDocument, File> documentBundledFilesRef,
                                                                   List<PDDocument> docsToClose) {
        return container
            .getSortedItems().flatMap(SortableBundleItem::getSortedDocuments)
            .map(bundleDocument -> {
                try {
                    PDDocument pdDocument = Loader.loadPDF(documentBundledFilesRef.get(bundleDocument));
                    docsToClose.add(pdDocument);
                    return pdDocument;
                } catch (IOException ioException) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .map(pdDocument -> pdDocument.getDocumentCatalog().getDocumentOutline())
            .filter(pdDocumentOutline ->
                Objects.nonNull(pdDocumentOutline) && Objects.nonNull(pdDocumentOutline.getFirstChild()));
    }

    private void closeDocuments(List<PDDocument> docsToClose) {
        docsToClose.forEach(doc -> {
            try {
                doc.close();
            } catch (IOException e) {
                e.getStackTrace();
            }
        });
    }

    @Transient
    private PDDocumentOutline extractDocumentOutline(
            BundleDocument bd,
            Map<BundleDocument, File> documentContainingFiles) {
        try (PDDocument pdDocument = Loader.loadPDF(documentContainingFiles.get(bd))) {
            return pdDocument
                    .getDocumentCatalog()
                    .getDocumentOutline();
        } catch (IOException e) {
            e.getStackTrace();
        }
        return null;
    }

    @Transient
    private Function<PDDocumentOutline, Integer> getItemsFromOutline = (outline) -> {
        ArrayList<String> firstSiblings = new ArrayList<>();
        PDOutlineItem anySubtitlesForItem = outline.getFirstChild();

        while (anySubtitlesForItem != null) {
            firstSiblings.add(anySubtitlesForItem.getTitle());
            anySubtitlesForItem = anySubtitlesForItem.getNextSibling();
        }

        return firstSiblings.size();
    };

    @Transient
    private Function<PDDocumentOutline, List<String>> getItemTitlesFromOutline = (outline) -> {
        ArrayList<String> firstSiblings = new ArrayList<>();
        PDOutlineItem anySubtitlesForItem = outline.getFirstChild();

        while (anySubtitlesForItem != null) {
            firstSiblings.add(anySubtitlesForItem.getTitle());
            anySubtitlesForItem = anySubtitlesForItem.getNextSibling();
        }

        return firstSiblings;
    };

    @Override
    @Transient
    public BundleItemType getType() {
        return BundleItemType.BUNDLE;
    }
}

