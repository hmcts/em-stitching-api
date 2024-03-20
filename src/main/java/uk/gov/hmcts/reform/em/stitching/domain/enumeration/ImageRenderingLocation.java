package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ImageRenderingLocation {
    @JsonProperty("allPages")
    ALL_PAGES,
    @JsonProperty("firstPage")
    FIRST_PAGE
}
