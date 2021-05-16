package jd.core.process.layouter.visitor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.layout.block.InstructionsLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.model.layout.block.LayoutBlockConstants;
import jd.core.preferences.Preferences;
import jd.core.process.layouter.ClassFileLayouter;

public class InstructionsSplitterVisitor extends BaseInstructionSplitterVisitor 
{
	protected Preferences preferences;
	protected List<LayoutBlock> layoutBlockList;
	protected Method method;
	protected List<Instruction> list;
	protected int firstLineNumber;
	protected int maxLineNumber;
	protected int initialIndex1;
	protected int index1;
	protected int index2;
	protected int offset1;
	
	public InstructionsSplitterVisitor() {}

	public void start(
		Preferences preferences, 
		List<LayoutBlock> layoutBlockList, ClassFile classFile, 
		Method method, List<Instruction> list, int index1)
	{
		super.start(classFile);
		
		this.preferences = preferences;
		this.layoutBlockList = layoutBlockList;
		this.method = method;
		this.list = list;
		this.firstLineNumber = this.maxLineNumber = 
			Instruction.UNKNOWN_LINE_NUMBER;
		this.initialIndex1 = this.index1 = index1;
		this.offset1 = 0;
	}
	
	public void end()
	{	
		int lastOffset = this.list.get(this.index2).offset;
		
		// S'il reste un fragment d'instruction a traiter...
		if ((this.index1 != this.index2) || (this.offset1 != lastOffset)) 
		{
	    	// Add last part of instruction		
	    	int lastLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
	    	
	    	for (int j=index2; j>=index1; j--)
			{
				Instruction instruction = list.get(j);
				if (instruction.lineNumber != Instruction.UNKNOWN_LINE_NUMBER)
				{
					lastLineNumber = MaxLineNumberVisitor.visit(instruction);
					break;
				}
			}
	    	
			addInstructionsLayoutBlock(lastLineNumber, lastOffset);
		}
	}
	
	public void setIndex2(int index2) 
	{
		this.index2 = index2;
	}
	
	public void visit(Instruction instruction)
	{
		if (this.firstLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
		{
			// Bloc executé soit lors de la visite de 
			// - du 1er statement
			// - d'un statement qui suit un statement dont la derniere 
			//   instruction est 'AnonymousNewInvoke'
			// Assez complexe a comprendre sans exemple sous les yeux 			
			// Methode d'exemple :
			//   java.io.ObjectInputStream, auditSubclass(...)
			int initialFirstLineNumber = 
				this.list.get(this.initialIndex1).lineNumber;
			
			if (initialFirstLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				// Si la methode possede des numeros de lignes
				if (initialFirstLineNumber < instruction.lineNumber)
				{
					// Cas d'un statement qui suit un statement dont la derniere 
					//   instruction est 'AnonymousNewInvoke' ==> on fait 
					//   commencer le bloc a la ligne precedent. 
					this.firstLineNumber = instruction.lineNumber - 1;	
				}
				else
				{
					// Cas du 1er statement
					this.firstLineNumber = instruction.lineNumber;
				}
			}
		}
		
		super.visit(null, instruction);
	}

	protected void visit(Instruction parent, Instruction instruction)
	{
		if (instruction.lineNumber == Instruction.UNKNOWN_LINE_NUMBER)
		{
			instruction.lineNumber = this.maxLineNumber;
		}
		else if (this.maxLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
		{
			this.maxLineNumber = instruction.lineNumber;
		}
		else if (instruction.lineNumber < this.maxLineNumber)
		{
			// Modification du numero de ligne fournit dans le fichier CLASS !
			instruction.lineNumber = this.maxLineNumber;
		}
		
		if (this.firstLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
		{
			// Bloc executé si une instruction 'AnonymousNewInvoke' vient 
			// d'etre traitee.
			this.firstLineNumber = instruction.lineNumber;
		}
		
		super.visit(parent, instruction);
	}

	public void visitAnonymousNewInvoke(
		Instruction parent, InvokeNew in, ClassFile innerClassFile) 
	{
		// Add a new part of instruction
		addInstructionsLayoutBlock(in.lineNumber, in.offset);
		
		// Add blocks for inner class body
		this.maxLineNumber = 
			ClassFileLayouter.CreateBlocksForBodyOfAnonymousClass(
				this.preferences, innerClassFile, this.layoutBlockList);
		
		this.firstLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		this.index1 = this.index2;
		this.offset1 = in.offset;
	}
	
	protected void addInstructionsLayoutBlock(int lastLineNumber, int lastOffset) 
	{
    	int preferedLineCount;
		
		if ((this.firstLineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
			(lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER))
		{
			if (lastLineNumber < this.firstLineNumber)
			{
				// Les instructions newAnonymousClass imbriquées n'ont pas de 
				// numéros de ligne correctes. Exemple: com.googlecode.dex2jar.v3.Dex2jar
				lastLineNumber = this.firstLineNumber;
			}
			preferedLineCount = lastLineNumber - this.firstLineNumber;
		}
		else
		{
			preferedLineCount = LayoutBlockConstants.UNLIMITED_LINE_COUNT;
		}
		
		this.layoutBlockList.add(new InstructionsLayoutBlock(		
			this.firstLineNumber, lastLineNumber, 
			preferedLineCount, preferedLineCount, preferedLineCount,
			this.classFile, this.method, this.list, 
			this.index1, this.index2, 
			this.offset1, lastOffset));
	}
}
