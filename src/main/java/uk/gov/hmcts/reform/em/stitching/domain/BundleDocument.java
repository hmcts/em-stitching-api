package uk.gov.hmcts.reform.em.stitching.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "bundle_document")
public class BundleDocument extends AbstractAuditingEntity implements Serializable {

    // Do we need this auto-generated ID if we have document-ID from CCD?
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

//    Can I delete this?
//    @OneToOne(mappedBy="bundle_document")
//    private DocumentTask documentTask;

    // documentId data type?
    private Long documentId;
    private String docTitle;
    private String docDescription;
    private String documentURI;
    private Instant dateAddedToCase;
    private boolean isIncludedInBundle;
    // todo: make an enum with applicant/respondant/staff/LR. Where?
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

    public String getCreatorRole() {
        return creatorRole;
    }

    public void setCreatorRole(String creatorRole) {
        this.creatorRole = creatorRole;
    }

    public boolean isIncludedInBundle() {
        return isIncludedInBundle;
    }

    public void setIncludedInBundle(boolean includedInBundle) {
        isIncludedInBundle = includedInBundle;
    }
}