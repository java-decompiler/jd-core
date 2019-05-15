/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.util;


public class CharacterUtil {

    public static String escapeChar(int c) {
        switch (c) {
            case '\\':
                return "\\\\";
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            case '\'':
                return "\\'";
            default:
                if (c < ' ') {
                    return "\\0" + ((char)('0' + (c >> 3))) + ((char)('0' + (c & 7)));
                } else {
                    return String.valueOf((char)c);
                }
        }
    }
}
