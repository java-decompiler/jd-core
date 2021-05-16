/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.process.analyzer.instruction.fast;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;
import jd.core.process.analyzer.instruction.fast.visitor.CountDupLoadVisitor;


/**
 * Efface les instructions DupStore si elles sont associ�es � une seule
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
