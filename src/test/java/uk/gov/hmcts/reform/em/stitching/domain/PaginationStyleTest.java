package uk.gov.hmcts.reform.em.stitching.domain;

import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaginationStyleTest {

    private PDPage page;

    @BeforeEach
    void setup() {
        page = new PDPage();
    }

    @Test
    void testGetPageLocationOff() {
        Pair<Float, Float> result = PaginationStyle.off.getPageLocation(page);
        assertNull(result);
    }

    @Test
    void testGetPageLocationTopLeft() {
        Pair result = PaginationStyle.topLeft.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 20.0);
        assertEquals(result.getSecond(), (float) 20.0);
    }

    @Test
    void testGetPageLocationTopCenter() {
        Pair result = PaginationStyle.topCenter.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 306.0);
        assertEquals(result.getSecond(), (float) 20.0);
    }

    @Test
    void testGetPageLocationTopRight() {
        Pair result = PaginationStyle.topRight.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 572.0);
        assertEquals(result.getSecond(), (float) 20.0);
    }

    @Test
    void testGetPageLocationBottomLeft() {
        Pair result = PaginationStyle.bottomLeft.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 20.0);
        assertEquals(result.getSecond(), (float) 772.0);
    }

    @Test
    void testGetPageLocationBottomCenter() {
        Pair result = PaginationStyle.bottomCenter.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 306.0);
        assertEquals(result.getSecond(), (float) 772.0);
    }

    @Test
    void testGetPageLocationBottomRight() {
        Pair result = PaginationStyle.bottomRight.getPageLocation(page);
        assertEquals(result.getFirst(), (float) 572.0);
        assertEquals(result.getSecond(), (float) 772.0);
    }
}
