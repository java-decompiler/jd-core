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

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.process.analyzer.classfile.visitor.CompareInstructionVisitor;

/*
 * Recontruction des operateurs d'assignation depuis les motifs :
 * 1) Operation sur les attributs de classes:
 *    PutStatic(BinaryOperator(GetStatic(), ...))
 * 2) Operation sur les attributs d'instance:
 *    PutField(objectref, BinaryOperator(GetField(objectref), ...))
 * 3) Operation sur les variables locales:
 *    Store(BinaryOperator(Load(), ...))
 * 4) Operation sur les variables locales:
 *    IStore(BinaryOperator(ILoad(), ...))
 * 5) Operation sur des tableaux:
 *    ArrayStore(arrayref, indexref,
 *               BinaryOperator(ArrayLoad(arrayref, indexref), ...))
 */
public final class AssignmentOperatorReconstructor
{
    private AssignmentOperatorReconstructor() {
        super();
    }

    public static void reconstruct(List<Instruction> list)
    {
        int index = list.size();

        while (index-- > 0)
        {
            Instruction i = list.get(index);

            switch (i.getOpcode())
            {
            case Const.PUTSTATIC:
                if (((PutStatic)i).getValueref().getOpcode() ==
                        ByteCodeConstants.BINARYOP) {
                    reconstructPutStaticOperator(list, index, i);
                }
                break;
            case Const.PUTFIELD:
                if (((PutField)i).getValueref().getOpcode() ==
                        ByteCodeConstants.BINARYOP) {
                    index = reconstructPutFieldOperator(list, index, i);
                }
                break;
            case Const.ISTORE:
                if (((StoreInstruction)i).getValueref().getOpcode() ==
                        ByteCodeConstants.BINARYOP)
                {
                    BinaryOperatorInstruction boi = (BinaryOperatorInstruction)
                        ((StoreInstruction)i).getValueref();
                    if (boi.getValue1().getOpcode() == Const.ILOAD) {
                        reconstructStoreOperator(list, index, i, boi);
                    }
                }
                break;
            case ByteCodeConstants.STORE:
                if (((StoreInstruction)i).getValueref().getOpcode() ==
                        ByteCodeConstants.BINARYOP)
                {
                    BinaryOperatorInstruction boi = (BinaryOperatorInstruction)
                        ((StoreInstruction)i).getValueref();
                    if (boi.getValue1().getOpcode() == ByteCodeConstants.LOAD) {
                        reconstructStoreOperator(list, index, i, boi);
                    }
                }
                break;
            case ByteCodeConstants.ARRAYSTORE:
                if (((ArrayStoreInstruction)i).getValueref().getOpcode() ==
                        ByteCodeConstants.BINARYOP) {
                    index = reconstructArrayOperator(list, index, i);
                }
                break;
            }
        }
    }

    /*
     * PutStatic(BinaryOperator(GetStatic(), ...))
     */
    private static void reconstructPutStaticOperator(
        List<Instruction> list, int index, Instruction i)
    {
        PutStatic putStatic = (PutStatic)i;
        BinaryOperatorInstruction boi =
            (BinaryOperatorInstruction)putStatic.getValueref();

        if (boi.getValue1().getOpcode() != Const.GETSTATIC) {
            return;
        }

        GetStatic getStatic = (GetStatic)boi.getValue1();

        if (putStatic.getLineNumber() != getStatic.getLineNumber() ||
            putStatic.getIndex() != getStatic.getIndex()) {
            return;
        }

        String newOperator = boi.getOperator() + "=";

        list.set(index, new AssignmentInstruction(
            ByteCodeConstants.ASSIGNMENT, putStatic.getOffset(),
            getStatic.getLineNumber(), boi.getPriority(), newOperator,
            getStatic, boi.getValue2()));

    }

