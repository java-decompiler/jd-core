/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.api.printer;


public interface Printer {
    int UNKNOWN_LINE_NUMBER = 0;

    // Declaration & reference flags
    int TYPE_FLAG = 1;
    int FIELD_FLAG = 2;
    int METHOD_FLAG = 4;
    int CONSTRUCTOR_FLAG = 8;

    // Marker types
    int COMMENT_TYPE = 1;
    int JAVADOC_TYPE = 2;
    int ERROR_TYPE = 3;
    int IMPORT_STATEMENTS_TYPE = 4;

    void start(int maxLineNumber, int majorVersion, int minorVersion);
    void end();

    void printText(String text);
    void printNumericConstant(String constant);
    void printStringConstant(String constant, String ownerInternalName);
    void printKeyword(String keyword);

    void printDeclaration(int flags, String internalTypeName, String name, String descriptor);
    void printReference(int flags, String internalTypeName, String name, String descriptor, String ownerInternalName);

    void indent();
    void unindent();

    void startLine(int lineNumber);
    void endLine();
    void extraLine(int count);

    void startMarker(int type);
    void endMarker(int type);
}
