package uk.gov.hmcts.reform.em.stitching.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

public class BundleFolderDTO extends AbstractAuditingDTO implements Serializable {

    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    private String description;
    private String folderName;
    private String orderDocumentsBy;
    private String orderFoldersBy;
    private List<BundleDocumentDTO> documents;
    private List<BundleFolderDTO> folders;

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

    public List<BundleDocumentDTO> getDocuments() {
        return documents;
    }

    public void setDocuments(List<BundleDocumentDTO> documents) {
        this.documents = documents;
    }

    public List<BundleFolderDTO> getFolders() {
        return folders;
    }

    public void setFolders(List<BundleFolderDTO> folders) {
        this.folders = folders;
    }

    public String getOrderDocumentsBy() {
        return orderDocumentsBy;
    }

    public void setOrderDocumentsBy(String orderDocumentsBy) {
        this.orderDocumentsBy = orderDocumentsBy;
    }

    public String getOrderFoldersBy() {
        return orderFoldersBy;
    }

    public void setOrderFoldersBy(String orderFoldersBy) {
        this.orderFoldersBy = orderFoldersBy;
    }
}

