package uk.gov.hmcts.reform.em.stitching.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.stitching.domain.Bundle;
import uk.gov.hmcts.reform.em.stitching.repository.BundleRepository;
import uk.gov.hmcts.reform.em.stitching.service.BundleService;
import uk.gov.hmcts.reform.em.stitching.service.dto.StitchedDocumentDto;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BundleServiceImpl implements BundleService {

    private final Logger log = LoggerFactory.getLogger(BundleServiceImpl.class);

    private final BundleRepository bundleRepository;

    @Override
    public Optional<StitchedDocumentDto> findOne(Long id) {
        log.debug("Request to get Bundle with Id : {}", id);
        Optional<Bundle> bundle = bundleRepository.findById(id);
        if (bundle.isPresent()) {
            StitchedDocumentDto stitchedDocumentDto = new StitchedDocumentDto();
            if (StringUtils.isNotBlank(bundle.get().getStitchedDocumentURI())) {
                stitchedDocumentDto.setUrl(bundle.get().getStitchedDocumentURI());
                stitchedDocumentDto.setBinaryUrl(bundle.get().getStitchedDocumentURI() + "/binary");
            }
            stitchedDocumentDto.setFileName(bundle.get().getFileName());

            return Optional.of(stitchedDocumentDto);
        }

        return Optional.empty();
    }
}
