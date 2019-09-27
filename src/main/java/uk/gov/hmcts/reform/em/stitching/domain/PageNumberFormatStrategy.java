package uk.gov.hmcts.reform.em.stitching.domain;

public interface PageNumberFormatStrategy {

    String getPageNumber(int pageNumber, int noOfPages);

    static PageNumberFormatStrategy pageRange() {
        return (pageNumber, noOfPages) -> (pageNumber + 1) + " - " + (pageNumber + noOfPages);
    }

    static PageNumberFormatStrategy numberOfPages() {
        return (pageNumber, noOfPages) -> String.valueOf(noOfPages);
    }
}
