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
import jd.core.model.instruction.bytecode.instruction.AALoad;
import jd.core.model.instruction.bytecode.instruction.AAStore;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.ILoad;
import jd.core.model.instruction.bytecode.instruction.IStore;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.attribute.ValuerefAttribute;
import jd.core.process.analyzer.classfile.visitor.CompareInstructionVisitor;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;
import jd.core.process.analyzer.classfile.visitor.SearchDupLoadInstructionVisitor;

/*
 * Recontruction des affectations multiples depuis le motif :
 * DupStore( ??? )
 * ...
 * {?Store | PutField | PutStatic}( DupLoad )
 * ...
 * ???( DupLoad )
 * Deux types de reconstruction :
 *  - a = b = c;
 *  - b = c; ...; a = b;
 */
public final class AssignmentInstructionReconstructor
{
    private AssignmentInstructionReconstructor() {
        super();
    }

    public static void reconstruct(List<Instruction> list)
    {
        for (int dupStoreIndex=0; dupStoreIndex<list.size(); dupStoreIndex++)
        {
            if (list.get(dupStoreIndex).getOpcode() != ByteCodeConstants.DUPSTORE) {
                continue;
            }

            // DupStore trouvé
            DupStore dupStore = (DupStore)list.get(dupStoreIndex);

            int length = list.size();

            // Ne pas prendre en compte les instructions DupStore suivie par une
            // instruction AASTORE ou ARRAYSTORE dont l'attribut arrayref pointe
            // vers l'instruction DupStore : ce cas est traité par
            // 'InitArrayInstructionReconstructor'.
            if (dupStoreIndex+1 < length)
            {
                Instruction i = list.get(dupStoreIndex+1);

                // Recherche de ?AStore ( DupLoad, index, value )
                if (i.getOpcode() == Const.AASTORE ||
                    i.getOpcode() == ByteCodeConstants.ARRAYSTORE)
                {
                    i = ((ArrayStoreInstruction)i).getArrayref();
                    if (i.getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((DupLoad)i).getDupStore() == dupStore) {
                        continue;
                    }
                }
            }

            int xstorePutfieldPutstaticIndex = dupStoreIndex;

            while (++xstorePutfieldPutstaticIndex < length)
            {
                Instruction xstorePutfieldPutstatic =
                    list.get(xstorePutfieldPutstaticIndex);
                Instruction dupload1 = null;

                if (xstorePutfieldPutstatic instanceof ValuerefAttribute)
                {   // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                    ValuerefAttribute valRefAttr = (ValuerefAttribute) xstorePutfieldPutstatic;
                    Instruction i = valRefAttr.getValueref();
                    if (i.getOpcode() == ByteCodeConstants.DUPLOAD &&
                        ((DupLoad)i).getDupStore() == dupStore)
                    {
                        // 1er DupLoad trouvé
                        dupload1 = i;
                    }
                } else if (xstorePutfieldPutstatic.getOpcode() == Const.DSTORE
                        || xstorePutfieldPutstatic.getOpcode() == Const.FSTORE
                        || xstorePutfieldPutstatic.getOpcode() == Const.LSTORE)
                {
                    new RuntimeException("Unexpected instruction")
                                .printStackTrace();
                }

                if (dupload1 == null) {
                    continue;
                }

                // Recherche du 2eme DupLoad
                Instruction dupload2 = null;
                int dupload2Index = xstorePutfieldPutstaticIndex;

                while (++dupload2Index < length)
                {
                    dupload2 = SearchDupLoadInstructionVisitor.visit(
                        list.get(dupload2Index), dupStore);
                    if (dupload2 != null) {
                        break;
                    }
                }

                if (dupload2 == null) {
                    continue;
                }

                if (dupload1.getLineNumber() == dupload2.getLineNumber())
                {
                    // Assignation multiple sur une seule ligne : a = b = c;
                    Instruction newInstruction = createAssignmentInstruction(
                        xstorePutfieldPutstatic, dupStore);

                    // Remplacement du 2eme DupLoad
                    ReplaceDupLoadVisitor visitor =
                        new ReplaceDupLoadVisitor(dupStore, newInstruction);
                    visitor.visit(list.get(dupload2Index));

                    // Mise a jour de toutes les instructions TernaryOpStore
                    // pointant vers cette instruction d'assignation.
                    // Explication:
                    //    ternaryOp2ndValueOffset est initialisée avec l'offset de
                    //  la derniere instruction poussant une valeur sur la pile.
                    //  Dans le cas d'une instruction d'assignation contenue
                    //  dans un operateur ternaire, ternaryOp2ndValueOffset est
                    //  initialise sur l'offset de dupstore. Il faut reajuster
                    //  ternaryOp2ndValueOffset a l'offset de AssignmentInstruction,
                    //  pour que TernaryOpReconstructor fonctionne.
                    int j = dupStoreIndex;

                    while (j-- > 0)
                    {
                        if (list.get(j).getOpcode() == ByteCodeConstants.TERNARYOPSTORE)
                        {
                            // TernaryOpStore trouvé
                            TernaryOpStore tos = (TernaryOpStore)list.get(j);
                            if (tos.getTernaryOp2ndValueOffset() == dupStore.getOffset())
                            {
                                tos.setTernaryOp2ndValueOffset(newInstruction.getOffset());
                                break;
                            }
                        }
                    }

                    list.remove(xstorePutfieldPutstaticIndex);
                    list.remove(dupStoreIndex);
                    dupStoreIndex--;
                    length -= 2;
                }
                else
                {
                    // Assignation multiple sur deux lignes : b = c; a = b;

                    // Create new instruction
                    // {?Load | GetField | GetStatic | AALoad | ARRAYLoad }
                    Instruction newInstruction =
                        createInstruction(xstorePutfieldPutstatic);

                    if (newInstruction != null)
                    {
                        // Remplacement du 1er DupLoad
                        ReplaceDupLoadVisitor visitor =
                            new ReplaceDupLoadVisitor(dupStore, dupStore.getObjectref());
                        visitor.visit(xstorePutfieldPutstatic);

                        // Remplacement du 2eme DupLoad
                        visitor.init(dupStore, newInstruction);
                        visitor.visit(list.get(dupload2Index));

                        list.remove(dupStoreIndex);
                        dupStoreIndex--;
                        length--;
                    }
                }
            }
        }
    }

