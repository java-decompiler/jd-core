/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.printer;

import org.jd.core.v1.api.Decompiler;
import org.jd.core.v1.api.loader.Loader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static jd.core.preferences.Preferences.ESCAPE_UNICODE_CHARACTERS;
import static jd.core.preferences.Preferences.REALIGN_LINE_NUMBERS;
import static jd.core.preferences.Preferences.WRITE_LINE_NUMBERS;
import static jd.core.preferences.Preferences.WRITE_METADATA;
import static org.apache.bcel.Const.MAJOR_1_1;
import static org.apache.bcel.Const.MAJOR_1_5;

import jd.core.ClassUtil;

public class LineNumberStringBuilderPrinter extends StringBuilderPrinter {

    private boolean showLineNumbers;

    private int maxLineNumber;
    private int digitCount;

    private String lineNumberBeginPrefix;
    private String lineNumberEndPrefix;
    private String unknownLineNumberPrefix;

    public void setShowLineNumbers(boolean showLineNumbers) { this.showLineNumbers = showLineNumbers; }

    protected int printDigit(int dcv, int lineNumber, int divisor, int left) {
        if (digitCount >= dcv) {
            if (lineNumber < divisor) {
                stringBuffer.append(' ');
            } else {
                int e = (lineNumber-left) / divisor;
                stringBuffer.append((char)('0' + e));
                left += e*divisor;
            }
        }

        return left;
    }

    /** --- Printer --- */
    @Override
    public void start(int maxLineNumber, int majorVersion, int minorVersion) {
        super.start(maxLineNumber, majorVersion, minorVersion);

        if (showLineNumbers) {
            this.maxLineNumber = maxLineNumber;

            if (maxLineNumber > 0) {
                digitCount = 1;
                unknownLineNumberPrefix = " ";
                for (int maximum = 9; maximum < maxLineNumber; maximum = maximum*10 + 9) {
                    digitCount++;
                }
                unknownLineNumberPrefix = " ".repeat(digitCount);
                lineNumberBeginPrefix = "/* ";
                lineNumberEndPrefix = " */ ";
            } else {
                unknownLineNumberPrefix = "";
                lineNumberBeginPrefix = "";
                lineNumberEndPrefix = "";
            }
        } else {
            this.maxLineNumber = 0;
            unknownLineNumberPrefix = "";
            lineNumberBeginPrefix = "";
            lineNumberEndPrefix = "";
        }
    }

    @Override
    public void startLine(int lineNumber) {
        if (maxLineNumber > 0) {
            stringBuffer.append(lineNumberBeginPrefix);

            if (lineNumber == UNKNOWN_LINE_NUMBER) {
                stringBuffer.append(unknownLineNumberPrefix);
            } else {
                int left = 0;

                left = printDigit(5, lineNumber, 10000, left);
                left = printDigit(4, lineNumber,  1000, left);
                left = printDigit(3, lineNumber,   100, left);
                left = printDigit(2, lineNumber,    10, left);
                stringBuffer.append((char)('0' + (lineNumber-left)));
            }

            stringBuffer.append(lineNumberEndPrefix);
        }

        for (int i=0; i<indentationCount; i++) {
            stringBuffer.append(TAB);
        }
    }
    @Override
    public void extraLine(int count) {
        if (realignmentLineNumber) {
            while (count-- > 0) {
                printLineNumber();

                stringBuffer.append(NEWLINE);
            }
        }
    }

    private void printLineNumber() {
        if (maxLineNumber > 0) {
            stringBuffer.append(lineNumberBeginPrefix);
            stringBuffer.append(unknownLineNumberPrefix);
            stringBuffer.append(lineNumberEndPrefix);
        }
    }

    public String buildDecompiledOutput(Map<String, String> preferences, Loader loader, String entryPath, Decompiler decompiler) throws IOException {
        // Init preferences
        boolean realignmentLineNumbers = Boolean.parseBoolean(preferences.getOrDefault(REALIGN_LINE_NUMBERS, Boolean.FALSE.toString()));

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("realignLineNumbers", realignmentLineNumbers);

        setRealignmentLineNumber(realignmentLineNumbers);
        setUnicodeEscape(Boolean.parseBoolean(preferences.getOrDefault(ESCAPE_UNICODE_CHARACTERS, Boolean.FALSE.toString())));
        setShowLineNumbers(Boolean.parseBoolean(preferences.getOrDefault(WRITE_LINE_NUMBERS, Boolean.TRUE.toString())));

        // Format internal name
        String entryInternalName = ClassUtil.getInternalName(entryPath);

        // Decompile class file
        decompiler.decompile(loader, this, entryInternalName, configuration);

        StringBuilder stringBuffer = getStringBuffer();

        // Metadata
        if (Boolean.parseBoolean(preferences.getOrDefault(WRITE_METADATA, Boolean.TRUE.toString()))) {
            stringBuffer.append("\n\n/*");
            // Add Java compiler version
            int majorVersion = getMajorVersion();

            if (majorVersion >= MAJOR_1_1) {
                stringBuffer.append("\n * Java compiler version: ");

                if (majorVersion >= MAJOR_1_5) {
                    stringBuffer.append(majorVersion - (MAJOR_1_5 - 5));
                } else {
                    stringBuffer.append("1.");
                    stringBuffer.append(majorVersion - (MAJOR_1_1 - 1));
                }

                stringBuffer.append(" (");
                stringBuffer.append(majorVersion);
                stringBuffer.append('.');
                stringBuffer.append(getMinorVersion());
                stringBuffer.append(')');
            }
            // Add JD-Core version
            stringBuffer.append("\n * JD-Core Version:       ");
            stringBuffer.append(getVersion());
            stringBuffer.append("\n */");
        }
        return stringBuffer.toString();
    }
}
