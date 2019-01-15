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
    private List<BundleDocumentDTO> documents;  // Todo should this be the DTO objects?
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

    public List<String> getDocuments() {
        return documents;
    }

    public void setDocuments(List<String> documents) {
        this.documents = documents;
    }

    public List<String> getFolders() {
        return folders;
    }

    public void setFolders(List<String> folders) {
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

