package uk.gov.hmcts.reform.em.stitching.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class Bundle {
    private Long id;
    private String bundleTitle;
    private int version;
    private String description;
    private String purpose;
    private Instant dateCreated;
    private String modifiedBy;
    private Instant dateUpdated;
    private String updatedBy;
    private String stitchedDocId;
    private String stitchedDocumentURI;
    private String stitchStatus;
    private boolean isLocked;
    private Instant dateLocked;
    private String lockedBy;
    private String comments;
    private String[] folders;
    private String[] documents;
    private String orderFoldersBy;
    private String orderDocumentsBy;

    public Bundle(
        Long id,
        String bundleTitle,
        int version,
        String description,
        String purpose,
        Instant dateCreated,
        String modifiedBy,
        Instant dateUpdated,
        String updatedBy,
        String stitchedDocId,
        String stitchedDocumentURI,
        String stitchStatus,
        boolean isLocked,
        Instant dateLocked,
        String lockedBy,
        String comments,
        String[] folders,
        String[] documents,
        String orderFoldersBy,
        String orderDocumentsBy
    ) {
        this.id = id;
        this.bundleTitle = bundleTitle;
        this.version = version;
        this.description = description;
        this.purpose = purpose;
        this.dateCreated = dateCreated;
        this.modifiedBy = modifiedBy;
        this.dateUpdated = dateUpdated;
        this.updatedBy = updatedBy;
        this.stitchedDocId = stitchedDocId;
        this.stitchedDocumentURI = stitchedDocumentURI;
        this.stitchStatus = stitchStatus;
        this.isLocked = isLocked;
        this.dateLocked = dateLocked;
        this.lockedBy = lockedBy;
        this.comments = comments;
        this.folders = folders;
        this.documents = documents;
        this.orderFoldersBy = orderFoldersBy;
        this.orderDocumentsBy = orderDocumentsBy;
    }

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

    public Instant getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Instant dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
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

    public String[] getFolders() {
        return folders;
    }

    public void setFolders(String[] folders) {
        this.folders = folders;
    }

    public String[] getDocuments() {
        return documents;
    }

    public void setDocuments(String[] documents) {
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
}
