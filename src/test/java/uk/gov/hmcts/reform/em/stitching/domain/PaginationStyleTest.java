package uk.gov.hmcts.reform.em.stitching.domain;

import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.data.util.Pair;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import static org.junit.Assert.*;

public class PaginationStyleTest {

    private PDPage page;

    @Before
    public void setup() {
        page = new PDPage();
    }

    @Test
    public void testGetPageLocationOff() {
        Pair result = PaginationStyle.off.getPageLocation(page);
        assertEquals(result, null);
    }

    @Test
    public void testGetPageLocationTopLeft() {
        Pair result = PaginationStyle.topLeft.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 20.0);
        assertEquals(result.getSecond(), (float) 20.0);
    }

    @Test
    public void testGetPageLocationTopCenter() {
        Pair result = PaginationStyle.topCenter.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 306.0);
        assertEquals(result.getSecond(), (float) 20.0);
    }

    @Test
    public void testGetPageLocationTopRight() {
        Pair result = PaginationStyle.topRight.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 572.0);
        assertEquals(result.getSecond(), (float) 20.0);
    }

    @Test
    public void testGetPageLocationBottomLeft() {
        Pair result = PaginationStyle.bottomLeft.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 20.0);
        assertEquals(result.getSecond(), (float) 772.0);
    }

    @Test
    public void testGetPageLocationBottomCenter() {
        Pair result = PaginationStyle.bottomCenter.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 306.0);
        assertEquals(result.getSecond(), (float) 772.0);
    }

    @Test
    public void testGetPageLocationBottomRight() {
        Pair result = PaginationStyle.bottomRight.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 572.0);
        assertEquals(result.getSecond(), (float) 772.0);
    }
}
