/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package jd.core.printer;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.util.StringConstants;

import java.io.IOException;

import static org.apache.bcel.Const.MAJOR_1_1;
import static org.apache.bcel.Const.MAJOR_1_5;

import jd.core.CoreConstants;
import jd.core.Decompiler;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.preferences.Preferences;

public class PlainTextPrinter implements Printer {
    protected static final String TAB = "  ";
    protected static final String NEWLINE = System.lineSeparator();

    private Preferences preferences;
    private StringBuilder sb = new StringBuilder();
    private int maxLineNumber;
    private int majorVersion;
    private int minorVersion;
    private int digitCount;

    private String lineNumberBeginPrefix;
    private String lineNumberEndPrefix;
    private String unknownLineNumberPrefix;
    private int indentationCount;
    private boolean display;

    public void setPreferences(Preferences preferences) { this.preferences = preferences; }

    public int getMajorVersion() { return majorVersion; }
    public int getMinorVersion() { return minorVersion; }

    @Override
    public void print(byte b) { this.sb.append(String.valueOf(b)); }
    @Override
    public void print(int i) { this.sb.append(String.valueOf(i)); }

    @Override
    public void print(char c) {
        if (this.display) {
            this.sb.append(String.valueOf(c));
        }
    }

    @Override
    public void print(String s) {
        if (this.display) {
            printEscape(s);
        }
    }

    @Override
    public void printNumeric(String s) { this.sb.append(s); }

    @Override
    public void printString(String s, String scopeInternalName)  { this.sb.append(s); }

    @Override
    public void printKeyword(String s) {
        if (this.display) {
            this.sb.append(s);
        }
    }

    @Override
    public void printJavaWord(String s) { this.sb.append(s); }

    @Override
    public void printType(String internalName, String name, String scopeInternalName) {
        if (this.display) {
            printEscape(name);
        }
    }

    @Override
    public void printTypeDeclaration(String internalName, String name)
    {
        printEscape(name);
    }

    @Override
    public void printTypeImport(String internalName, String name)
    {
        printEscape(name);
    }

    @Override
    public void printField(String internalName, String name, String descriptor, String scopeInternalName) {
        printEscape(name);
    }
    @Override
    public void printFieldDeclaration(String internalName, String name, String descriptor) {
        printEscape(name);
    }

    @Override
    public void printStaticField(String internalName, String name, String descriptor, String scopeInternalName) {
        printEscape(name);
    }
    @Override
    public void printStaticFieldDeclaration(String internalName, String name, String descriptor) {
        printEscape(name);
    }

    @Override
    public void printConstructor(String internalName, String name, String descriptor, String scopeInternalName) {
        printEscape(name);
    }
    @Override
    public void printConstructorDeclaration(String internalName, String name, String descriptor) {
        printEscape(name);
    }

    @Override
    public void printStaticConstructorDeclaration(String internalName, String name) {
        this.sb.append(name);
    }

    @Override
    public void printMethod(String internalName, String name, String descriptor, String scopeInternalName) {
        printEscape(name);
    }
    @Override
    public void printMethodDeclaration(String internalName, String name, String descriptor) {
        printEscape(name);
    }

    @Override
    public void printStaticMethod(String internalName, String name, String descriptor, String scopeInternalName) {
        printEscape(name);
    }
    @Override
    public void printStaticMethodDeclaration(String internalName, String name, String descriptor) {
        printEscape(name);
    }

    @Override
    public void start(int maxLineNumber, int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.indentationCount = 0;
        this.display = true;

        if (this.preferences.isShowLineNumbers()) {
            this.maxLineNumber = maxLineNumber;

            if (maxLineNumber > 0) {
                this.digitCount = 1;
                for (int maximum = 9; maximum < maxLineNumber; maximum = maximum*10 + 9) {
                    this.digitCount++;
                }
                this.unknownLineNumberPrefix = " ".repeat(digitCount);
                this.lineNumberBeginPrefix = "/* ";
                this.lineNumberEndPrefix = " */ ";
            } else {
                this.unknownLineNumberPrefix = "";
                this.lineNumberBeginPrefix = "";
                this.lineNumberEndPrefix = "";
            }
        } else {
            this.maxLineNumber = 0;
            this.unknownLineNumberPrefix = "";
            this.lineNumberBeginPrefix = "";
            this.lineNumberEndPrefix = "";
        }
    }

    @Override
    public void end() {}

    @Override
    public void indent() {
        this.indentationCount++;
    }
    @Override
    public void desindent() {
        if (this.indentationCount > 0) {
            this.indentationCount--;
        }
    }

    @Override
    public void startOfLine(int lineNumber) {
        if (this.maxLineNumber > 0)
        {
            this.sb.append(this.lineNumberBeginPrefix);

            if (lineNumber == Instruction.UNKNOWN_LINE_NUMBER) {
                this.sb.append(this.unknownLineNumberPrefix);
            } else {
                int left = 0;

                left = printDigit(5, lineNumber, 10000, left);
                left = printDigit(4, lineNumber,  1000, left);
                left = printDigit(3, lineNumber,   100, left);
                left = printDigit(2, lineNumber,    10, left);
                this.sb.append((char)('0' + (lineNumber-left)));
            }

            this.sb.append(this.lineNumberEndPrefix);
        }

        for (int i=0; i<indentationCount; i++) {
            this.sb.append(TAB);
        }
    }

