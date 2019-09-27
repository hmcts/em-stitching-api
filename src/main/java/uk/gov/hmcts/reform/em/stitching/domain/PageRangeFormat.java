package uk.gov.hmcts.reform.em.stitching.domain;

import org.springframework.stereotype.*;

@Component
public class PageRangeFormat implements PageNumberFormatStrategy {
    @Override
    public String getPageNumber(int pageNumber, int noOfPages) {
        return String.valueOf(noOfPages);
    }
}
