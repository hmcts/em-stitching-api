package uk.gov.hmcts.reform.em.stitching.domain;

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

    @ElementCollection
    @OneToMany(cascade = CascadeType.ALL)
    private List<BundleFolder> folders = new ArrayList<>();

    @ElementCollection
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
}
