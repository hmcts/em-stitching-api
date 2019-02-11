package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.touk.throwing.ThrowingFunction;
import uk.gov.hmcts.reform.em.stitching.domain.BundleDocument;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFCoversheetService;
import uk.gov.hmcts.reform.em.stitching.pdf.PDFMerger;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.DocumentConversionService;
import uk.gov.hmcts.reform.em.stitching.service.DocumentTaskService;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing DocumentTask.
 */
@Service
@Transactional
public class DocumentTaskServiceImpl implements DocumentTaskService {

    private final Logger log = LoggerFactory.getLogger(DocumentTaskServiceImpl.class);
    private final DocumentTaskRepository documentTaskRepository;
    private final DocumentTaskMapper documentTaskMapper;
    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;
    private final DocumentConversionService documentConverter;
    private final PDFCoversheetService coversheetService;
    private final PDFMerger pdfMerger;

    public DocumentTaskServiceImpl(DocumentTaskRepository documentTaskRepository,
                                   DocumentTaskMapper documentTaskMapper,
                                   DmStoreDownloader dmStoreDownloader,
                                   DmStoreUploader dmStoreUploader,
                                   DocumentConversionService documentConverter,
                                   PDFCoversheetService coversheetService,
                                   PDFMerger pdfMerger) {
        this.documentTaskRepository = documentTaskRepository;
        this.documentTaskMapper = documentTaskMapper;
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.documentConverter = documentConverter;
        this.coversheetService = coversheetService;
        this.pdfMerger = pdfMerger;
    }

    /**
     * Save a documentTask.
     *
     * @param documentTaskDTO the entity to save
     * @return the persisted entity
     */
    @Override
    @Transactional
    public DocumentTaskDTO save(DocumentTaskDTO documentTaskDTO) {
        log.debug("Request to save DocumentTask : {}", documentTaskDTO);
        DocumentTask documentTask = documentTaskMapper.toEntity(documentTaskDTO);
        documentTask = documentTaskRepository.save(documentTask);

        return documentTaskMapper.toDto(documentTask);
    }

    /**
     * Get one documentTask by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentTaskDTO> findOne(Long id) {
        log.debug("Request to get DocumentTask : {}", id);
        return documentTaskRepository.findById(id)
            .map(documentTaskMapper::toDto);
    }

    /**
     * Use the task processor to process the task
     *
     * @param documentTask task to process
     * @return updated dto
     */
    @Override
    @Transactional
    public DocumentTask process(DocumentTask documentTask) {
        try {
            List<Pair<BundleDocument, File>> documents = dmStoreDownloader
                    .downloadFiles(documentTask.getBundle().getSortedItems())
                    .map(ThrowingFunction.unchecked(documentConverter::convert))
                    .map(ThrowingFunction.unchecked(coversheetService::addCoversheet))
                    .collect(Collectors.toList());

            final File outputFile = pdfMerger.merge(documentTask.getBundle(), documents);

            dmStoreUploader.uploadFile(outputFile, documentTask);

            documentTask.setTaskState(TaskState.DONE);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);

            documentTask.setTaskState(TaskState.FAILED);
            documentTask.setFailureDescription(e.getMessage());
        }
        finally {
            documentTaskRepository.save(documentTask);
        }

        return documentTask;
    }
}
