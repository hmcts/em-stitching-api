package uk.gov.hmcts.reform.em.stitching.domain;

public interface PageNumberFormatStrategy {

    String getPageNumber(int pageNumber, int noOfPages);

}
