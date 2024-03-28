package uk.gov.hmcts.reform.em.stitching.pdf;

import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@Setter
@Getter
public class PDFLink extends PDFText {
    private PDPage destination;

    public PDFLink(String text, float xxOffset, float yyOffset,
                   PDType1Font pdType1Font, int fontSize, PDPage destination) {
        super(text, xxOffset, yyOffset, pdType1Font, fontSize);
        this.destination = destination;
    }

}
