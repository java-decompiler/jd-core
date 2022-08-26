package org.jd.core.v1;

import java.io.FileFilter;

public class FileAppender extends AbstractFileAppender {
    public static class Builder<B extends Builder<B>> extends AbstractFileAppender.Builder<B> {
        String fileName;

        public B withFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        public FileAppender build() {
            return new FileAppender();
        }
    }
    
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static <B extends Builder<B>> FileAppender createAppender(
            final String fileName,
            final FileFilter fileFilter,
            final int bufferSize) {

        return FileAppender.newBuilder()
                .setBufferSize(bufferSize)
                .withFileName(fileName)
                .setFilter(fileFilter)
                .build();
    }
}