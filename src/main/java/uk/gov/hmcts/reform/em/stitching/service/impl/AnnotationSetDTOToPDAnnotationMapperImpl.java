package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.service.AnnotationSetDTOToPDAnnotationMapper;
import uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation.CommentDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation.IdamDetailsDTO;
import uk.gov.hmcts.reform.em.stitching.service.dto.external.annotation.RectangleDTO;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnotationSetDTOToPDAnnotationMapperImpl implements AnnotationSetDTOToPDAnnotationMapper {

    @Override
    public void toNativeAnnotationsPerPage(PDDocument document, Set<AnnotationDTO> annotations) throws DocumentTaskProcessingException {

        for (AnnotationDTO annotationDTO : annotations) {
            processAnnotationDto(document, annotationDTO);
        }
    }

    private void processAnnotationDto(PDDocument document, AnnotationDTO annotationDTO) throws DocumentTaskProcessingException {
        Integer pageNumber = annotationDTO.getPage() - 1;

        PDPage pdPage = document.getPage(pageNumber);

        try {
            List<PDAnnotation> currentPageList = pdPage.getAnnotations();

            String allComments = annotationDTO.getComments() != null ?
                    annotationDTO.getComments().stream().sorted(Comparator.comparing(CommentDTO::getCreatedDate)).map(CommentDTO::getContent).collect(Collectors.joining("\n")) : null;

            if (annotationDTO.getRectangles() != null) {

                annotationDTO.getRectangles().forEach(rectangleDTO -> {

                    PDRectangle position = rectangleDTOToPDRectangle(pdPage, rectangleDTO);

                    PDAnnotationTextMarkup markup = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                    markup.setContents(allComments);
                    markup.setColor(getColor(annotationDTO));
                    markup.setRectangle(position);
                    markup.setQuadPoints(getQuadsWithRectangle(position));
                    markup.setPrinted(true);

                    if (annotationDTO.getCreatedDate() != null) {
                        markup.setCreationDate(GregorianCalendar.from(ZonedDateTime.ofInstant(annotationDTO.getCreatedDate(), ZoneId.systemDefault())));
                    }
                    if (annotationDTO.getLastModifiedDate() != null) {
                        markup.setModifiedDate(GregorianCalendar.from(ZonedDateTime.ofInstant(annotationDTO.getLastModifiedDate(), ZoneId.systemDefault())));
                    }
                    if (annotationDTO.getLastModifiedByDetails() != null ) {
                        markup.getCOSObject().setItem(COSName.T, new COSString(extractUserDetails(annotationDTO.getLastModifiedByDetails())));
                    } else if (annotationDTO.getCreatedByDetails() != null) {
                        markup.getCOSObject().setItem(COSName.T, new COSString(extractUserDetails(annotationDTO.getCreatedByDetails())));
                    }

                    currentPageList.add(markup);

                    PDAnnotationPopup pdAnnotationPopup = new PDAnnotationPopup();
                    pdAnnotationPopup.setContents(allComments);
                    pdAnnotationPopup.setRectangle(position);
                    pdAnnotationPopup.setPrinted(true);

                    if (annotationDTO.getLastModifiedDate() != null) {
                        pdAnnotationPopup.setModifiedDate(GregorianCalendar.from(ZonedDateTime.ofInstant(annotationDTO.getLastModifiedDate(), ZoneId.systemDefault())));
                    }
                    currentPageList.add(pdAnnotationPopup);

                });
            }
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Can't retrieve annotations from the document ", e);
        }
    }

    private String extractUserDetails(IdamDetailsDTO idamDetailsDTO) {
        if (idamDetailsDTO != null && idamDetailsDTO.getForename() != null && idamDetailsDTO.getSurname() != null) {
            return idamDetailsDTO.getForename() + " " + idamDetailsDTO.getSurname();
        }
        return null;
    }

    private PDColor getColor(AnnotationDTO annotationDTO) {
        if (annotationDTO.getColor() != null && annotationDTO.getColor().length() == 6) {
            int c = Integer.parseInt(annotationDTO.getColor(), 16);
            float r = ((c & 0xFF0000) >> 16) / 255f;
            float g = ((c & 0x00FF00) >> 8) / 255f;
            float b = ((c & 0x0000FF) >> 0) / 255f;
            return new PDColor( new float[] { r, g, b }, PDDeviceRGB.INSTANCE);
        } else {
            return new PDColor(new float[]{1f, 1f, 0}, PDDeviceRGB.INSTANCE);
        }
    }

    private PDRectangle rectangleDTOToPDRectangle(PDPage pdPage, RectangleDTO rectangleDTO) {
        PDRectangle pdRectangle = new PDRectangle();
        pdRectangle.setLowerLeftX(rectangleDTO.getX().floatValue());
        pdRectangle.setLowerLeftY(pdPage.getMediaBox().getUpperRightY() - (rectangleDTO.getY().floatValue() + rectangleDTO.getHeight().floatValue()));
        pdRectangle.setUpperRightX((rectangleDTO.getX().floatValue() + rectangleDTO.getWidth().floatValue()));
        pdRectangle.setUpperRightY(pdPage.getMediaBox().getUpperRightY() - rectangleDTO.getY().floatValue());
        return pdRectangle;
    }

    public static float[] getQuadsWithRectangle(PDRectangle position) {

        // work out the points forming the four corners of the annotations
        // set out in anti clockwise form (Completely wraps the text)
        // OK, the below doesn't match that description.
        // It's what acrobat 7 does and displays properly!
        float[] quads = new float[8];

        quads[0] = position.getLowerLeftX();  // x1
        quads[1] = position.getUpperRightY() - 2; // y1
        quads[2] = position.getUpperRightX(); // x2
        quads[3] = quads[1]; // y2
        quads[4] = quads[0];  // x3
        quads[5] = position.getLowerLeftY() - 2; // y3
        quads[6] = quads[2]; // x4
        quads[7] = quads[5]; // y5

        return quads;
    }


}
