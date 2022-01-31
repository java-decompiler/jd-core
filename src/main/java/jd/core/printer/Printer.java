/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.printer;

public interface Printer
{
    int UNKNOWN_LINE_NUMBER = 0;

    void print(byte b);
    void print(char c);
    void print(int i);
    void print(String s);

    void printNumeric(String s);
    void printString(String s, String scopeInternalName);

    void printKeyword(String keyword);
    void printJavaWord(String s);

    void printType(String internalName, String name, String scopeInternalName);
    void printTypeDeclaration(String internalName, String name);
    void printTypeImport(String internalName, String name);

    void printField(
        String internalName, String name,
        String descriptor, String scopeInternalName);
    void printFieldDeclaration(
        String internalName, String name, String descriptor);

    void printStaticField(
        String internalName, String name,
        String descriptor, String scopeInternalName);
    void printStaticFieldDeclaration(
        String internalName, String name, String descriptor);

    void printConstructor(
        String internalName, String name,
        String descriptor, String scopeInternalName);
    void printConstructorDeclaration(
        String internalName, String name, String descriptor);

    void printStaticConstructorDeclaration(
        String internalName, String name);

    void printMethod(
        String internalName, String name,
        String descriptor, String scopeInternalName);
    void printMethodDeclaration(
        String internalName, String name, String descriptor);

    void printStaticMethod(
        String internalName, String name,
        String descriptor, String scopeInternalName);
    void printStaticMethodDeclaration(
        String internalName, String name, String descriptor);

    void start(int maxLineNumber, int majorVersion, int minorVersion);
    void end();

    void indent();
    void desindent();

    void startOfLine(int lineNumber);
    void endOfLine();
    void extraLine(int count);

    void startOfComment();
    void endOfComment();

    void startOfJavadoc();
    void endOfJavadoc();

    void startOfXdoclet();
    void endOfXdoclet();

    void startOfError();
    void endOfError();

    void startOfImportStatements();
    void endOfImportStatements();

    void startOfTypeDeclaration(String internalPath);
    void endOfTypeDeclaration();

    void startOfAnnotationName();
    void endOfAnnotationName();

    void startOfOptionalPrefix();
    void endOfOptionalPrefix();

    void debugStartOfLayoutBlock();
    void debugEndOfLayoutBlock();
    void debugStartOfSeparatorLayoutBlock();
    void debugEndOfSeparatorLayoutBlock(int min, int value, int max);
    void debugStartOfStatementsBlockLayoutBlock();
    void debugEndOfStatementsBlockLayoutBlock(int min, int value, int max);
    void debugStartOfInstructionBlockLayoutBlock();
    void debugEndOfInstructionBlockLayoutBlock();
    void debugStartOfCommentDeprecatedLayoutBlock();
    void debugEndOfCommentDeprecatedLayoutBlock();
    void debugMarker(String marker);
    void debugStartOfCaseBlockLayoutBlock();
    void debugEndOfCaseBlockLayoutBlock();
}
