package uk.gov.hmcts.reform.em.stitching.testutil.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CcdBundleDocumentDTO {

    private String documentName;
    private CcdDocument documentLink;
}