    /*
     * PutField(objectref, BinaryOperator(GetField(objectref), ...))
     */
    private static int reconstructPutFieldOperator(
        List<Instruction> list, int index, Instruction i)
    {
        PutField putField = (PutField)i;
        BinaryOperatorInstruction boi =
            (BinaryOperatorInstruction)putField.getValueref();

        if (boi.getValue1().getOpcode() != Const.GETFIELD) {
            return index;
        }

        GetField getField = (GetField)boi.getValue1();
        CompareInstructionVisitor visitor = new CompareInstructionVisitor();

        if (putField.getLineNumber() != getField.getLineNumber() ||
            putField.getIndex() != getField.getIndex() ||
            !visitor.visit(putField.getObjectref(), getField.getObjectref())) {
            return index;
        }

        if (putField.getObjectref().getOpcode() == ByteCodeConstants.DUPLOAD)
        {
            // Remove DupStore & DupLoad
            DupLoad dupLoad = (DupLoad)getField.getObjectref();
            index = deleteDupStoreInstruction(list, index, dupLoad);
            getField.setObjectref(dupLoad.getDupStore().getObjectref());
        }

        String newOperator = boi.getOperator() + "=";

        list.set(index, new AssignmentInstruction(
            ByteCodeConstants.ASSIGNMENT, putField.getOffset(),
            getField.getLineNumber(), boi.getPriority(), newOperator,
            getField, boi.getValue2()));

        return index;
    }

    /*
     * StoreInstruction(BinaryOperator(LoadInstruction(), ...))
     */
    private static void reconstructStoreOperator(
        List<Instruction> list, int index,
        Instruction i, BinaryOperatorInstruction boi)
    {
        StoreInstruction si = (StoreInstruction)i;
        LoadInstruction li = (LoadInstruction)boi.getValue1();

        if (si.getLineNumber() != li.getLineNumber() || si.getIndex() != li.getIndex()) {
            return;
        }

        String newOperator = boi.getOperator() + "=";

        list.set(index, new AssignmentInstruction(
            ByteCodeConstants.ASSIGNMENT, si.getOffset(),
            li.getLineNumber(), boi.getPriority(), newOperator,
            li, boi.getValue2()));

    }

    /*
     * ArrayStore(arrayref, indexref,
     *            BinaryOperator(ArrayLoad(arrayref, indexref), ...))
     */
    private static int reconstructArrayOperator(
        List<Instruction> list, int index, Instruction i)
    {
        ArrayStoreInstruction asi = (ArrayStoreInstruction)i;
        BinaryOperatorInstruction boi = (BinaryOperatorInstruction)asi.getValueref();

        if (boi.getValue1().getOpcode() != ByteCodeConstants.ARRAYLOAD) {
            return index;
        }

        ArrayLoadInstruction ali = (ArrayLoadInstruction)boi.getValue1();
        CompareInstructionVisitor visitor = new CompareInstructionVisitor();

        if (asi.getLineNumber() != ali.getLineNumber() ||
            !visitor.visit(asi.getArrayref(), ali.getArrayref()) ||
            !visitor.visit(asi.getIndexref(), ali.getIndexref())) {
            return index;
        }

        if (asi.getArrayref().getOpcode() == ByteCodeConstants.DUPLOAD)
        {
            // Remove DupStore & DupLoad
            DupLoad dupLoad = (DupLoad)ali.getArrayref();
            index = deleteDupStoreInstruction(list, index, dupLoad);
            ali.setArrayref(dupLoad.getDupStore().getObjectref());
        }

        if (asi.getIndexref().getOpcode() == ByteCodeConstants.DUPLOAD)
        {
            // Remove DupStore & DupLoad
            DupLoad dupLoad = (DupLoad)ali.getIndexref();
            index = deleteDupStoreInstruction(list, index, dupLoad);
            ali.setIndexref(dupLoad.getDupStore().getObjectref());
        }

        String newOperator = boi.getOperator() + "=";

        list.set(index, new AssignmentInstruction(
            ByteCodeConstants.ASSIGNMENT, asi.getOffset(),
            ali.getLineNumber(), boi.getPriority(), newOperator,
            ali, boi.getValue2()));

        return index;
    }

    private static int deleteDupStoreInstruction(
        List<Instruction> list, int index, DupLoad dupLoad)
    {
        int indexTmp = index;

        while (indexTmp-- > 0)
        {
            Instruction i = list.get(indexTmp);

            if (dupLoad.getDupStore() == i)
            {
                list.remove(indexTmp);
                return --index;
            }
        }

        return index;
    }
}
