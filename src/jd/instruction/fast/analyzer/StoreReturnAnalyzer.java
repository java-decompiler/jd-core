package jd.instruction.fast.analyzer;

import java.util.List;

import jd.classfile.LocalVariable;
import jd.classfile.LocalVariables;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.IndexInstruction;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.ReturnInstruction;
import jd.instruction.bytecode.instruction.StoreInstruction;


/**
 * Compacte les instructions 'store' suivies d'instruction 'return'.
 */
public class StoreReturnAnalyzer 
{
	public static void Cleanup(
		List<Instruction> list, LocalVariables localVariables)
	{
		int index = list.size();

		while (index-- > 1)
		{
			if (list.get(index).opcode != ByteCodeConstants.XRETURN)
				continue;
			
			ReturnInstruction ri = (ReturnInstruction)list.get(index);
			
			if (ri.lineNumber == Instruction.UNKNOWN_LINE_NUMBER)
				continue;
			
			switch (ri.valueref.opcode)
			{
			case ByteCodeConstants.ALOAD:
				if (list.get(index-1).opcode == ByteCodeConstants.ASTORE)
					index = Compact(list, localVariables, ri, index);
				break;
			case ByteCodeConstants.LOAD:
				if (list.get(index-1).opcode == ByteCodeConstants.STORE)
					index = Compact(list, localVariables, ri, index);
				break;
			case ByteCodeConstants.ILOAD:
				if (list.get(index-1).opcode == ByteCodeConstants.ISTORE)
					index = Compact(list, localVariables, ri, index);
				break;
			}
		}
	}
	
	private static int Compact(
		List<Instruction> list, LocalVariables localVariables, 
		ReturnInstruction ri, int index)
	{
		IndexInstruction load = (IndexInstruction)ri.valueref;
		StoreInstruction store = (StoreInstruction)list.get(index-1);
		
		if ((load.index == store.index) && 
			(load.lineNumber == store.lineNumber))
		{
			// Remove local variable
			LocalVariable lv = localVariables.
				getLocalVariableWithIndexAndOffset(
						store.index, store.offset);
			
			if ((lv != null) && (lv.start_pc == store.offset) && 
				(lv.start_pc + lv.length <= ri.offset))
				localVariables.
					removeLocalVariableWithIndexAndOffset(
							store.index, store.offset);
			// Replace returned instruction
			ri.valueref = store.valueref;
			// Remove 'store' instruction
			list.remove(--index);						
		}	
		
		return index;
	}
}
