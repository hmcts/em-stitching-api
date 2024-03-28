package uk.gov.hmcts.reform.em.stitching.pdf;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@Setter
@Getter
@AllArgsConstructor
public class PDFText {
    private String text;
    private float xxOffset;
    private float yyOffset;
    private PDType1Font pdType1Font;
    private int fontSize;

    @Override
    public String toString() {
        return "PDFText["
            + "text=" + text + ", "
            + "xxOffset=" + xxOffset + ", "
            + "yyOffset=" + yyOffset + ", "
            + "pdType1Font=" + pdType1Font + ", "
            + "fontSize=" + fontSize + ']';
    }

}