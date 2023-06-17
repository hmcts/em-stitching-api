package uk.gov.hmcts.reform.em.stitching.service.dto;

import lombok.ToString;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.CallbackState;
import uk.gov.hmcts.reform.em.stitching.domain.validation.CallableEndpoint;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the DocumentTask entity.
 */
@ToString(callSuper = true)
public class CallbackDto extends AbstractAuditingDTO implements Serializable {

    private Long id;

    private CallbackState callbackState = CallbackState.NEW;

    private String failureDescription;

    @NotNull
    @CallableEndpoint
    private String callbackUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFailureDescription() {
        return failureDescription;
    }

    public void setFailureDescription(String failureDescription) {
        this.failureDescription = failureDescription;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CallbackDto callbackDto = (CallbackDto) o;
        if (callbackDto.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), callbackDto.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

}
