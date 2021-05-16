package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

/*
 * Cree pour la construction de l'operateur ternaire.
 * Instancie par IfCmpFactory, IfFactory, IfXNullFactory & GotoFactory
 */
public class TernaryOpStore extends Instruction 
{
	public Instruction objectref;
	public int         ternaryOp2ndValueOffset;
	
	public TernaryOpStore(
			int opcode, int offset, int lineNumber, Instruction objectref, 
			int ternaryOp2ndValueOffset)
	{
		super(opcode, offset, lineNumber);
		this.objectref = objectref;
		this.ternaryOp2ndValueOffset = ternaryOp2ndValueOffset;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		return this.objectref.getReturnedSignature(constants, localVariables);
	}
}
