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
import java.util.List;

public class LocalVariables
{
    private final List<LocalVariable> listOfLocalVariables;

    private int indexOfFirstLocalVariable;

    public LocalVariables()
    {
        this.listOfLocalVariables = new ArrayList<>(1);
    }

    public LocalVariables(
        LocalVariable[] localVariableTable,
        LocalVariable[] localVariableTypeTable)
    {
        int length = localVariableTable.length;

        this.listOfLocalVariables = new ArrayList<>(length);

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

                    if (typeLocalVariable != null &&
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
        final int index = localVariable.getIndex();

        for (int i = 0; i < length; ++i)
        {
            LocalVariable lv = this.listOfLocalVariables.get(i);

            if (
                lv.getIndex() == index && lv.getStartPc() > localVariable.getStartPc() ||
                lv.getIndex() > index
                )
            {
                this.listOfLocalVariables.add(i, localVariable);
                return;
            }
        }

        this.listOfLocalVariables.add(localVariable);
    }

    @Override
    public String toString()
    {
        return this.listOfLocalVariables.toString();
    }

    public LocalVariable getLocalVariableAt(int i)
    {
        return i >= this.listOfLocalVariables.size() ? null
                : this.listOfLocalVariables.get(i);
    }

    public LocalVariable getLocalVariableWithIndexAndOffset(int index,
            int offset)
    {
        int length = this.listOfLocalVariables.size();

        for (int i = length - 1; i >= 0; --i) {
            LocalVariable lv = this.listOfLocalVariables.get(i);

            if (lv.getIndex() == index && lv.getStartPc() <= offset
                    && offset < lv.getStartPc() + lv.getLength()) {
                return lv;
            }
        }

        return null;
    }

    public boolean containsLocalVariableWithNameIndex(int nameIndex)
    {
        int length = this.listOfLocalVariables.size();

        for (int i = length - 1; i >= 0; --i) {
            LocalVariable lv = this.listOfLocalVariables.get(i);

            if (lv.getNameIndex() == nameIndex) {
                return true;
            }
        }

        return false;
    }

    public void removeLocalVariableWithIndexAndOffset(int index, int offset)
    {
        int length = this.listOfLocalVariables.size();

        for (int i = length - 1; i >= 0; --i) {
            LocalVariable lv = this.listOfLocalVariables.get(i);

            if (lv.getIndex() == index && lv.getStartPc() <= offset
                    && offset < lv.getStartPc() + lv.getLength())
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

            if (lv.getIndex() == index && lv.getStartPc() <= offset) {
                return lv;
            }
        }

        return null;
    }

    public void removeUselessLocalVariables()
    {
        for (int i = this.listOfLocalVariables.size() - 1; i >= 0; i--) {
            LocalVariable lv = this.listOfLocalVariables.get(i);
            if (lv.isToBeRemoved()) {
                this.listOfLocalVariables.remove(i);
            }
        }
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

        return length == 0 ?
            -1 : this.listOfLocalVariables.get(length-1).getIndex();
    }
    
    public List<String> getNames(ConstantPool constantPool) {
        return this.listOfLocalVariables.stream().map(constantPool::getLocalVariableName).toList();
    }
}
