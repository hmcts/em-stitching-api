package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class PDFLink extends PDFText {
    private PDPage destination;

    public PDFLink(String text, float xxOffset, float yyOffset,
                   PDType1Font pdType1Font, int fontSize, PDPage destination) {
        super(text, xxOffset, yyOffset, pdType1Font, fontSize);
        this.destination = destination;
    }



    public PDPage getDestination() {
        return destination;
    }

    public void setDestination(PDPage destination) {
        this.destination = destination;
    }
}