    @Override
    public void endOfLine()
    {
        this.sb.append(NEWLINE);
    }

    @Override
    public void extraLine(int count) {
        if (this.preferences.getRealignmentLineNumber()) {
            while (count-- > 0) {
                if (this.maxLineNumber > 0) {
                    this.sb.append(this.lineNumberBeginPrefix);
                    this.sb.append(this.unknownLineNumberPrefix);
                    this.sb.append(this.lineNumberEndPrefix);
                }

                this.sb.append(NEWLINE);
            }
        }
    }

    @Override
    public void startOfComment() {}
    @Override
    public void endOfComment() {}

    @Override
    public void startOfJavadoc() {}
    @Override
    public void endOfJavadoc() {}

    @Override
    public void startOfXdoclet() {}
    @Override
    public void endOfXdoclet() {}

    @Override
    public void startOfError() {}
    @Override
    public void endOfError() {}

    @Override
    public void startOfImportStatements() {}
    @Override
    public void endOfImportStatements() {}

    @Override
    public void startOfTypeDeclaration(String internalPath) {}
    @Override
    public void endOfTypeDeclaration() {}

    @Override
    public void startOfAnnotationName() {}
    @Override
    public void endOfAnnotationName() {}

    @Override
    public void startOfOptionalPrefix() {
        if (!this.preferences.isShowPrefixThis()) {
            this.display = false;
        }
    }

    @Override
    public void endOfOptionalPrefix()
    {
        this.display = true;
    }

    @Override
    public void debugStartOfLayoutBlock() {}
    @Override
    public void debugEndOfLayoutBlock() {}

    @Override
    public void debugStartOfSeparatorLayoutBlock() {}
    @Override
    public void debugEndOfSeparatorLayoutBlock(int min, int value, int max) {}

    @Override
    public void debugStartOfStatementsBlockLayoutBlock() {}
    @Override
    public void debugEndOfStatementsBlockLayoutBlock(int min, int value, int max) {}

    @Override
    public void debugStartOfInstructionBlockLayoutBlock() {}
    @Override
    public void debugEndOfInstructionBlockLayoutBlock() {}

    @Override
    public void debugStartOfCommentDeprecatedLayoutBlock() {}
    @Override
    public void debugEndOfCommentDeprecatedLayoutBlock() {}

    @Override
    public void debugMarker(String marker) {}

    @Override
    public void debugStartOfCaseBlockLayoutBlock() {}
    @Override
    public void debugEndOfCaseBlockLayoutBlock() {}

    protected void printEscape(String s) {
        if (this.preferences.isUnicodeEscape()) {
            int length = s.length();

            char c;
            for (int i=0; i<length; i++) {
                c = s.charAt(i);

                if (c == '\t') {
                    this.sb.append(c);
                } else if (c < 32) {
                    // Write octal format
                    this.sb.append("\\0");
                    this.sb.append((char)('0' + (c >> 3)));
                    this.sb.append((char)('0' + (c & 0x7)));
                } else if (c > 127) {
                    // Write octal format
                    this.sb.append("\\u");

                    int z = c >> 12;
                    this.sb.append((char)(z <= 9 ? '0' + z : 'A' - 10 + z));
                    z = c >> 8 & 0xF;
                    this.sb.append((char)(z <= 9 ? '0' + z : 'A' - 10 + z));
                    z = c >> 4 & 0xF;
                    this.sb.append((char)(z <= 9 ? '0' + z : 'A' - 10 + z));
                    z = c & 0xF;
                    this.sb.append((char)(z <= 9 ? '0' + z : 'A' - 10 + z));
                } else {
                    this.sb.append(c);
                }
            }
        } else {
            this.sb.append(s);
        }
    }

    protected int printDigit(int dcv, int lineNumber, int divisor, int left) {
       if (this.digitCount >= dcv) {
           if (lineNumber < divisor) {
               this.sb.append(' ');
           } else {
               int e = (lineNumber-left) / divisor;
               this.sb.append((char)('0' + e));
               left += e*divisor;
           }
       }

       return left;
    }

    public void println() {
        this.sb.append(NEWLINE);
    }

    public void reset() {
        this.sb.setLength(0);
    }

    @Override
    public String toString() {
        return this.sb.toString();
    }

    public String buildDecompiledOutput(Loader loader, String internalName, Preferences preferences, Decompiler decompiler) throws IOException {
        setPreferences(preferences);

        // Decompile class file
        String internalClassPath = internalName + StringConstants.CLASS_FILE_SUFFIX;
        decompiler.decompile(preferences, loader, this, internalClassPath);

        // Metadata
        if (preferences.isWriteMetaData()) {
            print("\n\n/*");

            // Add Java compiler version
            if (majorVersion >= MAJOR_1_1) {
                println();
                print(" * Java compiler version: ");

                if (majorVersion >= MAJOR_1_5) {
                    print(majorVersion - (MAJOR_1_5 - 5));
                } else {
                    print(majorVersion - (MAJOR_1_1 - 1));
                }

                print(" (");
                print(majorVersion);
                print('.');
                print(getMinorVersion());
                print(')');
            }
            // Add JD-Core version
            println();
            print(" * JD-Core Version:       ");
            print(CoreConstants.JD_CORE_VERSION);
            println();
            print(" */");
        }
        return toString();
    }
}
