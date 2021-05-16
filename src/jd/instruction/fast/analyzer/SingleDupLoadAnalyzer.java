package jd.instruction.fast.analyzer;

import java.util.List;

import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.fast.visitor.CountDupLoadVisitor;
import jd.instruction.fast.visitor.ReplaceDupLoadVisitor;


/**
 * Efface les instructions DupStore si elles sont associées à une seule
 * instruction DupLoad.
 */
public class SingleDupLoadAnalyzer 
{
	public static void Cleanup(List<Instruction> list)
	{
		CountDupLoadVisitor countDupLoadVisitor = 
			new CountDupLoadVisitor();
		ReplaceDupLoadVisitor replaceDupLoadVisitor = 
			new ReplaceDupLoadVisitor();
				
		int length = list.size();
		
		// Effacement des instructions DupStore et DupLoad
		for (int dupStoreIndex=0; dupStoreIndex<length; dupStoreIndex++)
		{
			if (list.get(dupStoreIndex).opcode != ByteCodeConstants.DUPSTORE)
				continue;
			
			DupStore dupStore = (DupStore)list.get(dupStoreIndex);			
			countDupLoadVisitor.init(dupStore);
			
			for (int index=dupStoreIndex+1; index<length; ++index)
			{
				countDupLoadVisitor.visit(list.get(index));
				if (countDupLoadVisitor.getCounter() >= 2)
					break;
			}

			int counter = countDupLoadVisitor.getCounter();

			if (counter < 2)
			{
				if (counter > 0)
				{
					replaceDupLoadVisitor.init(dupStore, dupStore.objectref);					
					for (int index=dupStoreIndex+1; index<length; ++index)
						replaceDupLoadVisitor.visit(list.get(index));
				}
				
				list.remove(dupStoreIndex--);
				length--;
			}
		}	
	}
}
