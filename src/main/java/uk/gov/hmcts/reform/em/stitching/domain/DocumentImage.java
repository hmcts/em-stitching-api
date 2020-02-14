package uk.gov.hmcts.reform.em.stitching.domain;

import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;

public class DocumentImage {

    private String docmosisAssetId;
    private ImageRenderingLocation imageRenderingLocation;
    private int coordinateX;
    private int coordinateY;
    private ImageRendering imageRendering;

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

    public int getCoordinateX() {
        return coordinateX;
    }

    public void setCoordinateX(int coordinateX) {
        this.coordinateX = coordinateX;
    }

    public int getCoordinateY() {
        return coordinateY;
    }

    public void setCoordinateY(int coordinateY) {
        this.coordinateY = coordinateY;
    }

    public ImageRendering getImageRendering() {
        return imageRendering;
    }

    public void setImageRendering(ImageRendering imageRendering) {
        this.imageRendering = imageRendering;
    }

    public void verifyCoordinates() {
        if (getCoordinateX() < 0) {
            setCoordinateX(0);
        } else if (getCoordinateX() > 100) {
            setCoordinateX(100);
        }
        if (getCoordinateY() < 0) {
            setCoordinateY(0);
        } else if (getCoordinateY() > 100) {
            setCoordinateY(100);
        }
    }
}