    private static Instruction createInstruction(
        Instruction xstorePutfieldPutstatic)
    {
        switch (xstorePutfieldPutstatic.getOpcode())
        {
        case Const.ASTORE:
            return new ALoad(
                Const.ALOAD,
                xstorePutfieldPutstatic.getOffset(),
                xstorePutfieldPutstatic.getLineNumber(),
                ((AStore)xstorePutfieldPutstatic).getIndex());
        case Const.ISTORE:
            return new ILoad(
                Const.ILOAD,
                xstorePutfieldPutstatic.getOffset(),
                xstorePutfieldPutstatic.getLineNumber(),
                ((IStore)xstorePutfieldPutstatic).getIndex());
        case ByteCodeConstants.STORE:
            return new LoadInstruction(
                ByteCodeConstants.LOAD,
                xstorePutfieldPutstatic.getOffset(),
                xstorePutfieldPutstatic.getLineNumber(),
                ((StoreInstruction)xstorePutfieldPutstatic).getIndex(),
                xstorePutfieldPutstatic.getReturnedSignature(null, null));
        case Const.PUTFIELD:
            return new GetField(
                Const.GETFIELD,
                xstorePutfieldPutstatic.getOffset(),
                xstorePutfieldPutstatic.getLineNumber(),
                ((PutField)xstorePutfieldPutstatic).getIndex(),
                ((PutField)xstorePutfieldPutstatic).getObjectref());
        case Const.PUTSTATIC:
            return new GetStatic(
                Const.GETSTATIC,
                xstorePutfieldPutstatic.getOffset(),
                xstorePutfieldPutstatic.getLineNumber(),
                ((PutStatic)xstorePutfieldPutstatic).getIndex());
        case Const.AASTORE:
            return new AALoad(
                ByteCodeConstants.ARRAYLOAD,
                xstorePutfieldPutstatic.getOffset(),
                xstorePutfieldPutstatic.getLineNumber(),
                ((AAStore)xstorePutfieldPutstatic).getArrayref(),
                ((AAStore)xstorePutfieldPutstatic).getIndexref());
        case ByteCodeConstants.ARRAYSTORE:
            return new ArrayLoadInstruction(
                ByteCodeConstants.ARRAYLOAD,
                xstorePutfieldPutstatic.getOffset(),
                xstorePutfieldPutstatic.getLineNumber(),
                ((ArrayStoreInstruction)xstorePutfieldPutstatic).getArrayref(),
                ((ArrayStoreInstruction)xstorePutfieldPutstatic).getIndexref(),
                ((ArrayStoreInstruction)xstorePutfieldPutstatic).getSignature());
        default:
            return null;
        }
    }

