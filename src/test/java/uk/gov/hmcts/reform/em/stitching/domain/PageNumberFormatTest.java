package uk.gov.hmcts.reform.em.stitching.domain;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PageNumberFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageNumberFormatTest {

    private static final int NO_OF_PAGES = 10;
    private static final int PAGE_NUMBER = 13;
    private static final String PAGE_RANGE = (PAGE_NUMBER + 1) + " - " + (PAGE_NUMBER + NO_OF_PAGES);

    @Test
    void testPageRange() {
        assertEquals(PAGE_RANGE, PageNumberFormat.PAGE_RANGE.getPageNumber(PAGE_NUMBER, NO_OF_PAGES));
    }

    @Test
    void testNumberOfPagesSingle() {
        assertEquals("1 page", PageNumberFormat.NUMBER_OF_PAGES.getPageNumber(PAGE_NUMBER, 1));
    }

    @Test
    void testNumberOfPagesMultiple() {
        assertEquals("10 pages", PageNumberFormat.NUMBER_OF_PAGES.getPageNumber(PAGE_NUMBER, NO_OF_PAGES));
    }

    @Test
    void testGetPageNumberTitleNumberOfPages() {
        assertEquals("Total Pages", PageNumberFormat.NUMBER_OF_PAGES.getPageNumberTitle());
    }

    @Test
    void testGetPageNumberTitlePageRange() {
        assertEquals("Page", PageNumberFormat.PAGE_RANGE.getPageNumberTitle());
    }
}
