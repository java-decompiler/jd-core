package jd.core.printer;

import java.util.ArrayList;


public class InstructionPrinter implements Printer
{
	protected Printer printer;
	private int previousLineNumber;
	private boolean newInstruction;
	private boolean multiLineInstruction;
	private boolean active;
	private ArrayList<Boolean> states;

	/* 
	 * L'etat se reduit a  "multiLineInstruction" 
	 * --> Optimisation: utilisation de Boolean a la place de State.
	private static class State 
	{
		public boolean newInstruction;
		public boolean multiLineInstruction;
		
		public State(boolean newInstruction, boolean multiLineInstruction)
		{
			this.newInstruction = newInstruction;
			this.multiLineInstruction = multiLineInstruction;
		}
	} */
	
	// -------------------------------------------------------------------------
	public InstructionPrinter(Printer printer)
	{
		this.printer = printer;
		this.active = false;
		this.states = new ArrayList<Boolean>(0);
	}
	
	public void init(int previousLineNumber)
	{
		this.previousLineNumber = previousLineNumber;
		this.newInstruction = false;
		this.multiLineInstruction = false;	
		this.active = false;
	}
	
	public void startOfInstruction()
	{		
		this.active = true;
	}

	public void addNewLinesAndPrefix(int lineNumber)
	{
		if (this.active == false)
		{
			// Instruction non commencée, e cours d'affichage. Restoration de 
			// l'état precedant.
			this.multiLineInstruction = this.states.remove(this.states.size()-1).booleanValue();
			/* State state = this.states.remove(this.states.size()-1);
			this.newInstruction = state.newInstruction;
			this.multiLineInstruction = state.multiLineInstruction; */
			this.active = true;
		}
		
		if (lineNumber == UNKNOWN_LINE_NUMBER)
		{
			if (this.newInstruction)
			{
				if (this.previousLineNumber == UNKNOWN_LINE_NUMBER)
				{
					this.printer.endOfLine();
					this.printer.startOfLine(lineNumber);					
				}
				else
				{
					this.printer.print(' ');					
				}
			}
		}
		else
		{
			if (this.previousLineNumber == UNKNOWN_LINE_NUMBER)
			{
				this.previousLineNumber = lineNumber;
			}
			else
			{
				if (this.previousLineNumber < lineNumber)
				{
					int lineCount = lineNumber - this.previousLineNumber;
					
					this.printer.endOfLine();
		
					if (lineCount > 1)
					{
						this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
						this.printer.endOfLine();
		
						if (lineCount > 2)
						{
							this.printer.extraLine(lineCount-2);
						}
					}
				
					if ((this.newInstruction == false) && 
						(this.multiLineInstruction == false))
					{
						this.printer.indent();
						this.multiLineInstruction = true;
					}
					
					this.printer.startOfLine(lineNumber);
					
					this.previousLineNumber = lineNumber;
				}
			}
		}
		
		this.newInstruction = false;
	}

	public void endOfInstruction()
	{	
		if (this.multiLineInstruction)
		{
			this.printer.desindent();
		}
		
		this.newInstruction = true;
		this.multiLineInstruction = false;
		this.active = false;
	}
	
	public void release()
	{
		if (this.active == true)
		{
			// Instruction non terminée. Sauvegarde de l'état courant.
			this.states.add(Boolean.valueOf(this.multiLineInstruction));
			/* this.states.add(
				new State(this.newInstruction, this.multiLineInstruction)); */
		}
	}
	
	// -------------------------------------------------------------------------
	public void print(int lineNumber, byte b)
	{
		addNewLinesAndPrefix(lineNumber);
		this.printer.print(b);
	}
	
	public void print(int lineNumber, char c)
	{
		addNewLinesAndPrefix(lineNumber);
		this.printer.print(c);		
	}
	
	public void print(int lineNumber, int i)
	{
		addNewLinesAndPrefix(lineNumber);
		this.printer.print(i);	
	}
	
	public void print(int lineNumber, String s)
	{
		addNewLinesAndPrefix(lineNumber);
		this.printer.print(s);
	}

	public void printNumeric(int lineNumber, String s) 
	{ 
		addNewLinesAndPrefix(lineNumber);
		this.printer.printNumeric(s); 
	}

