package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.PaginationStyle;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFUtilityTest {

    @Test
    void addCenterTextAddsTextToCenter() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDFUtility.addCenterText(document, page, "Centered Text");

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("Centered Text"));

        document.close();
    }

    @Test
    void addCenterTextHandlesLargeMultilineText() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        String largeText = "This is a large text that should be split into multiple lines. ".repeat(20);
        PDFUtility.addCenterText(document, page, largeText);

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("This is a large text"));

        document.close();
    }

    @Test
    void addCenterTextHandlesNullText() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDFUtility.addCenterText(document, page, null);

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.isEmpty());

        document.close();
    }

    @Test
    void addTextAddsTextToPage() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDFText pdfText = new PDFText("Sample Text", 100, 700,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        PDFUtility.addText(document, page, pdfText);

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("Sample Text"));

        document.close();
    }

    @Test
    void addTextHandlesNullText() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDFText pdfText = new PDFText(null, 100, 700,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        PDFUtility.addText(document, page, pdfText);

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.isEmpty());

        document.close();
    }

    @Test
    void addPageNumbersAddsNumbersToPages() throws IOException {
        PDDocument document = new PDDocument();
        for (int i = 0; i < 5; i++) {
            document.addPage(new PDPage());
        }

        PDFUtility.addPageNumbers(document, PaginationStyle.bottomRight, 0, 5);

        String text = new PDFTextStripper().getText(document);
        for (int i = 1; i <= 5; i++) {
            assertTrue(text.contains(String.valueOf(i)));
        }

        document.close();
    }

    @Test
    void addPageNumbersHandlesInvalidRange() throws IOException {
        PDDocument document = new PDDocument();
        for (int i = 0; i < 5; i++) {
            document.addPage(new PDPage());
        }

        PDFUtility.addPageNumbers(document, PaginationStyle.bottomRight, 5, 0);

        String text = new PDFTextStripper().getText(document);
        for (int i = 1; i <= 5; i++) {
            assertTrue(text.isEmpty());
        }

        document.close();
    }

    @Test
    void getStringWidthCalculatesWidthCorrectly() {
        float width = PDFUtility.getStringWidth("Sample Text",
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        assertTrue(width > 0);
    }

    @Test
    void getStringWidthHandlesIOException() throws IOException {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        String text = "Sample Text";

        PDFont spyFont = Mockito.spy(font);
        Mockito.doThrow(IOException.class).when(spyFont).getStringWidth(Mockito.anyString());

        float width = PDFUtility.getStringWidth(text, spyFont, 12);
        assertEquals(0, width);
    }

    @Test
    void addSubtitleLinkAddsLinkToPage() throws IOException {
        PDDocument document = new PDDocument();
        PDPage from = new PDPage();
        PDPage to = new PDPage();
        document.addPage(from);
        document.addPage(to);

        PDFUtility.addSubtitleLink(document, from, to, "Subtitle Link", 700,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA));

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("Subtitle Link"));

        document.close();
    }

    @Test
    void addSubtitleLinkHandlesNullText() throws IOException {
        PDDocument document = new PDDocument();
        PDPage from = new PDPage();
        PDPage to = new PDPage();
        document.addPage(from);
        document.addPage(to);

        PDFUtility.addSubtitleLink(document, from, to, null, 700,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA));

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.isEmpty());

        document.close();
    }

    @Test
    void sanitizeTextRemovesUnsupportedCharacters() {
        String sanitized = PDFUtility.sanitizeText("Sample Text with unsupported char: •");
        assertEquals("Sample Text with unsupported char: ", sanitized);
    }

    @Test
    void sanitizeTextHandlesEmptyText() {
        String sanitized = PDFUtility.sanitizeText("");
        assertEquals("", sanitized);
    }

    @Test
    void splitStringSplitsTextCorrectly() {
        String[] lines = PDFUtility.splitString("This is a sample text that should be split into multiple lines",
            100, new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        assertTrue(lines.length > 1);
    }

    @Test
    void splitStringHandlesNullText() {
        String[] lines = PDFUtility.splitString(null, 100,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        assertEquals(0, lines.length);
    }

    @Test
    void splitStringHandlesEmptyText() {
        String[] lines = PDFUtility.splitString("", 100,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        assertEquals(0, lines.length);
    }

    @Test
    void splitStringHandlesIllegalArgumentException() throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        String text = "This is a sample text that should be split into multiple lines";

        PDType1Font spyFont = Mockito.spy(font);
        Mockito.doThrow(IllegalArgumentException.class)
            .doCallRealMethod()
            .when(spyFont).getStringWidth(Mockito.anyString());

        String[] lines = PDFUtility.splitString(text, 100, spyFont, 12);
        assertTrue(lines.length > 1);
    }

    @Test
    void addLinkAddsLinkToPage() throws IOException {
        PDDocument document = new PDDocument();
        PDPage from = new PDPage();
        PDPage to = new PDPage();
        document.addPage(from);
        document.addPage(to);

        PDFLink pdfLink = new PDFLink("Link Text", 700, 100,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12, to);
        PDFUtility.addLink(document, from, pdfLink, 100);

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("Link Text"));

        document.close();
    }

    @Test
    void addRightLinkAddsLinkToPage() throws IOException {
        PDDocument document = new PDDocument();
        PDPage from = new PDPage();
        PDPage to = new PDPage();
        document.addPage(from);
        document.addPage(to);

        PDFUtility.addRightLink(document, from, to, "Right Link", 700,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);

        String text = new PDFTextStripper().getText(document);
        assertTrue(text.contains("Right Link"));

        document.close();
    }
}