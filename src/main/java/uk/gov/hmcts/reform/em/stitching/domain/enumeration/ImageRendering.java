package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.pdfbox.multipdf.Overlay;

public enum ImageRendering {
    @JsonProperty("opaque")
    OPAQUE(Overlay.Position.FOREGROUND),
    @JsonProperty("translucent")
    TRANSLUCENT(Overlay.Position.BACKGROUND);

    private Overlay.Position position;

    ImageRendering(Overlay.Position position) {
        this.position = position;
    }

    public Overlay.Position getPosition() {
        return position;
    }
}
