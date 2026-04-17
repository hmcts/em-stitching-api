package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.gov.hmcts.reform.em.stitching.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.info.BuildInfo;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;
import uk.gov.hmcts.reform.em.stitching.service.dto.DocumentTaskDTO;
import uk.gov.hmcts.reform.em.stitching.service.mapper.DocumentTaskMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTaskServiceImplTest {

    @Mock
    private DocumentTaskRepository documentTaskRepository;

    @Mock
    private DocumentTaskMapper documentTaskMapper;

    @Mock
    private BuildInfo buildInfo;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DocumentTaskServiceImpl documentTaskService;

    private static final Long TASK_ID = 1L;
    private static final String OWNER = "user-abc";
    private static final String OTHER_USER = "user-xyz";

    @Test
    void findOneReturnsTaskWhenOwnedByCurrentUser() {
        DocumentTask task = new DocumentTask();
        DocumentTaskDTO taskDTO = new DocumentTaskDTO();
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of(OWNER));
        when(documentTaskRepository.findByIdAndCreatedBy(TASK_ID, OWNER)).thenReturn(Optional.of(task));
        when(documentTaskMapper.toDto(task)).thenReturn(taskDTO);

        Optional<DocumentTaskDTO> result = documentTaskService.findOne(TASK_ID);

        assertTrue(result.isPresent());
        assertEquals(taskDTO, result.get());
        verify(documentTaskRepository).findByIdAndCreatedBy(TASK_ID, OWNER);
    }

    @Test
    void findOneReturnsEmptyWhenTaskBelongsToDifferentUser() {
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of(OTHER_USER));
        when(documentTaskRepository.findByIdAndCreatedBy(TASK_ID, OTHER_USER)).thenReturn(Optional.empty());

        Optional<DocumentTaskDTO> result = documentTaskService.findOne(TASK_ID);

        assertTrue(result.isEmpty());
        verify(documentTaskRepository).findByIdAndCreatedBy(TASK_ID, OTHER_USER);
    }

    @Test
    void findOneThrowsWhenNoAuthenticatedUser() {
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> documentTaskService.findOne(TASK_ID));

        verify(documentTaskRepository, never()).findByIdAndCreatedBy(TASK_ID, null);
    }

    @Test
    void findOneReturnsEmptyWhenTaskDoesNotExist() {
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of(OWNER));
        when(documentTaskRepository.findByIdAndCreatedBy(TASK_ID, OWNER)).thenReturn(Optional.empty());

        Optional<DocumentTaskDTO> result = documentTaskService.findOne(TASK_ID);

        assertTrue(result.isEmpty());
    }
}
