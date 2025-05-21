package uk.gov.hmcts.reform.em.stitching.batch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveSpringBatchHistoryTaskletTest {

    @Mock
    private JdbcTemplate mockJdbcTemplate;

    @Mock
    private StepContribution mockStepContribution;

    @Mock
    private ChunkContext mockChunkContext;

    @Captor
    private ArgumentCaptor<Date> dateArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> sqlArgumentCaptor;

    private RemoveSpringBatchHistoryTasklet tasklet;

    private final int historicRetentionMilliseconds = (int) TimeUnit.DAYS.toMillis(30);
    private final String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

    private static final String SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT =
        "DELETE FROM %PREFIX%STEP_EXECUTION_CONTEXT "
            + "WHERE STEP_EXECUTION_ID IN (SELECT STEP_EXECUTION_ID FROM "
            + "%PREFIX%STEP_EXECUTION WHERE JOB_EXECUTION_ID IN "
            + "(SELECT JOB_EXECUTION_ID FROM  %PREFIX%JOB_EXECUTION where CREATE_TIME < ?))";

    private static final String SQL_DELETE_BATCH_STEP_EXECUTION =
        "DELETE FROM %PREFIX%STEP_EXECUTION WHERE JOB_EXECUTION_ID IN "
            + "(SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION where CREATE_TIME < ?)";

    private static final String SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT =
        "DELETE FROM %PREFIX%JOB_EXECUTION_CONTEXT WHERE "
            + "JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM  %PREFIX%JOB_EXECUTION where CREATE_TIME < ?)";

    private static final String SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS =
        "DELETE FROM %PREFIX%JOB_EXECUTION_PARAMS WHERE "
            + "JOB_EXECUTION_ID IN (SELECT JOB_EXECUTION_ID FROM %PREFIX%JOB_EXECUTION where CREATE_TIME < ?)";

    private static final String SQL_DELETE_BATCH_JOB_EXECUTION =
        "DELETE FROM %PREFIX%JOB_EXECUTION where CREATE_TIME < ?";

    private static final String SQL_DELETE_BATCH_JOB_INSTANCE =
        "DELETE FROM %PREFIX%JOB_INSTANCE WHERE JOB_INSTANCE_ID NOT IN "
            + "(SELECT JOB_INSTANCE_ID FROM %PREFIX%JOB_EXECUTION)";

    @BeforeEach
    void setUp() {
        tasklet = new RemoveSpringBatchHistoryTasklet(historicRetentionMilliseconds, mockJdbcTemplate);
    }

    private String getExpectedQuery(String baseQuery) {
        return StringUtils.replace(baseQuery, "%PREFIX%", tablePrefix);
    }

    @Test
    void executeDeletesOldBatchHistorySuccessfully() {
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT)), any(Date.class))).thenReturn(1);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION)), any(Date.class))).thenReturn(2);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT)), any(Date.class))).thenReturn(3);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS)), any(Date.class))).thenReturn(4);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION)), any(Date.class))).thenReturn(5);
        when(mockJdbcTemplate.update(getExpectedQuery(SQL_DELETE_BATCH_JOB_INSTANCE))).thenReturn(6);

        long startTime = System.currentTimeMillis();
        RepeatStatus status = tasklet.execute(mockStepContribution, mockChunkContext);
        long endTime = System.currentTimeMillis();

        assertEquals(RepeatStatus.FINISHED, status);

        verify(mockJdbcTemplate, times(5)).update(sqlArgumentCaptor.capture(), dateArgumentCaptor.capture());
        verify(mockJdbcTemplate, times(1)).update(sqlArgumentCaptor.capture());

        List<String> capturedSqls = sqlArgumentCaptor.getAllValues();
        assertEquals(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT), capturedSqls.get(0));
        assertEquals(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION), capturedSqls.get(1));
        assertEquals(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT), capturedSqls.get(2));
        assertEquals(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS), capturedSqls.get(3));
        assertEquals(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION), capturedSqls.get(4));
        assertEquals(getExpectedQuery(SQL_DELETE_BATCH_JOB_INSTANCE), capturedSqls.get(5));

        List<Date> capturedDates = dateArgumentCaptor.getAllValues();
        assertEquals(5, capturedDates.size());
        Date firstDate = capturedDates.getFirst();
        for (Date actualDate : capturedDates) {
            assertNotNull(actualDate);
            assertEquals(firstDate.getTime(), actualDate.getTime());
            long timeOfActualDatePlusRetention = actualDate.getTime() + historicRetentionMilliseconds;
            assertTrue(timeOfActualDatePlusRetention >= startTime && timeOfActualDatePlusRetention <= endTime + 1,
                "Cutoff date calculation is incorrect.");
        }

        verify(mockStepContribution).incrementWriteCount(1 + 2 + 3 + 4 + 5 + 6);
    }

    @Test
    void executeWhenNoRowsAreDeleted() {
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT)), any(Date.class))).thenReturn(0);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION)), any(Date.class))).thenReturn(0);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT)), any(Date.class))).thenReturn(0);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS)), any(Date.class))).thenReturn(0);
        when(mockJdbcTemplate.update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION)), any(Date.class))).thenReturn(0);
        when(mockJdbcTemplate.update(getExpectedQuery(SQL_DELETE_BATCH_JOB_INSTANCE))).thenReturn(0);

        long startTime = System.currentTimeMillis();
        RepeatStatus status = tasklet.execute(mockStepContribution, mockChunkContext);
        long endTime = System.currentTimeMillis();

        assertEquals(RepeatStatus.FINISHED, status);

        verify(mockJdbcTemplate).update(eq(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION_CONTEXT)), dateArgumentCaptor.capture());
        verify(mockJdbcTemplate).update(eq(getExpectedQuery(SQL_DELETE_BATCH_STEP_EXECUTION)), dateArgumentCaptor.capture());
        verify(mockJdbcTemplate).update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_CONTEXT)), dateArgumentCaptor.capture());
        verify(mockJdbcTemplate).update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION_PARAMS)), dateArgumentCaptor.capture());
        verify(mockJdbcTemplate).update(eq(getExpectedQuery(SQL_DELETE_BATCH_JOB_EXECUTION)), dateArgumentCaptor.capture());
        verify(mockJdbcTemplate).update(getExpectedQuery(SQL_DELETE_BATCH_JOB_INSTANCE));

        verify(mockStepContribution).incrementWriteCount(0);

        List<Date> capturedDates = dateArgumentCaptor.getAllValues();
        assertEquals(5, capturedDates.size());
        Date firstDate = capturedDates.getFirst();
        for (Date actualDate : capturedDates) {
            assertNotNull(actualDate);
            assertEquals(firstDate.getTime(), actualDate.getTime());
            long timeOfActualDatePlusRetention = actualDate.getTime() + historicRetentionMilliseconds;
            assertTrue(timeOfActualDatePlusRetention >= startTime && timeOfActualDatePlusRetention <= endTime + 1,
                "Cutoff date calculation is incorrect when no rows are deleted.");
        }
    }

    @Test
    void getQueryReplacesPrefixCorrectly() {
        String baseQuery = "SELECT * FROM %PREFIX%MY_TABLE";
        String expectedQuery = "SELECT * FROM " + tablePrefix + "MY_TABLE";
        assertEquals(expectedQuery, tasklet.getQuery(baseQuery));
    }
}