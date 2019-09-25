package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

public enum PageNumberFormat {

    NUMBER_OF_PAGES("NumberOfPages"),
    PAGE_RANGE("PageRange");

    private String value;

    PageNumberFormat(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return this.value();
    }
}
