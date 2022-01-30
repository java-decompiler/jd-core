/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.cfg;

import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

/**
 * A state diagram writer.
 *
 * http://plantuml.com/state.html
 * http://plantuml.com/plantuml
 */
public class ControlFlowGraphPlantURLWriter {
    protected static final int MAX_OFFSET = Integer.MAX_VALUE;

    //protected static final String PLANTUML_URL_PREFIX   = "http://plantuml.com/plantuml/png/";
    protected static final String PLANTUML_URL_PREFIX   = "http://plantuml.com/plantuml/svg/";
    protected static final char[] PLANTUML_ENCODE_6_BIT = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_".toCharArray();

    public static String writePlantUMLUrl(String plantuml) throws Exception {
        byte[] input = plantuml.getBytes(StandardCharsets.UTF_8);

        // Compress
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);

        deflater.setInput(input);
        deflater.finish();

        byte[] output = new byte[input.length * 2];
        int compressedDataLength = deflater.deflate(output);

        if (deflater.finished()) {
            // Encode
            final StringBuilder sb = new StringBuilder((output.length*4 + 2) / 3);

            for (int i=0; i<compressedDataLength; i+=3) {
                append3bytes(
                    sb,
                    output[i] & 0xFF,
                    i+1 < output.length ? output[i+1] & 0xFF : 0,
                    i+2 < output.length ? output[i+2] & 0xFF : 0);
            }

            return PLANTUML_URL_PREFIX + sb.toString();
        }
        return null;
    }

    protected static void append3bytes(StringBuilder sb, int b1, int b2, int b3) {
        int c1 = b1 >> 2;
        int c2 = ((b1 & 0x3) << 4) | (b2 >> 4);
        int c3 = ((b2 & 0xF) << 2) | (b3 >> 6);
        int c4 = b3 & 0x3F;

        sb.append(PLANTUML_ENCODE_6_BIT[c1 & 0x3F]);
        sb.append(PLANTUML_ENCODE_6_BIT[c2 & 0x3F]);
        sb.append(PLANTUML_ENCODE_6_BIT[c3 & 0x3F]);
        sb.append(PLANTUML_ENCODE_6_BIT[c4 & 0x3F]);
    }
}
