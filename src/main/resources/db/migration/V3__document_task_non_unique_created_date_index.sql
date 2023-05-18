DROP INDEX CONCURRENTLY IF EXISTS versioned_doc_task_created_date_index;
Analyse;
CREATE INDEX CONCURRENTLY IF NOT EXISTS ver_doc_task_non_unique_created_date_index ON versioned_document_task(created_date);
Analyse;