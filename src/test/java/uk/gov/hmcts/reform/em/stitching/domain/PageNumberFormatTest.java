package uk.gov.hmcts.reform.em.stitching.domain;


import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PageNumberFormat;

import static org.junit.Assert.assertEquals;

public class PageNumberFormatTest {

    private static final int noOfPages = 10;
    private static final int pageNumber = 13;
    private static final String pageRange = (pageNumber + 1) + " - " + (pageNumber + noOfPages);

    @Test
    public void testPageRange() {
        assertEquals(pageRange, PageNumberFormat.PAGE_RANGE.getPageNumber(pageNumber, noOfPages));
    }

    @Test
    public void testNumberOfPagesSingle() {
        assertEquals("1 page", PageNumberFormat.NUMBER_OF_PAGES.getPageNumber(pageNumber, 1));
    }

    @Test
    public void testNumberOfPagesMultiple() {
        assertEquals("10 pages", PageNumberFormat.NUMBER_OF_PAGES.getPageNumber(pageNumber, noOfPages));
    }

    @Test
    public void testGetPageNumberTitleNumberOfPages() {
        assertEquals("Total Pages", PageNumberFormat.NUMBER_OF_PAGES.getPageNumberTitle());
    }

    @Test
    public void testGetPageNumberTitlePageRange() {
        assertEquals("Page", PageNumberFormat.PAGE_RANGE.getPageNumberTitle());
    }
}
