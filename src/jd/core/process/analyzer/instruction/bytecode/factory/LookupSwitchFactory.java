package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;


public class LookupSwitchFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;

		// Skip padding
		int i = (offset+4) & 0xFFFC;
		
		final int defaultOffset = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		
		final int npairs = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		
		int[] keys    = new int[npairs];
		int[] offsets = new int[npairs];
		
		for (int j=0; j<npairs; j++)
		{
			keys[j] = 
				((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
	            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
							
			i += 4;
			
			offsets[j] = 
				((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
	            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
			
			i += 4;			
		}
		
		final Instruction key = stack.pop();

		list.add(new LookupSwitch(
			opcode, offset, lineNumber, key, defaultOffset, offsets, keys));
		
		return (i - offset - 1);
	}
}
