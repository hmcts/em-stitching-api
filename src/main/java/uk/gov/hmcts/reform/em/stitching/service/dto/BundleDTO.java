package uk.gov.hmcts.reform.em.stitching.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class BundleDTO extends AbstractAuditingDTO implements Serializable {
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @JsonIgnore
    private DocumentTaskDTO documentTask;
    private String bundleTitle;
    private int version;
    private String description;
    private String purpose;
    private Instant dateCreated;
    private Instant dateUpdated;
    private String updatedBy;
    private String stitchedDocId;
    private String stitchedDocumentURI;
    private String stitchStatus;
    private boolean isLocked;
    private Instant dateLocked;
    private String lockedBy;
    private String comments;
    private List<String> folders;
    private List<String> documents;
    private String orderFoldersBy;
    private String orderDocumentsBy;

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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStitchedDocId() {
        return stitchedDocId;
    }

    public void setStitchedDocId(String stitchedDocId) {
        this.stitchedDocId = stitchedDocId;
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

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public Instant getDateLocked() {
        return dateLocked;
    }

    public void setDateLocked(Instant dateLocked) {
        this.dateLocked = dateLocked;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<String> getFolders() {
        return folders;
    }

    public void setFolders(List<String> folders) {
        this.folders = folders;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void setDocuments(List<String> documents) {
        this.documents = documents;
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

    public DocumentTaskDTO getDocumentTask() {
        return documentTask;
    }

    public void setDocumentTask(DocumentTaskDTO documentTask) {
        this.documentTask = documentTask;
    }

    public Instant getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Instant dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Instant getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(Instant dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

