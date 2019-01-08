package uk.gov.hmcts.reform.em.stitching.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.em.stitching.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.stitching.domain.DocumentTask;
import uk.gov.hmcts.reform.em.stitching.service.AnnotationSetFetcher;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.stitching.service.PdfAnnotator;
import uk.gov.hmcts.reform.em.stitching.service.impl.DocumentTaskProcessingException;

import javax.persistence.EntityManagerFactory;

@EnableBatchProcessing
@Configuration
public class BatchConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public DmStoreUploader dmStoreUploader;

    @Autowired
    public DmStoreDownloader dmStoreDownloader;

    @Autowired
    public PdfAnnotator pdfAnnotator;

    @Autowired
    public EntityManagerFactory entityManagerFactory;

    @Autowired
    public AnnotationSetFetcher annotationSetFetcher;

//    @Bean
//    public JdbcCursorItemReader<DocumentTask> reader(DataSource dataSource) {
//        return new JdbcCursorItemReaderBuilder<DocumentTask>()
//            .name("jdbc-task-reader")
//            .dataSource(dataSource)
//            .sql("select id, input_document_id, output_document_id, task_state, failure_description, created_by, created_date, " +
//                "last_modified_by, last_modified_date from document_task where task_state='NEW'")
//            .rowMapper((rs, i) -> {
//                DocumentTask documentTask = new DocumentTask();
//                documentTask.setId(rs.getLong("id"));
//                documentTask.setInputDocumentId(rs.getString("input_document_id"));
//                documentTask.setOutputDocumentId(rs.getString("output_document_id"));
//                documentTask.setTaskState(TaskState.valueOf(rs.getString("task_state")));
//                return documentTask;
//            })
//            .build();
//    }

    @Bean
    public JpaPagingItemReader itemReader() {
        return new JpaPagingItemReaderBuilder<DocumentTask>()
            .name("documentTaskReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("select t from DocumentTask t where t.taskState = 'NEW'")
            .pageSize(1000)
            .build();
    }

    @Bean
    public DocumentTaskItemProcessor processor() {
        return new DocumentTaskItemProcessor(dmStoreDownloader, pdfAnnotator, dmStoreUploader, annotationSetFetcher);
    }

    @Bean
    public JpaItemWriter itemWriter() {
        JpaItemWriter writer = new JpaItemWriter<DocumentTask>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

//    @Bean
//    public JdbcBatchItemWriter<DocumentTask> writer(DataSource dataSource) {
//        return new JdbcBatchItemWriterBuilder<DocumentTask>()
//            .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
//            .sql("update document_task set task_state = :taskState, output_document_id = :outputDocumentId, failure_description = :failureDescription where id = :id")
//            .dataSource(dataSource)
//            .build();
//    }

    //JobCompletionNotificationListener listener,
    @Bean
    public Job processDocument(Step step1) {
        return jobBuilderFactory.get("processDocumentJob")
            .incrementer(new RunIdIncrementer())
            //.listener(listener)
            .flow(step1)
            .end()
            .build();
    }

    @Bean
    public Step step1() {
        return new FaultTolerantStepBuilder<DocumentTask, DocumentTask> (stepBuilderFactory.get("step1"))
            .<DocumentTask, DocumentTask> chunk(10)
            .faultTolerant().noRollback(DocumentTaskProcessingException.class)
            .reader(itemReader())
            .processor(processor())
            .writer(itemWriter())
            .build();

    }

}
