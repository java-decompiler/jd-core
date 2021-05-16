package jd.core.process.analyzer.instruction.bytecode.factory;

import java.util.List;
import java.util.Stack;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;


public class GotoFactory extends InstructionFactory
{
	public int create(
			ClassFile classFile, Method method, List<Instruction> list, 
			List<Instruction> listForAnalyze, 
			Stack<Instruction> stack, byte[] code, int offset, 
			int lineNumber, boolean[] jumps)
	{
		final int opcode = code[offset] & 255;
		final int value  = 
			(short)(((code[offset+1] & 255) << 8) | (code[offset+2] & 255));
		
		if (!stack.isEmpty() && !list.isEmpty())
			generateTernaryOpStore(
				list, listForAnalyze, stack, code, offset, value);
				
		list.add(new Goto(opcode, offset, lineNumber, value));
		
		return ByteCodeConstants.NO_OF_OPERANDS[opcode];
	}
	
	private static void generateTernaryOpStore(
		List<Instruction> list, List<Instruction> listForAnalyze, 
		Stack<Instruction> stack, byte[] code, int offset, int value)
	{
		int i = list.size();
		
		while (i-- > 0)
		{
			Instruction previousInstruction = list.get(i);
			
			switch (previousInstruction.opcode)
			{
			case ByteCodeConstants.IF:
			case ByteCodeConstants.IFCMP:
			case ByteCodeConstants.IFXNULL:
				{
					// Gestion de l'operateur ternaire
					final int ternaryOp2ndValueOffset = 
						search2ndValueOffset(code, offset, offset+value);
					
					final Instruction value0 = stack.pop();			
					TernaryOpStore tos = new TernaryOpStore(
						ByteCodeConstants.TERNARYOPSTORE, offset-1, 
						value0.lineNumber, value0, ternaryOp2ndValueOffset);
						
					list.add(tos);
					listForAnalyze.add(tos);				
				}
				return;
			}
		}		
	}
	
