package uk.gov.hmcts.reform.em.stitching.pdf;

import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class PDFText {
    private String text;
    private float xxOffset;
    private float yyOffset;
    private PDType1Font pdType1Font;
    private int fontSize;

    public PDFText(String text, float xxOffset, float yyOffset,
                   PDType1Font pdType1Font, int fontSize) {
        this.text = text;
        this.xxOffset = xxOffset;
        this.yyOffset = yyOffset;
        this.pdType1Font = pdType1Font;
        this.fontSize = fontSize;
    }

    public PDFText(PDFText pdfText) {
        this.text = pdfText.getText();
        this.xxOffset = pdfText.getXxOffset();
        this.yyOffset = pdfText.getYyOffset();
        this.pdType1Font = pdfText.getPdType1Font();
        this.fontSize = pdfText.getFontSize();
    }

    public PDFText() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public float getXxOffset() {
        return xxOffset;
    }

    public void setXxOffset(float xxOffset) {
        this.xxOffset = xxOffset;
    }

    public float getYyOffset() {
        return yyOffset;
    }

    public void setYyOffset(float yyOffset) {
        this.yyOffset = yyOffset;
    }

    public PDType1Font getPdType1Font() {
        return pdType1Font;
    }

    public void setPdType1Font(PDType1Font pdType1Font) {
        this.pdType1Font = pdType1Font;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

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