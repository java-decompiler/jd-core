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
package jd.core.process.analyzer.classfile.reconstructor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.New;
import jd.core.process.analyzer.util.ReconstructorUtil;

/*
 * Recontruction de l'instruction 'new' depuis le motif :
 * DupStore( New(java/lang/Long) )
 * ...
 * Invokespecial(DupLoad, <init>, [ IConst_1 ])
 * ...
 * ??? DupLoad
 */
public final class NewInstructionReconstructor extends NewInstructionReconstructorBase
{
    private NewInstructionReconstructor() {
        super();
    }

    public static void reconstruct(
            ClassFile classFile, Method method, List<Instruction> list)
    {
        for (int dupStoreIndex=0; dupStoreIndex<list.size(); dupStoreIndex++)
        {
            if (list.get(dupStoreIndex).getOpcode() != ByteCodeConstants.DUPSTORE) {
                continue;
            }

            DupStore ds = (DupStore)list.get(dupStoreIndex);

            if (ds.getObjectref().getOpcode() != Const.NEW) {
                continue;
            }

            int invokespecialIndex = dupStoreIndex;
            final int length = list.size();

            while (++invokespecialIndex < length)
            {
                Instruction instruction = list.get(invokespecialIndex);

                if (instruction.getOpcode() != Const.INVOKESPECIAL) {
                    continue;
                }

                Invokespecial is = (Invokespecial)instruction;

                if (is.getObjectref().getOpcode() != ByteCodeConstants.DUPLOAD) {
                    continue;
                }

                DupLoad dl = (DupLoad)is.getObjectref();

                if (dl.getOffset() != ds.getOffset()) {
                    continue;
                }

                ConstantPool constants = classFile.getConstantPool();
                ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());
                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                if (cnat.getNameIndex() == constants.getInstanceConstructorIndex())
                {
                    New nw = (New)ds.getObjectref();
                    InvokeNew invokeNew = new InvokeNew(
                        ByteCodeConstants.INVOKENEW, is.getOffset(),
                        nw.getLineNumber(), is.getIndex(), is.getArgs());

                    Instruction parentFound = ReconstructorUtil.replaceDupLoad(
                        list, invokespecialIndex+1, ds, invokeNew);

                    list.remove(invokespecialIndex);
                    if (parentFound == null) {
                        list.set(dupStoreIndex, invokeNew);
                    } else {
                        list.remove(dupStoreIndex--);
                    }

                    initAnonymousClassConstructorParameterName(
                        classFile, method, invokeNew);
                    break;
                }
            }
        }
    }
}
