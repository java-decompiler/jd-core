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
package jd.core.model.classfile;

import java.util.ArrayList;

public class LocalVariables 
{
	private ArrayList<LocalVariable> listOfLocalVariables;

	private int indexOfFirstLocalVariable = 0;

	public LocalVariables() 
	{
		this.listOfLocalVariables = new ArrayList<LocalVariable>(1);
	}

	public LocalVariables(
		LocalVariable[] localVariableTable, 
		LocalVariable[] localVariableTypeTable) 
	{
		int length = localVariableTable.length;

		this.listOfLocalVariables = new ArrayList<LocalVariable>(length);

		for (int i = 0; i < length; i++)
		{
			LocalVariable localVariable = localVariableTable[i];
					
			// Search local variable in 'localVariableTypeTable'
			if (localVariableTypeTable != null)
			{
				int typeLength = localVariableTypeTable.length;

				for (int j=0; j<typeLength; j++)
				{
					LocalVariable typeLocalVariable = localVariableTypeTable[j];

					if ((typeLocalVariable != null) &&
						localVariable.compareTo(typeLocalVariable) == 0)
					{
						localVariableTypeTable[j] = null;
						localVariable = typeLocalVariable;
						break;
					}
				}
			}
			
			add(localVariable);
		}
	}

	public void add(LocalVariable localVariable) 
	{
		final int length = this.listOfLocalVariables.size();
		final int index = localVariable.index;

		for (int i = 0; i < length; ++i) 
		{
			LocalVariable lv = this.listOfLocalVariables.get(i);

			if (
				((lv.index == index) && (lv.start_pc > localVariable.start_pc)) ||					
				(lv.index > index)
				)
			{
				this.listOfLocalVariables.add(i, localVariable);
				return;
			}
		}

		this.listOfLocalVariables.add(localVariable);
	}

	public LocalVariable get(int i) 
	{
		return this.listOfLocalVariables.get(i);
	}
	
	public void remove(int i) 
	{
		this.listOfLocalVariables.remove(i);
	}

	public String toString() 
	{
		return this.listOfLocalVariables.toString();
	}

	public LocalVariable getLocalVariableAt(int i) 
	{
		return (i >= this.listOfLocalVariables.size()) ? null
				: this.listOfLocalVariables.get(i);
	}

	public LocalVariable getLocalVariableWithIndexAndOffset(int index,
			int offset) 
	{
		int length = this.listOfLocalVariables.size();

		for (int i = length - 1; i >= 0; --i) {
			LocalVariable lv = this.listOfLocalVariables.get(i);

			if ((lv.index == index) && (lv.start_pc <= offset)
					&& (offset < lv.start_pc + lv.length))
				return lv;
		}

		return null;
	}

	public boolean containsLocalVariableWithNameIndex(int nameIndex) 
	{
		int length = this.listOfLocalVariables.size();

		for (int i = length - 1; i >= 0; --i) {
			LocalVariable lv = this.listOfLocalVariables.get(i);

			if (lv.name_index == nameIndex)
				return true;
		}

		return false;
	}

	public void removeLocalVariableWithIndexAndOffset(int index, int offset) 
	{
		int length = this.listOfLocalVariables.size();

		for (int i = length - 1; i >= 0; --i) {
			LocalVariable lv = this.listOfLocalVariables.get(i);

			if ((lv.index == index) && (lv.start_pc <= offset)
					&& (offset < lv.start_pc + lv.length))
			{
				this.listOfLocalVariables.remove(i);
				break;
			}
		}
	}
	
	public LocalVariable searchLocalVariableWithIndexAndOffset(int index,
			int offset) 
	{
		int length = this.listOfLocalVariables.size();

		for (int i = length - 1; i >= 0; --i) 
		{
			LocalVariable lv = this.listOfLocalVariables.get(i);

			if ((lv.index == index) && (lv.start_pc <= offset))
				return lv;
		}

		return null;
	}

	public int size() 
	{
		return this.listOfLocalVariables.size();
	}

	public int getIndexOfFirstLocalVariable() 
	{
		return indexOfFirstLocalVariable;
	}

	public void setIndexOfFirstLocalVariable(int indexOfFirstLocalVariable) 
	{
		this.indexOfFirstLocalVariable = indexOfFirstLocalVariable;
	}

	public int getMaxLocalVariableIndex() 
	{
		int length = this.listOfLocalVariables.size();

		return (length == 0) ? 
			-1 : this.listOfLocalVariables.get(length-1).index;
	}
}
