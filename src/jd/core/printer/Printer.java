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
	public static int UNKNOWN_LINE_NUMBER = 0;
	

	public void print(byte b);
	public void print(char c);
	public void print(int i);
	public void print(String s);
	
	public void printNumeric(String s);
	public void printString(String s, String scopeInternalName);

	public void printKeyword(String keyword);
	public void printJavaWord(String s);
	
	public void printType(String internalName, String name, String scopeInternalName);
	public void printTypeDeclaration(String internalName, String name); 
	public void printTypeImport(String internalName, String name);

	public void printField(
		String internalName, String name, 
		String descriptor, String scopeInternalName);
	public void printFieldDeclaration(
		String internalName, String name, String descriptor);
	
	public void printStaticField(
		String internalName, String name, 
		String descriptor, String scopeInternalName);
	public void printStaticFieldDeclaration(
		String internalName, String name, String descriptor);
	
	public void printConstructor(
		String internalName, String name, 
		String descriptor, String scopeInternalName);
	public void printConstructorDeclaration(
		String internalName, String name, String descriptor);
	
	public void printStaticConstructorDeclaration(
		String internalName, String name);
	
	public void printMethod(
		String internalName, String name, 
		String descriptor, String scopeInternalName);
	public void printMethodDeclaration(
		String internalName, String name, String descriptor);
	
	public void printStaticMethod(
		String internalName, String name, 
		String descriptor, String scopeInternalName);
	public void printStaticMethodDeclaration(
		String internalName, String name, String descriptor);
	
	public void start(int maxLineNumber, int majorVersion, int minorVersion);
	public void end();

	public void indent();
	public void desindent();
	
	public void startOfLine(int lineNumber);
	public void endOfLine();
	public void extraLine(int count);
	
	public void startOfComment();
	public void endOfComment();

	public void startOfJavadoc();
	public void endOfJavadoc();

	public void startOfXdoclet();
	public void endOfXdoclet();
	
	public void startOfError();
	public void endOfError();

	public void startOfImportStatements();
	public void endOfImportStatements();
	
	public void startOfTypeDeclaration(String internalPath);
	public void endOfTypeDeclaration();

	public void startOfAnnotationName();
	public void endOfAnnotationName();
	
	public void startOfOptionalPrefix();
	public void endOfOptionalPrefix();
	
	public void debugStartOfLayoutBlock();
	public void debugEndOfLayoutBlock();
	public void debugStartOfSeparatorLayoutBlock();
	public void debugEndOfSeparatorLayoutBlock(int min, int value, int max);
	public void debugStartOfStatementsBlockLayoutBlock();
	public void debugEndOfStatementsBlockLayoutBlock(int min, int value, int max);
	public void debugStartOfInstructionBlockLayoutBlock();
	public void debugEndOfInstructionBlockLayoutBlock();
	public void debugStartOfCommentDeprecatedLayoutBlock();
	public void debugEndOfCommentDeprecatedLayoutBlock();
	public void debugMarker(String marker);
	public void debugStartOfCaseBlockLayoutBlock();
	public void debugEndOfCaseBlockLayoutBlock();
}
