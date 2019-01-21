package uk.gov.hmcts.reform.em.stitching.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bundle_folder")
public class BundleFolder extends AbstractAuditingEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    private String description;
    private String folderName;
    private String orderFoldersBy;
    private String orderDocumentsBy;

    @ElementCollection
    @OneToMany(cascade=CascadeType.ALL)
    private List<BundleDocument> documents = new ArrayList<>();

    @ElementCollection
    @OneToMany(cascade=CascadeType.ALL)
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

    public String getOrderFoldersBy() {
        return orderFoldersBy;
    }

    public void setOrderFoldersBy(String orderFoldersBy) {
        this.orderFoldersBy = orderFoldersBy;
    }

    public String getOrderDocumentsBy() {
        return orderDocumentsBy;
    }

    public void setOrderDocumentsBy(String orderDocumentsBy) {
        this.orderDocumentsBy = orderDocumentsBy;
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
}
