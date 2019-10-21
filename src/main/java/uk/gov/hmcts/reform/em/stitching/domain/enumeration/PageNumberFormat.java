package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

public enum PageNumberFormat {

    numberOfPages {
        public String getPageNumber(int pageNumber, int noOfPages) {
            String pageText = noOfPages == 1 ? " page" : " pages";
            return String.valueOf(noOfPages) + pageText;
        }
    },
    pageRange {
        public String getPageNumber(int pageNumber, int noOfPages) {
            return (pageNumber + 1) + " - " + (pageNumber + noOfPages);
        }
    };

    public abstract String getPageNumber(int pageNumber, int noOfPages);
}
