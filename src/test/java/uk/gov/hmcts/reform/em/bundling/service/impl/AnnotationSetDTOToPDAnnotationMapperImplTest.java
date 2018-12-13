package uk.gov.hmcts.reform.em.bundling.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.em.bundling.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.bundling.service.dto.external.annotation.CommentDTO;
import uk.gov.hmcts.reform.em.bundling.service.dto.external.annotation.IdamDetailsDTO;
import uk.gov.hmcts.reform.em.bundling.service.dto.external.annotation.RectangleDTO;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class AnnotationSetDTOToPDAnnotationMapperImplTest {

    AnnotationSetDTOToPDAnnotationMapperImpl annotationSetDTOToPDAnnotationMapperImpl =
        new AnnotationSetDTOToPDAnnotationMapperImpl();

    @Test
    public void testSuccessfulRun() throws Exception {
        Set<AnnotationDTO> annotationSet = new HashSet<>();

        AnnotationDTO annotationDTO = new AnnotationDTO();
        annotationDTO.setPage(1);
        annotationDTO.setAnnotationType("highlight");

        annotationDTO.setLastModifiedByDetails(new IdamDetailsDTO("john", "smith"));
        annotationDTO.setCreatedDate(Instant.now());
        annotationDTO.setLastModifiedDate(Instant.now());

        Set<RectangleDTO> rectangles = new HashSet<>();

        RectangleDTO rectangleDTO = new RectangleDTO();
        rectangleDTO.setX(0d);
        rectangleDTO.setY(0d);
        rectangleDTO.setHeight(10d);
        rectangleDTO.setWidth(10d);
        rectangles.add(rectangleDTO);

        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setContent("comment");

        annotationDTO.setRectangles(rectangles);
        annotationDTO.getComments().add(commentDTO);

        annotationSet.add(annotationDTO);

        PDDocument pdDocument = new PDDocument();

        PDPage page = new PDPage();
        pdDocument.addPage( page );

        Float mediaBoxUpperRightY = page.getMediaBox().getUpperRightY();

        annotationSetDTOToPDAnnotationMapperImpl.toNativeAnnotationsPerPage(pdDocument, annotationSet);

        Assert.assertNotNull(page.getAnnotations());
        Assert.assertEquals(2, page.getAnnotations().size());
        Assert.assertEquals(1f, page.getAnnotations().get(0).getColor().getComponents()[0], 0);
        Assert.assertEquals(1f, page.getAnnotations().get(0).getColor().getComponents()[1], 0);
        Assert.assertEquals(0f, page.getAnnotations().get(0).getColor().getComponents()[2], 0);

        Assert.assertEquals(rectangleDTO.getX().floatValue(), page.getAnnotations().get(0).getRectangle().getLowerLeftX(), 0);
        Assert.assertEquals(rectangleDTO.getX().floatValue()+rectangleDTO.getWidth().floatValue(), page.getAnnotations().get(0).getRectangle().getUpperRightX(), 0 );

        Assert.assertEquals(mediaBoxUpperRightY - rectangleDTO.getY().floatValue(), page.getAnnotations().get(0).getRectangle().getUpperRightY(), 0 );
        Assert.assertEquals(mediaBoxUpperRightY - (rectangleDTO.getY().floatValue()+rectangleDTO.getHeight().floatValue()), page.getAnnotations().get(0).getRectangle().getLowerLeftY(), 0 );


    }

}
