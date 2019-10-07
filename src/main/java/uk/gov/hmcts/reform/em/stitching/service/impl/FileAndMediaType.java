package uk.gov.hmcts.reform.em.stitching.service.impl;

import okhttp3.MediaType;

import java.io.File;

public class FileAndMediaType {

    private final File file;
    private final MediaType mediaType;

    public FileAndMediaType(File file, MediaType mediaType) {
        this.file = file;
        this.mediaType = mediaType;
    }

    public File getFile() {
        return file;
    }

    public MediaType getMediaType() {
        return mediaType;
    }
}
