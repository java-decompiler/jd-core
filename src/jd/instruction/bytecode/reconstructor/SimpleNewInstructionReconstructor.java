package jd.instruction.bytecode.reconstructor;

import java.util.List;

import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.InvokeNew;
import jd.instruction.bytecode.instruction.Invokespecial;
import jd.instruction.bytecode.instruction.New;


/*
 * Recontruction de l'instruction 'new' depuis le motif :
 * Invokespecial(New, <init>, [ IConst_1 ])
 */
public class SimpleNewInstructionReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		for (int index=0; index<list.size(); index++)
		{
			if (list.get(index).opcode != ByteCodeConstants.INVOKESPECIAL)
				continue;
			
			Invokespecial is = (Invokespecial)list.get(index);
			
			if (is.objectref.opcode != ByteCodeConstants.NEW)
				continue;
			
			InvokeNew invokeNew = new InvokeNew(
				ByteCodeConstants.INVOKENEW, is.offset, is.lineNumber,
				((New)is.objectref).index, is.index, is.args);
			
			list.set(index, invokeNew);
		}
	}
}
