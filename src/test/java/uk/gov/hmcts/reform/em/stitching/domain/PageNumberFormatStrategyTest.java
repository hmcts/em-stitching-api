package uk.gov.hmcts.reform.em.stitching.domain;

import org.junit.*;

import static org.junit.Assert.*;

public class PageNumberFormatStrategyTest {

    private static final int noOfPages = 10;
    private static final int pageNumber = 13;
    private static final String pageRange = (pageNumber + 1) + " - " + (pageNumber + noOfPages);

    @Test
    public void testPageRange() {
        assertEquals(pageRange, PageNumberFormatStrategy.pageRange().getPageNumber(pageNumber, noOfPages));
    }

    @Test
    public void testNumberOfPages() {
        assertEquals(String.valueOf(noOfPages), PageNumberFormatStrategy.numberOfPages().getPageNumber(pageNumber, noOfPages));
    }
}
