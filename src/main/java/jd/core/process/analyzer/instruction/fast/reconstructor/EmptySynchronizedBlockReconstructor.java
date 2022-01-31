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
package jd.core.process.analyzer.instruction.fast.reconstructor;

import org.apache.bcel.Const;

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

public final class EmptySynchronizedBlockReconstructor
{
    private EmptySynchronizedBlockReconstructor() {
        super();
    }

    public static void reconstruct(
        LocalVariables localVariables, List<Instruction> list)
    {
        int index = list.size();

        while (index-- > 2)
        {
            Instruction monitorExit = list.get(index);

            if (monitorExit.getOpcode() != Const.MONITOREXIT) {
                continue;
            }

            Instruction instruction = list.get(index-1);

            if (instruction.getOpcode() != Const.MONITORENTER) {
                continue;
            }

            MonitorEnter me = (MonitorEnter)instruction;

            if (me.getObjectref().getOpcode() != ByteCodeConstants.DUPLOAD) {
                continue;
            }

            DupStore dupStore;
            instruction = list.get(index-2);

            if (instruction.getOpcode() == ByteCodeConstants.DUPSTORE)
            {
                dupStore = (DupStore)instruction;
            }
            else if (instruction.getOpcode() == Const.ASTORE)
            {
                if (index <= 2) {
                    continue;
                }

                AStore astore = (AStore)instruction;

                instruction = list.get(index-3);
                if (instruction.getOpcode() != ByteCodeConstants.DUPSTORE) {
                    continue;
                }

                dupStore = (DupStore)instruction;

                // Remove local variable for monitor
                localVariables.removeLocalVariableWithIndexAndOffset(
                        astore.getIndex(), astore.getOffset());
                // Remove MonitorExit
                list.remove(index--);
            }
            else
            {
                continue;
            }

            FastSynchronized fastSynchronized = new FastSynchronized(
                FastConstants.SYNCHRONIZED, monitorExit.getOffset(),
                instruction.getLineNumber(),  1, new ArrayList<>());
            fastSynchronized.setMonitor(dupStore.getObjectref());

            // Remove MonitorExit/MonitorEnter
            list.remove(index--);
            // Remove MonitorEnter/Astore
            list.remove(index--);
            // Replace DupStore with FastSynchronized
            list.set(index, fastSynchronized);
        }
    }
}
