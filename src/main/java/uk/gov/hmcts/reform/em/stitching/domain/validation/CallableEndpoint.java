package uk.gov.hmcts.reform.em.stitching.domain.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CallableEndpointValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CallableEndpoint {
    String message() default "{CallableEndpoint.documentTaskDTO.callback.callbackUrl}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
