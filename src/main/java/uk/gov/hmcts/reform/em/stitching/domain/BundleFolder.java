package uk.gov.hmcts.reform.em.stitching.domain;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Comparator;
import java.util.ArrayList;
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
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<BundleDocument> documents = new ArrayList<>();

    @ElementCollection
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<BundleFolder> folders = new ArrayList<>();


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

    public void setFolders(@NotNull List<BundleFolder> folders) {
        if (folders == null) {
            throw new RuntimeException("null folders");
        }
        this.folders = folders;
    }

    public List<BundleDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(@NotNull List<BundleDocument> documents) {
        if (documents == null) {
            throw new RuntimeException("null documents");
        }

        this.documents = documents;
    }

    @Override
    @Transient
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
