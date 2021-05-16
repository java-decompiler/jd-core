package jd.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.exception.UnexpectedOpcodeException;
import jd.instruction.bytecode.instruction.ALoad;
import jd.instruction.bytecode.instruction.AStore;
import jd.instruction.bytecode.instruction.IInc;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.LoadInstruction;
import jd.instruction.bytecode.instruction.Ret;
import jd.instruction.bytecode.instruction.StoreInstruction;


public class WideFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze,  
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset+1] & 255;
		final int index = ((code[offset+2] & 255) << 8) | (code[offset+3] & 255);
		
		if (opcode == ByteCodeConstants.IINC)
		{
			final int count = 
				(short)(((code[offset+4] & 255) << 8) | (code[offset+5] & 255));
			Instruction instruction = new IInc(
				opcode, offset, lineNumber, index, count);
			
			list.add(instruction);
			listForAnalyze.add(instruction);
			
			return 5;
		}
		else
		{
			if (opcode == ByteCodeConstants.RET)
			{
				list.add(new Ret(opcode, offset, lineNumber, index));
			}
			else
			{
				Instruction instruction = null;

				switch (opcode)
				{
				case ByteCodeConstants.ILOAD:
					instruction = new LoadInstruction(
						ByteCodeConstants.ILOAD, offset, lineNumber, index, "I");
					stack.push(instruction);
					break;
				case ByteCodeConstants.FLOAD:				
					instruction = new LoadInstruction(
						ByteCodeConstants.LOAD, offset, lineNumber, index, "F");
					stack.push(instruction);
					break;
				case ByteCodeConstants.ALOAD:				
					instruction = new ALoad(
						ByteCodeConstants.ALOAD, offset, lineNumber, index);
					stack.push(instruction);
					break;
				case ByteCodeConstants.LLOAD:				
					instruction = new LoadInstruction(
						ByteCodeConstants.LOAD, offset, lineNumber, index, "J");
					stack.push(instruction);
					break;
				case ByteCodeConstants.DLOAD:				
					instruction = new LoadInstruction(
						ByteCodeConstants.LOAD, offset, lineNumber, index, "D");
					stack.push(instruction);
					break;
				case ByteCodeConstants.ISTORE:				
					instruction = new StoreInstruction(
						ByteCodeConstants.ISTORE, offset, lineNumber, 
						index, "I", stack.pop());
					list.add(instruction);
					break;
				case ByteCodeConstants.FSTORE:				
					instruction = new StoreInstruction(
						ByteCodeConstants.STORE, offset, lineNumber, 
						index, "F", stack.pop());
					list.add(instruction);
					break;
				case ByteCodeConstants.ASTORE:				
					instruction = new AStore(
						ByteCodeConstants.ASTORE, offset, lineNumber,
						index, stack.pop());
					list.add(instruction);
					break;
				case ByteCodeConstants.LSTORE:				
					instruction = new StoreInstruction(
						ByteCodeConstants.STORE, offset, lineNumber, 
						index, "J", stack.pop());
					list.add(instruction);
					break;
				case ByteCodeConstants.DSTORE:				
					instruction = new StoreInstruction(
						ByteCodeConstants.STORE, offset, lineNumber, 
						index, "D", stack.pop());
					list.add(instruction);
					break;
				default:
					throw new UnexpectedOpcodeException(opcode);
				}

				listForAnalyze.add(instruction);
			}
			
			return 2;
		}
	}
}
