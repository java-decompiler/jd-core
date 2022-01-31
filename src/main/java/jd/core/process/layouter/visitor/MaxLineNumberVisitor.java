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
package jd.core.process.layouter.visitor;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;

public final class MaxLineNumberVisitor
{
    private MaxLineNumberVisitor() {
        super();
    }

    public static int visit(Instruction instruction)
    {
        int maxLineNumber = instruction.getLineNumber();

        switch (instruction.getOpcode())
        {
        case ByteCodeConstants.ARRAYLOAD:
            maxLineNumber = visit(((ArrayLoadInstruction)instruction).getIndexref());
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            maxLineNumber = visit(((ArrayStoreInstruction)instruction).getValueref());
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                maxLineNumber = visit(ai.getMsg() == null ? ai.getTest() : ai.getMsg());
            }
            break;
        case Const.ATHROW:
            maxLineNumber = visit(((AThrow)instruction).getValue());
            break;
        case ByteCodeConstants.UNARYOP:
            maxLineNumber = visit(((UnaryOperatorInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.BINARYOP,
             ByteCodeConstants.ASSIGNMENT:
            maxLineNumber = visit(((BinaryOperatorInstruction)instruction).getValue2());
            break;
        case Const.CHECKCAST:
            maxLineNumber = visit(((CheckCast)instruction).getObjectref());
            break;
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            maxLineNumber = visit(((StoreInstruction)instruction).getValueref());
            break;
        case ByteCodeConstants.DUPSTORE:
            maxLineNumber = visit(((DupStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            maxLineNumber = visit(((ConvertInstruction)instruction).getValue());
            break;
        case FastConstants.DECLARE:
            {
                FastDeclaration fd = (FastDeclaration)instruction;
                if (fd.getInstruction() != null) {
                    maxLineNumber = visit(fd.getInstruction());
                }
            }
            break;
        case ByteCodeConstants.IFCMP:
            maxLineNumber = visit(((IfCmp)instruction).getValue2());
            break;
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            maxLineNumber = visit(((IfInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                maxLineNumber = visit(branchList.get(branchList.size()-1));
            }
            break;
        case Const.INSTANCEOF:
            maxLineNumber = visit(((InstanceOf)instruction).getObjectref());
            break;
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL,
             Const.INVOKESTATIC:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                int length = list.size();

                if (length == 0)
                {
                    maxLineNumber = instruction.getLineNumber();
                }
                else
                {
                    // Correction pour un tres curieux bug : les numéros de
                    // ligne des parametres ne sont pas toujours en ordre croissant
                    maxLineNumber = visit(list.get(0));

                    for (int i=length-1; i>0; i--)
                    {
                        int lineNumber = visit(list.get(i));
                        if (maxLineNumber < lineNumber) {
                            maxLineNumber = lineNumber;
                        }
                    }
                }
            }
            break;
        case ByteCodeConstants.INVOKENEW,
             FastConstants.ENUMVALUE:
            {
                List<Instruction> list = ((InvokeNew)instruction).getArgs();
                int length = list.size();

                if (length == 0)
                {
                    maxLineNumber = instruction.getLineNumber();
                }
                else
                {
                    // Correction pour un tres curieux bug : les numéros de
                    // ligne des parametres ne sont pas toujours en ordre croissant
                    maxLineNumber = visit(list.get(0));

                    for (int i=length-1; i>0; i--)
                    {
                        int lineNumber = visit(list.get(i));
                        if (maxLineNumber < lineNumber) {
                            maxLineNumber = lineNumber;
                        }
                    }
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            maxLineNumber = visit(((LookupSwitch)instruction).getKey());
            break;
        case Const.MONITORENTER:
            maxLineNumber = visit(((MonitorEnter)instruction).getObjectref());
            break;
        case Const.MONITOREXIT:
            maxLineNumber = visit(((MonitorExit)instruction).getObjectref());
            break;
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                int length = dimensions.length;
                if (length > 0) {
                    maxLineNumber = visit(dimensions[length-1]);
                }
            }
            break;
        case Const.NEWARRAY:
            maxLineNumber = visit(((NewArray)instruction).getDimension());
            break;
        case Const.ANEWARRAY:
            maxLineNumber = visit(((ANewArray)instruction).getDimension());
            break;
        case Const.POP:
            maxLineNumber = visit(((Pop)instruction).getObjectref());
            break;
        case Const.PUTFIELD:
            maxLineNumber = visit(((PutField)instruction).getValueref());
            break;
        case Const.PUTSTATIC:
            maxLineNumber = visit(((PutStatic)instruction).getValueref());
            break;
        case ByteCodeConstants.XRETURN:
            maxLineNumber = visit(((ReturnInstruction)instruction).getValueref());
            break;
        case Const.TABLESWITCH:
            maxLineNumber = visit(((TableSwitch)instruction).getKey());
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            maxLineNumber = visit(((TernaryOpStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            IncInstruction ii = (IncInstruction)instruction;
            maxLineNumber = visit(ii.getValue());
            break;
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                int length = iai.getValues().size();
                if (length > 0) {
                    maxLineNumber = visit(iai.getValues().get(length-1));
                }
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            maxLineNumber = visit(((TernaryOperator)instruction).getValue2());
            break;
        }

        // Autre curieux bug : les constantes finales passees en parametres
        // peuvent avoir un numéro de ligne plus petit que le numéro de ligne
        // de l'instruction INVOKE*
        if (maxLineNumber < instruction.getLineNumber())
        {
            return instruction.getLineNumber();
        }
        return maxLineNumber;
    }
}
