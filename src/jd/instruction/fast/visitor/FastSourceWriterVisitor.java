package jd.instruction.fast.visitor;

import java.util.HashSet;

import jd.Preferences;
import jd.classfile.ClassFile;
import jd.classfile.LocalVariable;
import jd.classfile.LocalVariables;
import jd.classfile.writer.SignatureWriter;
import jd.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.instruction.bytecode.instruction.IfCmp;
import jd.instruction.bytecode.instruction.IfInstruction;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.visitor.ByteCodeSourceWriterVisitor;
import jd.instruction.fast.FastConstants;
import jd.instruction.fast.instruction.FastDeclaration;
import jd.printer.Printer;
import jd.util.ReferenceMap;



public class FastSourceWriterVisitor 
	extends ByteCodeSourceWriterVisitor
{
	public FastSourceWriterVisitor(
			HashSet<String> keywordSet, Preferences preferences, Printer spw, 
			ReferenceMap referenceMap, ClassFile classFile, 
			int methodAccessFlags, LocalVariables localVariables)
	{
		super(
			keywordSet, preferences, spw, referenceMap, classFile, 
			methodAccessFlags, localVariables);
	}

	public void visit(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case FastConstants.IF:
			writeIfTest((IfInstruction)instruction);
			break;
		case FastConstants.IFCMP:
			writeIfCmpTest((IfCmp)instruction);
			break;
		case FastConstants.IFXNULL:
			writeIfXNullTest((IfInstruction)instruction);
			break;
		case FastConstants.COMPLEXIF:
			writeComplexConditionalBranchInstructionTest((ComplexConditionalBranchInstruction)instruction);
			break;
		case FastConstants.DECLARE:
			WriteDeclaration((FastDeclaration)instruction);
			break;
		default:
			super.visit(instruction);
		}
	}
	
	private void WriteDeclaration(FastDeclaration fd)
	{
		LocalVariable lv = 
			localVariables.getLocalVariableWithIndexAndOffset(fd.index, fd.offset);
		
		if (lv == null)
		{
			if (fd.instruction == null)
				spw.print(
					Instruction.UNKNOWN_LINE_NUMBER, "???");
			else
				visit(fd.instruction);
		}
		else
		{
			String signature = this.constants.getConstantUtf8(lv.signature_index);
			
			int lineNumber = fd.lineNumber;
			
			spw.print(
				lineNumber, 
				SignatureWriter.WriteSimpleSignature(
					referenceMap, classFile, signature));
			spw.print(lineNumber, ' ');
			
			if (fd.instruction == null)
				spw.print(
					lineNumber, constants.getConstantUtf8(lv.name_index));
			else
				visit(fd.instruction);
		}
	}
}
