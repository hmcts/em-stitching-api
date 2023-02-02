package uk.gov.hmcts.reform.em.stitching.domain;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Comparator;
import java.util.ArrayList;
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
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL)
    private List<BundleDocument> documents = new ArrayList<>();

    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    @OneToMany(cascade = CascadeType.ALL)
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
