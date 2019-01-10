package uk.gov.hmcts.reform.em.stitching.domain;


import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.service.mapper.BundleToStringMapper;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DocumentTask.
 */
@Entity
@Table(name = "document_task")
public class DocumentTask extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "bundle")
    @Convert(converter = BundleToStringMapper.class)
    private Bundle bundle;

    @Column(name = "output_document_id")
    private String outputDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_state")
    private TaskState taskState = TaskState.NEW;

    @Column(name = "failure_description")
    private String failureDescription;

    @Column(name = "jwt")
    private String jwt;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public DocumentTask bundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public String getOutputDocumentId() {
        return outputDocumentId;
    }

    public DocumentTask outputDocumentId(String outputDocumentId) {
        this.outputDocumentId = outputDocumentId;
        return this;
    }

    public void setOutputDocumentId(String outputDocumentId) {
        this.outputDocumentId = outputDocumentId;
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public DocumentTask taskState(TaskState taskState) {
        this.taskState = taskState;
        return this;
    }

    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }

    public String getFailureDescription() {
        return failureDescription;
    }

    public DocumentTask failureDescription(String failureDescription) {
        this.failureDescription = failureDescription;
        return this;
    }

    public void setFailureDescription(String failureDescription) {
        this.failureDescription = failureDescription;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DocumentTask documentTask = (DocumentTask) o;
        if (documentTask.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), documentTask.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "DocumentTask{" +
            "id=" + getId() +
            ", bundle='" + getBundle() + "'" +
            ", outputDocumentId='" + getOutputDocumentId() + "'" +
            ", taskState='" + getTaskState() + "'" +
            ", failureDescription='" + getFailureDescription() + "'" +
            "}";
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
