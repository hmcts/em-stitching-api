package uk.gov.hmcts.reform.em.stitching.testutil.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class CcdBundleDocumentDTO implements Serializable {
    private String documentName;
    private CcdDocument documentLink;
}