    private static Instruction createAssignmentInstruction(
        Instruction xstorePutfieldPutstatic, DupStore dupStore)
    {
        if (dupStore.getObjectref().getOpcode() == ByteCodeConstants.BINARYOP)
        {
            // Reconstruction de "a = b += c"
            Instruction value1 =
                ((BinaryOperatorInstruction)dupStore.getObjectref()).getValue1();

            if (xstorePutfieldPutstatic.getLineNumber() == value1.getLineNumber())
            {
                switch (xstorePutfieldPutstatic.getOpcode())
                {
                case Const.ASTORE:
                    if (value1.getOpcode() == Const.ALOAD &&
                        ((StoreInstruction)xstorePutfieldPutstatic).getIndex() ==
                            ((LoadInstruction)value1).getIndex()) {
                        return createBinaryOperatorAssignmentInstruction(
                            xstorePutfieldPutstatic, dupStore);
                    }
                    break;
                case Const.ISTORE:
                    if (value1.getOpcode() == Const.ILOAD &&
                        ((StoreInstruction)xstorePutfieldPutstatic).getIndex() ==
                            ((LoadInstruction)value1).getIndex()) {
                        return createBinaryOperatorAssignmentInstruction(
                            xstorePutfieldPutstatic, dupStore);
                    }
                    break;
                case ByteCodeConstants.STORE:
                    if (value1.getOpcode() == ByteCodeConstants.LOAD &&
                        ((StoreInstruction)xstorePutfieldPutstatic).getIndex() ==
                            ((LoadInstruction)value1).getIndex()) {
                        return createBinaryOperatorAssignmentInstruction(
                            xstorePutfieldPutstatic, dupStore);
                    }
                    break;
                case Const.PUTFIELD:
                    if (value1.getOpcode() == Const.GETFIELD &&
                        ((PutField)xstorePutfieldPutstatic).getIndex() ==
                            ((GetField)value1).getIndex())
                    {
                        CompareInstructionVisitor visitor =
                            new CompareInstructionVisitor();

                        if (visitor.visit(
                                ((PutField)xstorePutfieldPutstatic).getObjectref(),
                                ((GetField)value1).getObjectref())) {
                            return createBinaryOperatorAssignmentInstruction(
                                xstorePutfieldPutstatic, dupStore);
                        }
                    }
                    break;
                case Const.PUTSTATIC:
                    if (value1.getOpcode() == Const.GETFIELD &&
                        ((PutStatic)xstorePutfieldPutstatic).getIndex() ==
                            ((GetStatic)value1).getIndex()) {
                        return createBinaryOperatorAssignmentInstruction(
                            xstorePutfieldPutstatic, dupStore);
                    }
                    break;
                case Const.AASTORE:
                    if (value1.getOpcode() == Const.AALOAD)
                    {
                        ArrayStoreInstruction aas =
                            (ArrayStoreInstruction)xstorePutfieldPutstatic;
                        ArrayLoadInstruction aal =
                            (ArrayLoadInstruction)value1;
                        CompareInstructionVisitor visitor =
                            new CompareInstructionVisitor();

                        if (visitor.visit(
                                aas.getArrayref(), aal.getArrayref()) &&
                            visitor.visit(
                                aas.getIndexref(), aal.getIndexref())) {
                            return createBinaryOperatorAssignmentInstruction(
                                    xstorePutfieldPutstatic, dupStore);
                        }
                    }
                    break;
                case ByteCodeConstants.ARRAYSTORE:
                    if (value1.getOpcode() == ByteCodeConstants.ARRAYLOAD)
                    {
                        ArrayStoreInstruction aas =
                            (ArrayStoreInstruction)xstorePutfieldPutstatic;
                        ArrayLoadInstruction aal =
                            (ArrayLoadInstruction)value1;
                        CompareInstructionVisitor visitor =
                            new CompareInstructionVisitor();

                        if (visitor.visit(
                                aas.getArrayref(), aal.getArrayref()) &&
                            visitor.visit(
                                aas.getIndexref(), aal.getIndexref())) {
                            return createBinaryOperatorAssignmentInstruction(
                                    xstorePutfieldPutstatic, dupStore);
                        }
                    }
                    break;
                case Const.DSTORE,
                     Const.FSTORE,
                     Const.LSTORE:
                    new RuntimeException("Unexpected instruction")
                                .printStackTrace();
                }
            }
        }

        Instruction newInstruction = createInstruction(xstorePutfieldPutstatic);
        return new AssignmentInstruction(
            ByteCodeConstants.ASSIGNMENT, xstorePutfieldPutstatic.getOffset(),
            dupStore.getLineNumber(), 14, "=",
            newInstruction, dupStore.getObjectref());
    }

    private static AssignmentInstruction createBinaryOperatorAssignmentInstruction(
            Instruction xstorePutfieldPutstatic, DupStore dupstore)
    {
        BinaryOperatorInstruction boi =
            (BinaryOperatorInstruction)dupstore.getObjectref();

        String newOperator = boi.getOperator() + "=";

        return new AssignmentInstruction(
                ByteCodeConstants.ASSIGNMENT, xstorePutfieldPutstatic.getOffset(),
                dupstore.getLineNumber(), boi.getPriority(), newOperator,
                createInstruction(xstorePutfieldPutstatic), boi.getValue2());
    }
}
