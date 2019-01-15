package uk.gov.hmcts.reform.em.stitching.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.stitching.Application;
import uk.gov.hmcts.reform.em.stitching.service.DmStoreDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@Transactional
public class DmStoreDownloaderImplTest {

    @Autowired
    DmStoreDownloader dmStoreDownloader;

    @Test(expected = RuntimeException.class)
    public void downloadFile() throws Exception {
        Stream<File> results = dmStoreDownloader.downloadFiles(Arrays.asList("aaa", "bbb"));

        results.collect(Collectors.toList());
    }
}