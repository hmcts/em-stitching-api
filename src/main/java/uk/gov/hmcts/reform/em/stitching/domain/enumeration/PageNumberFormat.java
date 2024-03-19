package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

public enum PageNumberFormat {

    NUMBER_OF_PAGES {
        public String getPageNumber(int pageNumber, int noOfPages) {
            String pageText = noOfPages == 1 ? " page" : " pages";
            return String.valueOf(noOfPages) + pageText;
        }

        public String getPageNumberTitle() {
            return "Total Pages";
        }
    },
    PAGE_RANGE {
        public String getPageNumber(int pageNumber, int noOfPages) {
            return (pageNumber + 1) + " - " + (pageNumber + noOfPages);
        }

        public String getPageNumberTitle() {
            return "Page";
        }
    };

    public abstract String getPageNumber(int pageNumber, int noOfPages);

    public abstract String getPageNumberTitle();
}
