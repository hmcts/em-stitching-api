package uk.gov.hmcts.reform.em.stitching.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRendering;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.ImageRenderingLocation;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DocumentImage {

    private String docmosisAssetId;
    private ImageRenderingLocation imageRenderingLocation;
    private Integer coordinateX;
    private Integer coordinateY;
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

    public Integer getCoordinateX() {
        return coordinateX;
    }

    public void setCoordinateX(Integer coordinateX) {
        this.coordinateX = coordinateX;
    }

    public Integer getCoordinateY() {
        return coordinateY;
    }

    public void setCoordinateY(Integer coordinateY) {
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