	public void printString(int lineNumber, String s, String scopeInternalName)
	{
		addNewLinesAndPrefix(lineNumber);
		this.printer.printString(s, scopeInternalName);
	}
	
	public void printKeyword(int lineNumber, String keyword)
	{
		addNewLinesAndPrefix(lineNumber);
		this.printer.printKeyword(keyword);
	}
	
	public void printType(
			int lineNumber, String internalName, 
			String name, String scopeInternalName) 
	{ 
		addNewLinesAndPrefix(lineNumber);
		this.printer.printType(internalName, name, scopeInternalName); 
	}
	
	public void printField(
			int lineNumber, String internalName, String name, 
			String descriptor, String scopeInternalName) 
	{ 
		addNewLinesAndPrefix(lineNumber);
		this.printer.printField(
			internalName, name, descriptor, scopeInternalName);
	}
	
	public void printStaticField(
			int lineNumber, String internalName, String name, 
			String descriptor, String scopeInternalName) 
	{ 
		addNewLinesAndPrefix(lineNumber);
		this.printer.printStaticField(
			internalName, name, descriptor, scopeInternalName);
	}
	
	public void printMethod(
			int lineNumber, String internalName, String name, 
			String descriptor, String scopeInternalName)
	{ 
		addNewLinesAndPrefix(lineNumber);
		this.printer.printMethod(
			internalName, name, descriptor, scopeInternalName); 
	}

	public void printStaticMethod(
			int lineNumber, String internalName, String name, 
			String descriptor, String scopeInternalName)
	{ 
		addNewLinesAndPrefix(lineNumber);
		this.printer.printStaticMethod(
			internalName, name, descriptor, scopeInternalName); 
	}
	
	// -------------------------------------------------------------------------
	public void print(byte b) { this.printer.print(b); }
	public void print(char c) { this.printer.print(c); }
	public void print(int i) { this.printer.print(i); }
	public void print(String s) { this.printer.print(s); }

	public void printNumeric(String s) { this.printer.printNumeric(s); }
	public void printString(String s, String scopeInternalName) { this.printer.printString(s, scopeInternalName); }
	public void printKeyword(String keyword) { this.printer.printKeyword(keyword); }
	public void printJavaWord(String s) { this.printer.printJavaWord(s); }
	
	// TODO pourquoi de temps en temps passer un 'internalName' et d'autre fois un 'internalPath'? comprendre les besoin de jd-gui.
	public void printType(String internalName, String name, String scopeInternalName) 
		{ this.printer.printType(internalName, name, scopeInternalName); }
	public void printTypeDeclaration(String internalName, String name) 
		{ this.printer.printTypeDeclaration(internalName, name); }
	public void printTypeImport(String internalName, String name) 
		{ this.printer.printTypeImport(internalName, name); }

	public void printField(
			String internalName, String name, 
			String descriptor, String scopeInternalName)
		{ this.printer.printField(internalName, name, descriptor, scopeInternalName); }
	public void printFieldDeclaration(
			String internalName, String name, String descriptor)
		{ this.printer.printFieldDeclaration(internalName, name, descriptor); }
	
	public void printStaticField(
			String internalName, String name, 
			String descriptor, String scopeInternalName) 
		{ this.printer.printStaticField(internalName, name, descriptor, scopeInternalName); }
	public void printStaticFieldDeclaration(
			String internalName, String name, String descriptor)
		{ this.printer.printStaticFieldDeclaration(internalName, name, descriptor); }
	
	public void printConstructor(
			String internalName, String name, 
			String descriptor, String scopeInternalName) 
		{ this.printer.printConstructor(internalName, name, descriptor, scopeInternalName); }
	public void printConstructorDeclaration(
			String internalName, String name, String descriptor)
		{ this.printer.printConstructorDeclaration(internalName, name, descriptor); }
	public void printStaticConstructorDeclaration(
			String internalName, String name)
		{ this.printer.printStaticConstructorDeclaration(internalName, name); }
	
