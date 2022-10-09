package org.jd.core.test;

import org.apache.commons.imaging.FormatCompliance;
import org.apache.commons.imaging.common.bytesource.ByteSource;

import java.io.IOException;
import java.io.InputStream;

public abstract class TryResourcesImaging {

    FormatCompliance getFormatCompliance(ByteSource byteSource) throws IOException {
        FormatCompliance result = new FormatCompliance(byteSource.getDescription());
        try (InputStream is = byteSource.getInputStream()) {
            readImageContents(is, result);
        }
        return result;
    }

    abstract void readImageContents(InputStream is, FormatCompliance result) throws IOException;
}