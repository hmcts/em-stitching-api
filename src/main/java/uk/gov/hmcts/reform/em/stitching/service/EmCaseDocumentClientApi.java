package uk.gov.hmcts.reform.em.stitching.service;

import org.springframework.cloud.openfeign.FeignClient;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;

@FeignClient(configuration = CaseDocumentClientApi.class, name = "EmCaseDocumentClientApi")
public interface EmCaseDocumentClientApi {
}
