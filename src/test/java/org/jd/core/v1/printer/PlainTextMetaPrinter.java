/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.printer;

public class PlainTextMetaPrinter extends PlainTextPrinter {
    // --- Printer --- //
    public void printStringConstant(String constant, String ownerInternalName) {
        sb.append(constant);
        sb.append("<META-STRING ownerInternalName='");
        sb.append(ownerInternalName);
        sb.append("'/>");
    }

    public void printDeclaration(int flags, String internalTypeName, String name, String descriptor) {
        sb.append(name);
        sb.append("<META-DECLARATION flags='");
        printFlags(flags);
        sb.append("' internalName='");
        sb.append(internalTypeName);
        sb.append("' descriptor='");
        sb.append(descriptor);
        sb.append("'/>");
    }

    public void printReference(int flags, String internalTypeName, String name, String descriptor, String ownerInternalName) {
        sb.append(name);
        sb.append("<META-REFERENCE flags='");
        printFlags(flags);
        sb.append("' internalName='");
        sb.append(internalTypeName==null ? "?" : internalTypeName);
        sb.append("' descriptor='");
        sb.append(descriptor);
        sb.append("' ownerInternalName='");
        sb.append(ownerInternalName);
        sb.append("'/>");
    }

    public void startLine(int lineNumber) {
        printLineNumber(lineNumber);

        for (int i=0; i<indentationCount; i++)
            sb.append(TAB);
    }

    public void extraLine(int count) {
        sb.append("<EXTRALINE>");
        while (count-- > 0) {
            printLineNumber(0);
            sb.append(NEWLINE);
        }
        sb.append("</EXTRALINE>");
    }

    public void startMarker(int type) {
        sb.append("<MARKER type='");
        printMarker(type);
        sb.append("'>");
    }

    public void endMarker(int type) {
        sb.append("</MARKER type='");
        printMarker(type);
        sb.append("'>");
    }

    protected void printFlags(int flags) {
        if ((flags & TYPE_FLAG) != 0) {
            sb.append("+TYPE");
        }
        if ((flags & FIELD_FLAG) != 0) {
            sb.append("+FIELD");
        }
        if ((flags & METHOD_FLAG) != 0) {
            sb.append("+METHOD");
        }
        if ((flags & CONSTRUCTOR_FLAG) != 0) {
            sb.append("+CONSTRUCTOR");
        }
    }

    protected void printMarker(int marker) {
        switch (marker) {
            case COMMENT_TYPE:
                sb.append("COMMENT");
                break;
            case JAVADOC_TYPE:
                sb.append("JAVADOC");
                break;
            case ERROR_TYPE:
                sb.append("ERROR");
                break;
            case IMPORT_STATEMENTS_TYPE:
                sb.append("IMPORT_STATEMENTS");
                break;
        }
    }
}
