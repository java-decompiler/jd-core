package jd.core.process.analyzer.classfile.reconstructor;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.ConstInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.process.analyzer.util.ReconstructorUtil;


/*
 * Recontruction des pre-incrementations depuis le motif :
 * DupStore( (i - 1F) )
 * ...
 * {?Store | PutField | PutStatic}( DupLoad )
 * ...
 * ???( DupLoad )
 */
public class PreIncReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		int length = list.size();

		for (int dupStoreIndex=0; dupStoreIndex<length; dupStoreIndex++)
		{
			if (list.get(dupStoreIndex).opcode != ByteCodeConstants.DUPSTORE)
				continue;

			// DupStore trouvé
			DupStore dupstore = (DupStore)list.get(dupStoreIndex);
			
			if ((dupstore.objectref.opcode != ByteCodeConstants.BINARYOP))
				continue;
			
			BinaryOperatorInstruction boi = 
				(BinaryOperatorInstruction)dupstore.objectref;
			
			if ((boi.value2.opcode != ByteCodeConstants.ICONST) && 
				(boi.value2.opcode != ByteCodeConstants.LCONST) && 
				(boi.value2.opcode != ByteCodeConstants.DCONST) && 
				(boi.value2.opcode != ByteCodeConstants.FCONST))
				continue;

			ConstInstruction ci = (ConstInstruction)boi.value2;
			
			if (ci.value != 1)
				continue;
			
			int value;
			
			if (boi.operator.equals("+"))
				value = 1;
			else if (boi.operator.equals("-"))
				value = -1;
			else
				continue;			
			
			int xstorePutfieldPutstaticIndex = dupStoreIndex;
			
			while (++xstorePutfieldPutstaticIndex < length)
			{
				Instruction i = list.get(xstorePutfieldPutstaticIndex);
				Instruction dupload = null;
				
				switch (i.opcode)
				{
				case ByteCodeConstants.ASTORE:
					if ((boi.value1.opcode == ByteCodeConstants.ALOAD) && 
						(((StoreInstruction)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
						(((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouvé
						dupload = (DupLoad)((StoreInstruction)i).valueref;
						break;
				case ByteCodeConstants.ISTORE:
					if ((boi.value1.opcode == ByteCodeConstants.ILOAD) &&
						(((StoreInstruction)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
						(((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouvé
						dupload = (DupLoad)((StoreInstruction)i).valueref;
						break;
				case ByteCodeConstants.STORE:
					if ((boi.value1.opcode == ByteCodeConstants.LOAD) &&
						(((StoreInstruction)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
						(((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouvé
						dupload = (DupLoad)((StoreInstruction)i).valueref;
					break;
				case ByteCodeConstants.PUTFIELD:
					if ((boi.value1.opcode == ByteCodeConstants.GETFIELD) &&
						(((PutField)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
					    (((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouvé
						dupload = (DupLoad)((PutField)i).valueref;
					break;
				case ByteCodeConstants.PUTSTATIC:
					if ((boi.value1.opcode == ByteCodeConstants.GETSTATIC) &&
						(((PutStatic)i).valueref.opcode == ByteCodeConstants.DUPLOAD) &&
				        (((IndexInstruction)i).index == ((IndexInstruction)boi.value1).index))
						// 1er DupLoad trouvé
						dupload = (DupLoad)((PutStatic)i).valueref;
					break;
				}
					
				if ((dupload == null) || (dupload.offset != dupstore.offset))
					continue;
				
				Instruction preinc = new IncInstruction(
					ByteCodeConstants.PREINC, boi.offset, 
					boi.lineNumber, boi.value1, value);

				ReconstructorUtil.ReplaceDupLoad(
						list, xstorePutfieldPutstaticIndex+1, dupstore, preinc);
				
				list.remove(xstorePutfieldPutstaticIndex);
				list.remove(dupStoreIndex);
				dupStoreIndex--;
				length = list.size();
				break;
			}			
		}	
	}
}
