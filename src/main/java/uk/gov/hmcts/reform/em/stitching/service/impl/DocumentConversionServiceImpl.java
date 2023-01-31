package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.conversion.FileToPDFConverter;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static pl.touk.throwing.ThrowingFunction.unchecked;

public class DocumentConversionServiceImpl implements DocumentConversionService {

    private final List<FileToPDFConverter> converters;

    public DocumentConversionServiceImpl(List<FileToPDFConverter> converters) {
        this.converters = converters;
    }

    @Override
    public Pair<BundleDocument, File> convert(Pair<BundleDocument, FileAndMediaType> pair) throws IOException {
        FileAndMediaType fileAndMediaType = pair.getSecond();
        File convertedFile = converters.stream().parallel()
            .filter(f -> f.accepts().contains(fileAndMediaType.getMediaType().toString()))
            .findFirst()
            .map(unchecked(f -> f.convert(fileAndMediaType.getFile())))
            .orElseThrow(() -> new IOException("Unknown file type: " + fileAndMediaType.getMediaType().toString()));

        return Pair.of(pair.getFirst(), convertedFile);
    }

}

