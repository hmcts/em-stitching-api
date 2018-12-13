package uk.gov.hmcts.reform.em.bundling.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.bundling.Application;
import uk.gov.hmcts.reform.em.bundling.service.AnnotationSetFetcher;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class AnnotationSetFetcherImplTest {

    @Autowired
    private AnnotationSetFetcher annotationSetFetcher;

    @Test(expected = DocumentTaskProcessingException.class)
    public void fetchAnnotationSet() throws Exception {
        annotationSetFetcher.fetchAnnotationSet("whatever", "whatever");
    }

}