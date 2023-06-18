package uk.gov.hmcts.reform.em.stitching.domain;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.CallbackState;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Callback.
 */
@Entity
@Table(name = "callback")
public class Callback extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_seq")
    @SequenceGenerator(name = "hibernate_seq", sequenceName = "hibernate_sequence")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_state")
    private CallbackState callbackState = CallbackState.NEW;

    @Column(name = "failure_description", length = 5000)
    private String failureDescription;

    @Column(length = 5000)
    @NotNull
    private String callbackUrl;

    private int version;

    @Getter
    @Setter
    private int attempts;

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

    public String getFailureDescription() {
        return failureDescription;
    }

    public Callback failureDescription(String failureDescription) {
        this.setFailureDescription(failureDescription);
        return this;
    }

    public void setFailureDescription(String failureDescription) {
        this.failureDescription = failureDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Callback callback = (Callback) o;
        if (callback.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), callback.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public CallbackState getCallbackState() {
        return callbackState;
    }

    public void setCallbackState(CallbackState callbackState) {
        this.callbackState = callbackState;
    }

    public String toString() {
        return "Callback(id=" + this.getId() + ", callbackState=" + this.getCallbackState()
                + ", failureDescription=" + this.getFailureDescription()
                + ", callbackUrl=" + this.getCallbackUrl() + ", version=" + this.getVersion() + ")";
    }
}
