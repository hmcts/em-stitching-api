package uk.gov.hmcts.reform.em.stitching.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
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
import java.util.function.Function;
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

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL)
    private List<BundleFolder> folders = new ArrayList<>();

    @ElementCollection
    @OneToMany(cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
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
            .<SortableBundleItem>concat(documents.stream(), folders.stream())
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
        return pageNumberFormat == null ? PageNumberFormat.numberOfPages : pageNumberFormat;
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
                + ", stitchStatus=" + this.getStitchStatus() + ", fileName=" + this.getFileName() + ", hasTableOfContents="
                + this.hasTableOfContents + ", hasCoversheets=" + this.hasCoversheets + ", hasFolderCoversheets="
                + this.hasFolderCoversheets + ")";
    }

    @Transient
    public Integer getSubtitles(SortableBundleItem container, Map<BundleDocument, File> documentBundledFilesRef) {
        if (container.getSortedDocuments().count() == documentBundledFilesRef.size()) {
            return container
                    .getSortedItems().flatMap(SortableBundleItem::getSortedDocuments)
                    .map(i -> extractDocumentOutline(i, documentBundledFilesRef))
                    .filter(o -> o != null && o.getFirstChild() != null)
                    .mapToInt(o -> getItemsFromOutline.apply(o)).sum();
        } else {
            return 0;
        }

    }

    @Transient
    private PDDocumentOutline extractDocumentOutline(BundleDocument bd, Map<BundleDocument, File> documentContainingFiles) {
        try (PDDocument pdDocument = PDDocument
                .load(documentContainingFiles.get(bd))) {
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

    @Override
    @Transient
    public BundleItemType getType() {
        return BundleItemType.BUNDLE;
    }
}

