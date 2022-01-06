package uk.gov.hmcts.reform.em.stitching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;

import java.time.Instant;
import java.util.List;

@Repository
public interface DocumentTaskRepository extends JpaRepository<DocumentTask, Long> {

    @Query(value = "SELECT m.id  FROM versioned_document_task m WHERE m.created_date <= :createdDate", nativeQuery = true)
    List<Long> findAllByCreatedDate(@Param("createdDate") Instant date);

}