	private static int search2ndValueOffset(
			byte[] code, int offset, int jumpOffset)
	{
		int result = offset;
		
		while (offset < jumpOffset)
		{			
			int opcode = code[offset] & 255;
			
			// on retient l'offset de la derniere opération placant une 
			// information sur la pile.
			switch (opcode)
			{			
			case ByteCodeConstants.ACONST_NULL:
			case ByteCodeConstants.ICONST_M1:
			case ByteCodeConstants.ICONST_0:
			case ByteCodeConstants.ICONST_1:
			case ByteCodeConstants.ICONST_2:
			case ByteCodeConstants.ICONST_3:
			case ByteCodeConstants.ICONST_4:
			case ByteCodeConstants.ICONST_5:
			case ByteCodeConstants.LCONST_0:
			case ByteCodeConstants.LCONST_1:
			case ByteCodeConstants.FCONST_0:
			case ByteCodeConstants.FCONST_1:
			case ByteCodeConstants.FCONST_2:
			case ByteCodeConstants.DCONST_0:
			case ByteCodeConstants.DCONST_1:
			case ByteCodeConstants.BIPUSH:
			case ByteCodeConstants.SIPUSH:
			case ByteCodeConstants.LDC:
			case ByteCodeConstants.LDC_W:
			case ByteCodeConstants.LDC2_W:
			case ByteCodeConstants.ILOAD:
			case ByteCodeConstants.LLOAD:
			case ByteCodeConstants.FLOAD:
			case ByteCodeConstants.DLOAD:
			case ByteCodeConstants.ALOAD:
			case ByteCodeConstants.ILOAD_0:
			case ByteCodeConstants.ILOAD_1:
			case ByteCodeConstants.ILOAD_2:
			case ByteCodeConstants.ILOAD_3:
			case ByteCodeConstants.LLOAD_0:
			case ByteCodeConstants.LLOAD_1:
			case ByteCodeConstants.LLOAD_2:
			case ByteCodeConstants.LLOAD_3:
			case ByteCodeConstants.FLOAD_0:
			case ByteCodeConstants.FLOAD_1:
			case ByteCodeConstants.FLOAD_2:
			case ByteCodeConstants.FLOAD_3:
			case ByteCodeConstants.DLOAD_0:
			case ByteCodeConstants.DLOAD_1:
			case ByteCodeConstants.DLOAD_2:
			case ByteCodeConstants.DLOAD_3:
			case ByteCodeConstants.ALOAD_0:
			case ByteCodeConstants.ALOAD_1:
			case ByteCodeConstants.ALOAD_2:
			case ByteCodeConstants.ALOAD_3:
			case ByteCodeConstants.IALOAD:
			case ByteCodeConstants.LALOAD:
			case ByteCodeConstants.FALOAD:
			case ByteCodeConstants.DALOAD:
			case ByteCodeConstants.AALOAD:
			case ByteCodeConstants.BALOAD:
			case ByteCodeConstants.CALOAD:
			case ByteCodeConstants.SALOAD:
			case ByteCodeConstants.DUP:
			case ByteCodeConstants.DUP_X1:
			case ByteCodeConstants.DUP_X2:
			case ByteCodeConstants.DUP2:
			case ByteCodeConstants.DUP2_X1:
			case ByteCodeConstants.DUP2_X2:
			case ByteCodeConstants.SWAP:
			case ByteCodeConstants.IADD:
			case ByteCodeConstants.LADD:
			case ByteCodeConstants.FADD:
			case ByteCodeConstants.DADD:
			case ByteCodeConstants.ISUB:
			case ByteCodeConstants.LSUB:
			case ByteCodeConstants.FSUB:
			case ByteCodeConstants.DSUB:
			case ByteCodeConstants.IMUL:
			case ByteCodeConstants.LMUL:
			case ByteCodeConstants.FMUL:
			case ByteCodeConstants.DMUL:
			case ByteCodeConstants.IDIV:
			case ByteCodeConstants.LDIV:
			case ByteCodeConstants.FDIV:
			case ByteCodeConstants.DDIV:
			case ByteCodeConstants.IREM:
			case ByteCodeConstants.LREM:
			case ByteCodeConstants.FREM:
			case ByteCodeConstants.DREM:
			case ByteCodeConstants.INEG:
			case ByteCodeConstants.LNEG:
			case ByteCodeConstants.FNEG:
			case ByteCodeConstants.DNEG:
			case ByteCodeConstants.ISHL:
			case ByteCodeConstants.LSHL:
			case ByteCodeConstants.ISHR:
			case ByteCodeConstants.LSHR:
			case ByteCodeConstants.IUSHR:
			case ByteCodeConstants.LUSHR:
			case ByteCodeConstants.IAND:
			case ByteCodeConstants.LAND:
			case ByteCodeConstants.IOR:
			case ByteCodeConstants.LOR:
			case ByteCodeConstants.IXOR:
			case ByteCodeConstants.LXOR:
			case ByteCodeConstants.IINC:
			case ByteCodeConstants.I2L:
			case ByteCodeConstants.I2F:
			case ByteCodeConstants.I2D:
			case ByteCodeConstants.L2I:
			case ByteCodeConstants.L2F:
			case ByteCodeConstants.L2D:
			case ByteCodeConstants.F2I:
			case ByteCodeConstants.F2L:
			case ByteCodeConstants.F2D:
			case ByteCodeConstants.D2I:
			case ByteCodeConstants.D2L:
			case ByteCodeConstants.D2F:
			case ByteCodeConstants.I2B:
			case ByteCodeConstants.I2C:
			case ByteCodeConstants.I2S:
			case ByteCodeConstants.LCMP:
			case ByteCodeConstants.FCMPL:
			case ByteCodeConstants.FCMPG:
			case ByteCodeConstants.DCMPL:
			case ByteCodeConstants.DCMPG:
			case ByteCodeConstants.GETSTATIC:
			case ByteCodeConstants.OUTERTHIS:
			case ByteCodeConstants.GETFIELD:
			case ByteCodeConstants.INVOKEVIRTUAL:
			case ByteCodeConstants.INVOKESPECIAL:
			case ByteCodeConstants.INVOKESTATIC:
			case ByteCodeConstants.INVOKEINTERFACE:
			case ByteCodeConstants.NEW:
			case ByteCodeConstants.NEWARRAY:
			case ByteCodeConstants.ANEWARRAY:
			case ByteCodeConstants.ARRAYLENGTH:
			case ByteCodeConstants.CHECKCAST:
			case ByteCodeConstants.INSTANCEOF:
			case ByteCodeConstants.WIDE:
			case ByteCodeConstants.MULTIANEWARRAY:			
			// Extension for decompiler
			case ByteCodeConstants.ICONST:
			case ByteCodeConstants.LCONST:
			case ByteCodeConstants.FCONST:
			case ByteCodeConstants.DCONST:
			case ByteCodeConstants.DUPLOAD:
			case ByteCodeConstants.ASSIGNMENT:
			case ByteCodeConstants.UNARYOP:
			case ByteCodeConstants.BINARYOP:
			case ByteCodeConstants.LOAD:
			case ByteCodeConstants.EXCEPTIONLOAD:
			case ByteCodeConstants.ARRAYLOAD:
			case ByteCodeConstants.INVOKENEW:
			case ByteCodeConstants.CONVERT:	
			case ByteCodeConstants.IMPLICITCONVERT:	
			case ByteCodeConstants.PREINC:
			case ByteCodeConstants.POSTINC:
				result = offset;			
			}
			
			int nbOfOperands = ByteCodeConstants.NO_OF_OPERANDS[opcode];
				
			switch (nbOfOperands)
			{
			case ByteCodeConstants.NO_OF_OPERANDS_UNPREDICTABLE:
				switch (opcode)
				{
				case ByteCodeConstants.TABLESWITCH:
					offset = ByteCodeUtil.NextTableSwitchOffset(code, offset);		
					break;
				case ByteCodeConstants.LOOKUPSWITCH:
					offset = ByteCodeUtil.NextLookupSwitchOffset(code, offset);		
					break;
				case ByteCodeConstants.WIDE:
					offset = ByteCodeUtil.NextWideOffset(code, offset);						
				}
				break;
			case ByteCodeConstants.NO_OF_OPERANDS_UNDEFINED:
				break;
			default:
				offset += nbOfOperands;
			}
			
			++offset;
		}
		
		return result;
	}
}
