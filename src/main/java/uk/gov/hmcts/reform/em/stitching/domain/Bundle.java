package uk.gov.hmcts.reform.em.stitching.domain;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Table(name = "bundle")
public class Bundle extends AbstractAuditingEntity implements SortableBundleItem, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String bundleTitle;
    private String description;
    private String stitchedDocumentURI;
    private String stitchStatus;
    private String fileName;
    private boolean hasTableOfContents;
    private boolean hasCoversheets;
    private boolean hasFolderCoversheets;

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL)
    private List<BundleFolder> folders = new ArrayList<>();

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL)
    private List<BundleDocument> documents = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public Stream<BundleDocument> getSortedItems() {
        return Stream
                .<SortableBundleItem>concat(documents.stream(), folders.stream())
                .sorted(Comparator.comparingInt(SortableBundleItem::getSortIndex))
                .flatMap(SortableBundleItem::getSortedItems);
    }

    public boolean hasFolderCoversheets() {
        return hasFolderCoversheets;
    }

    public void setHasFolderCoversheets(boolean hasFolderCoversheets) {
        this.hasFolderCoversheets = hasFolderCoversheets;
    }
}
