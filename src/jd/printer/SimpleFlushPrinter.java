package jd.printer;

import java.io.PrintWriter;

import jd.Constants;



public class SimpleFlushPrinter implements Printer
{
	private final PrintWriter printWriter;
	private String prefix;
	private boolean newLine;
	private int lastLineNumber;
	
	public SimpleFlushPrinter(PrintWriter printWriter)
	{
		this.printWriter = printWriter;
		this.prefix = "";
		this.newLine = true;
		this.lastLineNumber = UNKNOWN_LINE_NUMBER;
	}

	public void close() 
	{
		printWriter.close();
	}

	public void flush() 
	{
		printWriter.flush();
	}

	public void print(int lineNumber, byte b) 
	{
		prePrint(lineNumber);
		printWriter.print(b);
		printWriter.flush();
		this.newLine = false;
	}

	public void print(int lineNumber, char c) 
	{
		prePrint(lineNumber);
		printWriter.print(c);
		printWriter.flush();
		this.newLine = false;
	}

	public void print(int lineNumber, int i) 
	{
		prePrint(lineNumber);
		printWriter.print(i);
		printWriter.flush();
		this.newLine = false;
	}

	public void print(int lineNumber, String s) 
	{
		prePrint(lineNumber);
		printWriter.print(s);
		printWriter.flush();
		this.newLine = false;
	}
	
	private void prePrint(int lineNumber)
	{
		//////////////printWriter.print("[[" + lineNumber + "]]");
		
		// Affichage des sauts de ligne supplementaires et des lignes coupées
		if ((lineNumber != UNKNOWN_LINE_NUMBER) && 
			(this.lastLineNumber != UNKNOWN_LINE_NUMBER) &&
			(this.lastLineNumber < lineNumber))
		{
			if (this.newLine)
			{
				if ((lineNumber - lastLineNumber) > 1)
				{
					// Sauts de ligne supplementaires
					printWriter.println();
				}
				printWriter.print(this.prefix);
			}
			else
			{
				// Lignes coupées
				printWriter.println();
				printWriter.print(this.prefix);
				printWriter.print(Constants.INDENT);
			}
		}
		else if (this.newLine)
		{
			// Saut de ligne normal
			printWriter.print(this.prefix);
		}

		this.lastLineNumber = lineNumber;		
	}
	
	public void printKeyword(int lineNumber, String s) 
	{ print(lineNumber, s); }
	public void printJavaWord(int lineNumber, String s) 
	{ print(lineNumber, s); }
	public void printField(int lineNumber, String s) 
	{ print(lineNumber, s); }
	public void printStaticField(int lineNumber, String s) 
	{ print(lineNumber, s); }
	public void printStaticMethod(int lineNumber, String s) 
	{ print(lineNumber, s); }
	public void printNumeric(int lineNumber, String s) 
	{ print(lineNumber, s); }
	
	public void printString(int lineNumber, String s) 
	{				
		print(lineNumber, s); 
	}
	
	public void printClass(int lineNumber, String name, String qualifiedName) 
	{ print(lineNumber, name); }
	
	public void endOfStatement() { endOfLine(); }
	
	public void startAnnotation() {}
	public void endAnnotation() { endOfLine(); }

	public void startAnnotationName() {}
	public void endAnnotationName() {}

	public void startComment() {}
	public void endComment() { endOfLine(); }
	public void endOfLineComment() { endOfLine(); }

	public void startJavadoc() {}
	public void endJavadoc() { endOfLine(); }
	public void endOfLineJavadoc() { endOfLine(); }
	
	public void startPackageStatement() {}
	public void endPackageStatement() { endOfLine(); }

	public void startImportStatements() {}
	public void endImportStatements() { endOfLine(); }
	public void startImportStatement() {}
	public void endImportStatement() { endOfLine(); }	

	public void startClassDeclaration(String qualifiedName)
	{
		endOfLine();
	}
	public void endClassDeclaration() {}
	public void startClassDeclarationExtends() 
	{
		printWriter.print(" ");	
	}
	public void endClassDeclarationExtends() {}
	public void startClassDeclarationImplements() {}
	public void endClassDeclarationImplements() {}

	public void startFieldDeclaration() {}	
	public void endFieldDeclaration()
	{
		endOfLine();
	}

	public void startMethodDeclaration()
	{
		endOfLine();
	}
	public void endMethodDeclaration() {}
	public void startMethodDeclarationThrows()
	{
		endOfLine();
	}
	public void endMethodDeclarationThrows() {}

	public void startStatementBlock()
	{
		printWriter.println();
		printWriter.print(this.prefix);
		printWriter.println('{');
		this.prefix += Constants.INDENT;
		this.newLine = true;
	}
	
	public void endStatementBlock()
	{
		if (this.prefix.length() >= Constants.INDENT.length())
			this.prefix = this.prefix.substring(Constants.INDENT.length());
		printWriter.print(this.prefix);
		printWriter.println('}');
	}
	
	public void startSingleStatementBlock()
	{
		printWriter.println();
		this.prefix += Constants.INDENT;
		this.newLine = true;		
	}
	
	public void endSingleStatementBlock()
	{
		if (this.prefix.length() >= Constants.INDENT.length())
			this.prefix = this.prefix.substring(Constants.INDENT.length());		
	}

	public void startCaseStatement()
	{
		printWriter.println();
		this.prefix += Constants.INDENT;
		this.newLine = true;
	}
	
	public void endCaseStatement()
	{
		if (this.prefix.length() >= Constants.INDENT.length())
			this.prefix = this.prefix.substring(Constants.INDENT.length());
	}

	public void startEnumValueDeclaration() {}
	public void endEnumValueDeclaration() 
	{
		printWriter.println(';');		
	}
		
	public void startErrorBlock() {}
	public void endErrorBlock() {}

	public void printOffset(int lineNumber, int offset)
	{
		printWriter.print("//  ");
		printWriter.print(offset);
		printWriter.print(": ");		
		printWriter.flush();
	}

	private void endOfLine() 
	{
		printWriter.println();
		printWriter.flush();
		this.newLine = true;
	}
}
