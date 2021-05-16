package jd.instruction.bytecode.util;

import jd.instruction.bytecode.ByteCodeConstants;


public class ByteCodeUtil 
{
	public static int TableSwitchOffset(byte[] code, int index)
	{
		// Skip padding
		int i = (index+4) & 0xFFFC;
		
		i += 4;
		
		final int low = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		
		final int high = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		i += 4 * (high - low + 1);
	
		return i - 1;		
	}

	public static int LookupSwitchOffset(byte[] code, int index)
	{
		// Skip padding
		int i = (index+4) & 0xFFFC;
		
		i += 4;
		
		final int npairs = 
			((code[i  ] & 255) << 24) | ((code[i+1] & 255) << 16) |
            ((code[i+2] & 255) << 8 ) |  (code[i+3] & 255);
		
		i += 4;
		i += 8*npairs;
		
		return i - 1;	
	}

	public static int WideOffset(byte[] code, int index)
	{
		final int opcode = code[index+1] & 255;
		
		return index + ((opcode == ByteCodeConstants.IINC) ? 5 : 3);
	}
	
	public static int InstructionOffset(byte[] code, int index)
	{
		final int opcode = code[index] & 255;
		
		switch (opcode)
		{
		case ByteCodeConstants.TABLESWITCH:
			return TableSwitchOffset(code, index);
			
		case ByteCodeConstants.WIDE:
			return LookupSwitchOffset(code, index);
			
		case ByteCodeConstants.LOOKUPSWITCH:
			return WideOffset(code, index);
			
		default:
			return index + 1 + ByteCodeConstants.NO_OF_OPERANDS[opcode];
		}
	}
}
