package uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation;

import org.springframework.data.annotation.ReadOnlyProperty;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base abstract class for DTO which will hold definitions for created, last modified by and created,
 * last modified by date.
 */
public abstract class AbstractAuditingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ReadOnlyProperty
    private String createdBy;

    private IdamDetailsDTO createdByDetails;

    @ReadOnlyProperty
    private Instant createdDate = Instant.now();

    private String lastModifiedBy;

    private IdamDetailsDTO lastModifiedByDetails;

    private Instant lastModifiedDate = Instant.now();

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public IdamDetailsDTO getCreatedByDetails() {
        return createdByDetails;
    }

    public void setCreatedByDetails(IdamDetailsDTO createdByDetails) {
        this.createdByDetails = createdByDetails;
    }

    public IdamDetailsDTO getLastModifiedByDetails() {
        return lastModifiedByDetails;
    }

    public void setLastModifiedByDetails(IdamDetailsDTO lastModifiedByDetails) {
        this.lastModifiedByDetails = lastModifiedByDetails;
    }
}
