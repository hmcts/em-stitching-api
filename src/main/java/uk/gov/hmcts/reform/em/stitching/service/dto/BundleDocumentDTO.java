package uk.gov.hmcts.reform.em.stitching.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

public class BundleDocumentDTO extends AbstractAuditingDTO implements Serializable {

    // Do we need this auto-generated ID if we have document-ID from CCD?
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @JsonIgnore
    private Long documentId;
    private String docTitle;
    private String docDescription;
    private String documentURI;
    private Instant dateAddedToCase;
    private boolean isIncludedInBundle;
    // make an enum with applicant/respondant/staff/LR. Where?
    private String creatorRole;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public String getDocDescription() {
        return docDescription;
    }

    public void setDocDescription(String docDescription) {
        this.docDescription = docDescription;
    }

    public String getDocumentURI() {
        return documentURI;
    }

    public void setDocumentURI(String documentURI) {
        this.documentURI = documentURI;
    }

    public Instant getDateAddedToCase() {
        return dateAddedToCase;
    }

    public void setDateAddedToCase(Instant dateAddedToCase) {
        this.dateAddedToCase = dateAddedToCase;
    }

    public boolean isIncludedInBundle() {
        return isIncludedInBundle;
    }

    public void setIncludedInBundle(boolean includedInBundle) {
        isIncludedInBundle = includedInBundle;
    }

    public String getCreatorRole() {
        return creatorRole;
    }

    public void setCreatorRole(String creatorRole) {
        this.creatorRole = creatorRole;
    }
}
