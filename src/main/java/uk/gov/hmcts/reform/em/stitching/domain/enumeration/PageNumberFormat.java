package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

public enum PageNumberFormat {

    numberOfPages {
        public String getPageNumber(int pageNumber, int noOfPages) {
            return String.valueOf(noOfPages);
        }
    },
    pageRange {
        public String getPageNumber(int pageNumber, int noOfPages) {
            return (pageNumber + 1) + " - " + (pageNumber + noOfPages);
        }
    };

    public abstract String getPageNumber(int pageNumber, int noOfPages);
}
