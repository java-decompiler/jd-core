/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.printer;

import java.util.Optional;

public class PlainTextMetaPrinter extends PlainTextPrinter {
    // --- Printer --- //
    @Override
    public void printStringConstant(String constant, String ownerInternalName) {
        sb.append(constant);
        sb.append("<META-STRING ownerInternalName='");
        sb.append(ownerInternalName);
        sb.append("'/>");
    }

    @Override
    public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
        sb.append(name);
        sb.append("<META-DECLARATION type='");
        printType(type);
        sb.append("' internalName='");
        sb.append(internalTypeName);
        sb.append("' descriptor='");
        sb.append(descriptor);
        sb.append("'/>");
    }

    @Override
    public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
        sb.append(name);
        sb.append("<META-REFERENCE type='");
        printType(type);
        sb.append("' internalName='");
        sb.append(Optional.ofNullable(internalTypeName).orElse("?"));
        sb.append("' descriptor='");
        sb.append(descriptor);
        sb.append("' ownerInternalName='");
        sb.append(ownerInternalName);
        sb.append("'/>");
    }

    @Override
    public void startLine(int lineNumber) {
        printLineNumber(lineNumber);

        for (int i=0; i<indentationCount; i++) {
            sb.append(TAB);
        }
    }

    @Override
    public void extraLine(int count) {
        sb.append("<EXTRALINE>");
        while (count-- > 0) {
            printLineNumber(0);
            sb.append(NEWLINE);
        }
        sb.append("</EXTRALINE>");
    }

    @Override
    public void startMarker(int type) {
        sb.append("<MARKER type='");
        printMarker(type);
        sb.append("'>");
    }

    @Override
    public void endMarker(int type) {
        sb.append("</MARKER type='");
        printMarker(type);
        sb.append("'>");
    }

    protected void printType(int type) {
        switch (type) {
            case TYPE:
                sb.append("TYPE");
                break;
            case FIELD:
                sb.append("FIELD");
                break;
            case METHOD:
                sb.append("METHOD");
                break;
            case CONSTRUCTOR:
                sb.append("CONSTRUCTOR");
                break;
            case PACKAGE:
                sb.append("PACKAGE");
                break;
            case MODULE:
                sb.append("MODULE");
                break;
        }
    }

    protected void printMarker(int type) {
        switch (type) {
            case COMMENT:
                sb.append("COMMENT");
                break;
            case JAVADOC:
                sb.append("JAVADOC");
                break;
            case ERROR:
                sb.append("ERROR");
                break;
            case IMPORT_STATEMENTS:
                sb.append("IMPORT_STATEMENTS");
                break;
        }
    }
}
