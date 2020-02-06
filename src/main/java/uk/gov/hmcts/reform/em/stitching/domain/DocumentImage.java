package uk.gov.hmcts.reform.em.stitching.domain;

import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;

public class DocumentImage {

    private boolean enabled;
    private String docmosisAssetId;
    private ImageRenderingLocation imageRenderingLocation;
    private Pair<Integer, Integer> imageCoordinates;
    private ImageRendering imageRendering;

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDocmosisAssetId() {
        return docmosisAssetId;
    }

    public void setDocmosisAssetId(String docmosisAssetId) {
        this.docmosisAssetId = docmosisAssetId;
    }

    public ImageRenderingLocation getImageRenderingLocation() {
        return imageRenderingLocation;
    }

    public void setImageRenderingLocation(ImageRenderingLocation imageRenderingLocation) {
        this.imageRenderingLocation = imageRenderingLocation;
    }

    public Pair<Integer, Integer> getImageCoordinates() {
        return imageCoordinates;
    }

    public void setImageCoordinates(Pair<Integer, Integer> imageCoordinates) {
        this.imageCoordinates = imageCoordinates;
    }

    public ImageRendering getImageRendering() {
        return imageRendering;
    }

    public void setImageRendering(ImageRendering imageRendering) {
        this.imageRendering = imageRendering;
    }
}
