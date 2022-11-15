package uk.gov.hmcts.reform.em.stitching.domain.enumeration;

import org.springframework.data.util.Pair;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * The Pagination Style enumeration.
 */
@SuppressWarnings("all")
public enum PaginationStyle {
    off {
        public Pair getPageLocation(PDPage page) {
            return null;
        }
    }, topLeft {
        public Pair getPageLocation(PDPage page) {
            return Pair.of((page.getMediaBox().getLowerLeftX() + offset), (page.getMediaBox().getLowerLeftY() + offset));
        }
    }, topCenter {
        public Pair getPageLocation(PDPage page) {
            return Pair.of((page.getMediaBox().getWidth() / 2), (page.getMediaBox().getLowerLeftY() + offset));
        }
    }, topRight {
        public Pair getPageLocation(PDPage page) {
            return Pair.of((page.getMediaBox().getWidth() - right_side_offset), (page.getMediaBox().getLowerLeftY() + offset));
        }
    }, bottomLeft {
        public Pair getPageLocation(PDPage page) {
            return Pair.of((page.getMediaBox().getLowerLeftX() + offset), (page.getMediaBox().getHeight() - offset));
        }
    }, bottomCenter {
        public Pair getPageLocation(PDPage page) {
            return Pair.of((page.getMediaBox().getWidth() / 2), (page.getMediaBox().getHeight() - offset));
        }
    }, bottomRight {
        public Pair getPageLocation(PDPage page) {
            return Pair.of((page.getMediaBox().getWidth() - right_side_offset), (page.getMediaBox().getHeight() - offset));
        }
    };

    private static final int offset = 20;
    private static final int right_side_offset = 40;

    public abstract Pair<Float, Float> getPageLocation(PDPage page);
}
