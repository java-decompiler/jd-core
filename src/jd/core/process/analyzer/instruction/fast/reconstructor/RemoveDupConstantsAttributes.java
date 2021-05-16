package jd.core.process.analyzer.instruction.fast.reconstructor;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;


/*
 * Retrait des instructions DupLoads & DupStore associés à une constante ou un 
 * attribut:
 * DupStore( GetField | GetStatic | BIPush | SIPush | ALoad )
 * ...
 * ???( DupLoad )
 * ...
 * ???( DupLoad )
 */
public class RemoveDupConstantsAttributes 
{
	public static void Reconstruct(List<Instruction> list)
	{
		for (int dupStoreIndex=0; dupStoreIndex<list.size(); dupStoreIndex++)
		{
			if (list.get(dupStoreIndex).opcode != ByteCodeConstants.DUPSTORE)
				continue;

			// DupStore trouvé
			DupStore dupstore = (DupStore)list.get(dupStoreIndex);
			
			int opcode = dupstore.objectref.opcode;
			
			if (/*(opcode != ByteCodeConstants.GETFIELD) && 
				(opcode != ByteCodeConstants.GETSTATIC) &&*/
				(opcode != ByteCodeConstants.BIPUSH) &&
				(opcode != ByteCodeConstants.SIPUSH) /*&&
				(opcode != ByteCodeConstants.ALOAD) &&
				(opcode != ByteCodeConstants.ILOAD)*/)
				continue;
						
			Instruction i = dupstore.objectref;
			int dupLoadIndex = dupStoreIndex+1;
			ReplaceDupLoadVisitor visitor = 
				new ReplaceDupLoadVisitor(dupstore, i);
			final int length = list.size();
			
			// 1er substitution
			while (dupLoadIndex < length)
			{
				visitor.visit(list.get(dupLoadIndex));
				if (visitor.getParentFound() != null)
					break;
				dupLoadIndex++;
			}			

			visitor.init(dupstore, i);
			
			// 2eme substitution
			while (dupLoadIndex < length)
			{
				visitor.visit(list.get(dupLoadIndex));
				if (visitor.getParentFound() != null)
					break;
				dupLoadIndex++;
			}			

			list.remove(dupStoreIndex--);
		}	
	}
}
