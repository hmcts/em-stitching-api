package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.springframework.data.util.Pair;
import uk.gov.hmcts.reform.em.stitching.conversion.FileToPDFConverter;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;
import uk.gov.hmcts.reform.em.stitching.service.exception.DocmosisConversionException;

import java.io.File;
import java.util.List;

import static pl.touk.throwing.ThrowingFunction.unchecked;

public class DocumentConversionServiceImpl implements DocumentConversionService {

    private final List<FileToPDFConverter> converters;

    public DocumentConversionServiceImpl(List<FileToPDFConverter> converters) {
        this.converters = converters;
    }

    @Override
    public Pair<BundleDocument, File> convert(Pair<BundleDocument, FileAndMediaType> pair) {
        FileAndMediaType fileAndMediaType = pair.getSecond();
        File convertedFile = converters.stream().parallel()
            .filter(f -> f.accepts().contains(fileAndMediaType.getMediaType().toString()))
            .findFirst()
            .map(unchecked(f -> f.convert(fileAndMediaType.getFile())))
            .orElseThrow(() -> {
                String errMsg = String.format(
                        "Error converting document: %s with file type: %s",
                        pair.getFirst().getDocTitle(),fileAndMediaType.getMediaType().toString()
                );
                return new DocmosisConversionException(errMsg);
            });
        return Pair.of(pair.getFirst(), convertedFile);
    }

}

