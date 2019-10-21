package uk.gov.hmcts.reform.em.stitching.domain;

import org.junit.*;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.*;

import static org.junit.Assert.*;

public class PageNumberFormatTest {

    private static final int noOfPages = 10;
    private static final int pageNumber = 13;
    private static final String pageRange = (pageNumber + 1) + " - " + (pageNumber + noOfPages);

    @Test
    public void testPageRange() {
        assertEquals(pageRange, PageNumberFormat.pageRange.getPageNumber(pageNumber, noOfPages));
    }

    @Test
    public void testNumberOfPagesSingle() {
        assertEquals("1 page", PageNumberFormat.numberOfPages.getPageNumber(pageNumber, 1));
    }

    @Test
    public void testNumberOfPagesMultiple() {
        assertEquals("10 pages", PageNumberFormat.numberOfPages.getPageNumber(pageNumber, noOfPages));
    }

    @Test
    public void testgetPageNumberTitleNumberOfPages(){
        assertEquals("Total Pages", PageNumberFormat.getPageNumberTitle(PageNumberFormat.numberOfPages));
    }

    @Test
    public void testgetPageNumberTitlePageRange(){
        assertEquals("Page", PageNumberFormat.getPageNumberTitle(PageNumberFormat.pageRange));
    }
}
