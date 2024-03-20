package uk.gov.hmcts.reform.em.stitching.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Objects;

public class CloseableCloser {

    private static final Logger log = LoggerFactory.getLogger(CloseableCloser.class);

    private CloseableCloser() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instatiated");
    }

    public static void close(Closeable closeable) {
        try {
            if (Objects.nonNull(closeable)) {
                closeable.close();
            }
        } catch (Exception ex) {
            log.info("Closing {} failed.Exception:{}", closeable.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
