package uk.gov.hmcts.reform.em.stitching.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Entity
@Table(name = "bundle_folder")
public class BundleFolder extends AbstractAuditingEntity implements Serializable, SortableBundleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    private String description;
    private String folderName;
    private int sortIndex;

    @ElementCollection
    private List<BundleDocument> documents;

    @ElementCollection
    private List<BundleFolder> folders;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
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
    public Stream<BundleDocument> getSortedItems() {
        return Stream
                .<SortableBundleItem>concat(documents.stream(), folders.stream())
                .sorted(Comparator.comparingInt(SortableBundleItem::getSortIndex))
                .flatMap(SortableBundleItem::getSortedItems);
    }

    @Override
    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
    }
}
