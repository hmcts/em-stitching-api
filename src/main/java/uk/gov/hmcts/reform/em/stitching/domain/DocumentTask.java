package uk.gov.hmcts.reform.em.stitching.domain;

import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DocumentTask.
 */
@Entity
@Table(name = "versioned_document_task")
public class DocumentTask extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private Bundle bundle;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_state")
    private TaskState taskState = TaskState.NEW;

    @Column(name = "failure_description")
    private String failureDescription;

    @Column(name = "jwt", length = 5000)
    private String jwt;

    @OneToOne(cascade = CascadeType.ALL)
    private Callback callback;

    private int version;

    public DocumentTask() {
        // this is intentional
    }

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
