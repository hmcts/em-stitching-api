package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.apache.tika.Tika;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.conversion.FileToPDFConverter;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static pl.touk.throwing.ThrowingFunction.unchecked;

@Transactional
public class DocumentConversionServiceImpl implements DocumentConversionService {

    private final List<FileToPDFConverter> converters;
    private final Tika tika;

    public DocumentConversionServiceImpl(List<FileToPDFConverter> converters, Tika tika) {
        this.converters = converters;
        this.tika = tika;
    }

    @Override
    public Pair<BundleDocument, File> convert(Pair<BundleDocument, File> pair) throws IOException {
        File originalFile = pair.getSecond();
        String mimeType = tika.detect(originalFile);
        File convertedFile = converters.stream()
            .filter(f -> f.accepts().contains(mimeType))
            .findFirst()
            .map(unchecked(f -> f.convert(originalFile)))
            .orElseThrow(() -> new IOException("Unknown file type: " + mimeType));

        return Pair.of(pair.getFirst(), convertedFile);
    }

}

