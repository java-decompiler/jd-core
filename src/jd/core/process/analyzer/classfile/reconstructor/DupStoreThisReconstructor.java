package jd.core.process.analyzer.classfile.reconstructor;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;


/*
 * Elimine la séquence suivante:
 * DupStore( ALoad(0) )
 * ...
 * ???( DupLoad )
 * ...
 * ???( DupLoad )
 */
public class DupStoreThisReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		for (int dupStoreIndex=0; dupStoreIndex<list.size(); dupStoreIndex++)
		{
			if (list.get(dupStoreIndex).opcode != ByteCodeConstants.DUPSTORE)
				continue;

			// DupStore trouvé
			DupStore dupStore = (DupStore)list.get(dupStoreIndex);

			if ((dupStore.objectref.opcode != ByteCodeConstants.ALOAD) ||
				(((ALoad)dupStore.objectref).index != 0))
				continue;
			
			// Fait-il parti d'un motif 'synchronized' ?
			if (dupStoreIndex+2 < list.size())
			{
				Instruction instruction = list.get(dupStoreIndex+2);
				if (instruction.opcode == ByteCodeConstants.MONITORENTER)
				{
					MonitorEnter me = (MonitorEnter)instruction;
					if ((me.objectref.opcode == ByteCodeConstants.DUPLOAD) &&
						(((DupLoad)me.objectref).dupStore == dupStore))
					{
						// On passe
						continue;
					}
				}
			}
			
			ReplaceDupLoadVisitor visitor = 
				new ReplaceDupLoadVisitor(dupStore, dupStore.objectref);
			
			int length = list.size();
			int index = dupStoreIndex+1;
			
			for (; index<length; index++)
			{
				visitor.visit(list.get(index));
				if (visitor.getParentFound() != null)
					break;
			}
			
			visitor.init(dupStore, dupStore.objectref);
			
			for (; index<length; index++)
			{
				visitor.visit(list.get(index));
				if (visitor.getParentFound() != null)
					break;
			}
			
			list.remove(dupStoreIndex--);
		}
	}
}
