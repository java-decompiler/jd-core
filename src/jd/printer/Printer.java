package jd.printer;

import jd.instruction.bytecode.instruction.Instruction;


public interface Printer 
{
	public static int UNKNOWN_LINE_NUMBER = Instruction.UNKNOWN_LINE_NUMBER;
	
	public void close();
	public void flush();

	public void print(int lineNumber, byte b);
	public void print(int lineNumber, char c);
	public void print(int lineNumber, int i);
	public void print(int lineNumber, String s);
	
	public void printKeyword(int lineNumber, String s);	
	public void printJavaWord(int lineNumber, String s);
	public void printField(int lineNumber, String s);
	public void printStaticField(int lineNumber, String s);
	public void printStaticMethod(int lineNumber, String s);
	public void printNumeric(int lineNumber, String s);
	public void printString(int lineNumber, String s);
	public void printClass(int lineNumber, String name, String qualifiedName);

	public void endOfStatement();

	public void startComment();
	public void endComment();
	public void endOfLineComment();
	
	public void startJavadoc();
	public void endJavadoc();
	public void endOfLineJavadoc();
	
	public void startPackageStatement();
	public void endPackageStatement();

	public void startImportStatements();
	public void endImportStatements();
	public void startImportStatement();
	public void endImportStatement();

	public void startAnnotation();
	public void endAnnotation();
	public void startAnnotationName();
	public void endAnnotationName();

	public void startClassDeclaration(String qualifiedName);
	public void endClassDeclaration();
	public void startClassDeclarationExtends();
	public void endClassDeclarationExtends();
	public void startClassDeclarationImplements();
	public void endClassDeclarationImplements();

	public void startFieldDeclaration();	
	public void endFieldDeclaration();

	public void startMethodDeclaration();	
	public void endMethodDeclaration();
	public void startMethodDeclarationThrows();
	public void endMethodDeclarationThrows();

	public void startStatementBlock();	
	public void endStatementBlock();

	public void startSingleStatementBlock();	
	public void endSingleStatementBlock();

	public void startCaseStatement();	
	public void endCaseStatement();

	public void startEnumValueDeclaration();	
	public void endEnumValueDeclaration();
		
	public void startErrorBlock();
	public void endErrorBlock();
	
	public void printOffset(int lineNumber, int offset);
}
