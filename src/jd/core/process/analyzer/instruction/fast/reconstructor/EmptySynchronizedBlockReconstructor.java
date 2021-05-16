package jd.core.process.analyzer.instruction.fast.reconstructor;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastSynchronized;


public class EmptySynchronizedBlockReconstructor 
{
	public static void Reconstruct(
		LocalVariables localVariables, List<Instruction> list)
	{
		int index = list.size();
		
		while (index-- > 2)
		{
			Instruction monitorExit = list.get(index);
			
			if (monitorExit.opcode != ByteCodeConstants.MONITOREXIT)
				continue;

			Instruction instruction = list.get(index-1);
			
			if (instruction.opcode != ByteCodeConstants.MONITORENTER)
				continue;
			
			MonitorEnter me = (MonitorEnter)instruction;
			
			if (me.objectref.opcode != ByteCodeConstants.DUPLOAD)
				continue;
			
			DupStore dupStore;
			instruction = list.get(index-2);

			if (instruction.opcode == ByteCodeConstants.DUPSTORE)
			{			
				dupStore = (DupStore)instruction;
			}
			else if (instruction.opcode == ByteCodeConstants.ASTORE)
			{
				if (index <= 2)
					continue;
				
				AStore astore = (AStore)instruction;
				
				instruction = list.get(index-3);					
				if (instruction.opcode != ByteCodeConstants.DUPSTORE)
					continue;

				dupStore = (DupStore)instruction;

				// Remove local variable for monitor
				localVariables.removeLocalVariableWithIndexAndOffset(
						astore.index, astore.offset); 				
				// Remove MonitorExit
				list.remove(index--);
			}
			else
			{
				continue;
			}
				
			FastSynchronized fastSynchronized = new FastSynchronized(
				FastConstants.SYNCHRONIZED, monitorExit.offset, 
				instruction.lineNumber,  1, new ArrayList<Instruction>());
			fastSynchronized.monitor = dupStore.objectref;

			// Remove MonitorExit/MonitorEnter
			list.remove(index--);
			// Remove MonitorEnter/Astore
			list.remove(index--);
			// Replace DupStore with FastSynchronized
			list.set(index, fastSynchronized);
		}		
	}	
}
