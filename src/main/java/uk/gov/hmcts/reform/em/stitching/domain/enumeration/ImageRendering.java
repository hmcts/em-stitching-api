package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

import org.apache.pdfbox.multipdf.Overlay;

public enum ImageRendering {

    opaque(Overlay.Position.FOREGROUND),
    translucent(Overlay.Position.BACKGROUND);

    private Overlay.Position position;

    ImageRendering(Overlay.Position position) {
        this.position = position;
    }

    public Overlay.Position getPosition() {
        return position;
    }
}