	public void printMethod(
			String internalName, String name, 
			String descriptor, String scopeInternalName)
		{ this.printer.printMethod(internalName, name, descriptor, scopeInternalName); }
	public void printMethodDeclaration(
			String internalName, String name, String descriptor)
		{ this.printer.printMethodDeclaration(internalName, name, descriptor); }
	
	public void printStaticMethod(
			String internalName, String name, 
			String descriptor, String scopeInternalName)
		{ this.printer.printStaticMethod(internalName, name, descriptor, scopeInternalName); }
	public void printStaticMethodDeclaration(
			String internalName, String name, String descriptor)
		{ this.printer.printStaticMethodDeclaration(internalName, name, descriptor); }
	
	public void start(int maxLineNumber, int majorVersion, int minorVersion)
		{ this.printer.start(maxLineNumber, majorVersion, minorVersion); }
	public void end() { this.printer.end(); }
	
	public void indent() { this.printer.indent(); }
	public void desindent() { this.printer.desindent(); }

	public void startOfLine(int lineNumber) 
		{ this.printer.startOfLine(lineNumber); }
	public void endOfLine() { this.printer.endOfLine(); }
	public void extraLine(int count) { this.printer.extraLine(count); }
	
	public void startOfComment() { this.printer.startOfComment(); }
	public void endOfComment() { this.printer.endOfComment(); }

	public void startOfJavadoc() { this.printer.startOfJavadoc(); }
	public void endOfJavadoc() { this.printer.endOfJavadoc(); }

	public void startOfXdoclet() { this.printer.startOfXdoclet(); }
	public void endOfXdoclet() { this.printer.endOfXdoclet(); }
	
	public void startOfError() { this.printer.startOfError(); }
	public void endOfError() { this.printer.endOfError(); }

	public void startOfImportStatements() { this.printer.startOfImportStatements(); }
	public void endOfImportStatements() { this.printer.endOfImportStatements(); }
	
	public void startOfTypeDeclaration(String internalPath) { this.printer.startOfTypeDeclaration(internalPath); }
	public void endOfTypeDeclaration() { this.printer.endOfTypeDeclaration(); }

	public void startOfAnnotationName() { this.printer.startOfAnnotationName(); }
	public void endOfAnnotationName() { this.printer.endOfAnnotationName(); }
	
	public void startOfOptionalPrefix() { this.printer.startOfOptionalPrefix(); }
	public void endOfOptionalPrefix() { this.printer.endOfOptionalPrefix(); }
	
	public void debugStartOfLayoutBlock() { this.printer.debugStartOfLayoutBlock(); }
	public void debugEndOfLayoutBlock() { this.printer.debugEndOfLayoutBlock(); }
	public void debugStartOfSeparatorLayoutBlock() 
		{ this.printer.debugStartOfSeparatorLayoutBlock(); }
	public void debugEndOfSeparatorLayoutBlock(int min, int value, int max) 
		{ this.printer.debugEndOfSeparatorLayoutBlock(min, value, max); }
	public void debugStartOfStatementsBlockLayoutBlock() 
		{ this.printer.debugStartOfStatementsBlockLayoutBlock(); }
	public void debugEndOfStatementsBlockLayoutBlock(int min, int value, int max)
		{ this.printer.debugEndOfStatementsBlockLayoutBlock(min, value, max); }
	public void debugStartOfInstructionBlockLayoutBlock()
		{ this.printer.debugStartOfInstructionBlockLayoutBlock(); }
	public void debugEndOfInstructionBlockLayoutBlock()
		{ this.printer.debugEndOfInstructionBlockLayoutBlock(); }
	public void debugStartOfCommentDeprecatedLayoutBlock()
		{ this.printer.debugStartOfCommentDeprecatedLayoutBlock(); }
	public void debugEndOfCommentDeprecatedLayoutBlock() 
		{ this.printer.debugEndOfCommentDeprecatedLayoutBlock(); }
	public void debugMarker(String marker) 
		{ this.printer.debugMarker(marker); }
	public void debugEndOfCaseBlockLayoutBlock() 
		{ this.printer.debugEndOfCaseBlockLayoutBlock(); }
	public void debugStartOfCaseBlockLayoutBlock()
		{ this.printer.debugStartOfCaseBlockLayoutBlock(); }
}
