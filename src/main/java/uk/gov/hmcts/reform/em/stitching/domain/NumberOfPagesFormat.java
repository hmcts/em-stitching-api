package uk.gov.hmcts.reform.em.stitching.domain;

import org.springframework.stereotype.*;

@Component
public class NumberOfPagesFormat implements PageNumberFormatStrategy {
    @Override
    public String getPageNumber(int pageNumber, int noOfPages) {
        return (pageNumber + 1) + " - " + (pageNumber + noOfPages);
    }
}
