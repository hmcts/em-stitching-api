package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DmStoreDownloaderImplTest {

    @Autowired
    DmStoreDownloader dmStoreDownloader;

    @Test(expected = DocumentTaskProcessingException.class)
    public void downloadFile() throws Exception {
        dmStoreDownloader.downloadFiles(Arrays.asList("xxx"));
    }
}