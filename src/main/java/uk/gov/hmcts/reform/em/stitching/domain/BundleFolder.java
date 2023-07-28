package uk.gov.hmcts.reform.em.stitching.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Table(name = "bundle_folder")
public class BundleFolder extends AbstractAuditingEntity implements Serializable, SortableBundleItem, BundleContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    @Size(max = 255, message = "Bundle Folder Description can not be more than 255 Chars")
    private String description;
    @Size(max = 255, message = "Bundle Folder Name can not be more than 255 Chars")
    private String folderName;
    private int sortIndex;

    @ElementCollection
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<BundleDocument> documents = new ArrayList<>();

    @ElementCollection
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<BundleFolder> folders = new ArrayList<>();


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    @Transient
    public String getTitle() {
        return getFolderName();
    }

    public String getDescription() {
        return description;
    }

    @Override
    @Transient
    public Stream<SortableBundleItem> getSortedItems() {
        return Stream
                .<SortableBundleItem>concat(documents.stream(), folders.stream())
                .filter(i -> i.getSortedDocuments().count() > 0)
                .sorted(Comparator.comparingInt(SortableBundleItem::getSortIndex));
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
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
    @Transient
    public Stream<BundleDocument> getSortedDocuments() {
        return getSortedItems().flatMap(SortableBundleItem::getSortedDocuments);
    }

    @Override
    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }

    @Override
    @Transient
    public BundleItemType getType() {
        return BundleItemType.FOLDER;
    }
}
