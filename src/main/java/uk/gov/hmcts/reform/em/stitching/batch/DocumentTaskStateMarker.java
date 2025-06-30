package uk.gov.hmcts.reform.em.stitching.batch;


import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.stitching.repository.DocumentTaskRepository;

@Component
public class DocumentTaskStateMarker {
    private final DocumentTaskRepository documentTaskRepository;

    public DocumentTaskStateMarker(DocumentTaskRepository documentTaskRepository) {
        this.documentTaskRepository = documentTaskRepository;
    }

    /**
     * Finds a DocumentTask by its ID and updates its state to IN_PROGRESS.
     * This change is committed to the database immediately in a new transaction.
     *
     * @param taskId The ID of the task to update.
     * @throws EntityNotFoundException if no task with the given ID is found.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void commitTaskAsInProgress(Long taskId) {
        DocumentTask task = documentTaskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        task.setTaskState(TaskState.IN_PROGRESS);
    }
}