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
package jd.core.process.analyzer.classfile.visitor;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
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

/*
 * Replace 'ALoad(1)' in constructor by 'OuterThis()':
 * replace '???.xxx' by 'TestInnerClass.this.xxx'.
 */
public class ReplaceOuterReferenceVisitor
{
    private final int opcode;
    private final int index;
    private final int outerThisInstructionIndex;

    public ReplaceOuterReferenceVisitor(
        int opcode, int index, int outerThisInstructionIndex)
    {
        this.opcode = opcode;
        this.index = index;
        this.outerThisInstructionIndex = outerThisInstructionIndex;
    }

    public void visit(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            {
                ArrayLength al = (ArrayLength)instruction;
                if (match(al.getArrayref())) {
                    al.setArrayref(newInstruction(al.getArrayref()));
                } else {
                    visit(al.getArrayref());
                }
            }
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                if (match(asi.getArrayref())) {
                    asi.setArrayref(newInstruction(asi.getArrayref()));
                } else {
                    visit(asi.getArrayref());
                }
                if (match(asi.getIndexref())) {
                    asi.setIndexref(newInstruction(asi.getIndexref()));
                } else {
                    visit(asi.getIndexref());
                }
                if (match(asi.getValueref())) {
                    asi.setValueref(newInstruction(asi.getValueref()));
                } else {
                    visit(asi.getValueref());
                }
            }
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                if (match(ai.getTest())) {
                    ai.setTest(newInstruction(ai.getTest()));
                } else {
                    visit(ai.getTest());
                }
                if (ai.getMsg() != null)
                {
                    if (match(ai.getMsg())) {
                        ai.setMsg(newInstruction(ai.getMsg()));
                    } else {
                        visit(ai.getMsg());
                    }
                }
            }
            break;
        case Const.ATHROW:
            {
                AThrow aThrow = (AThrow)instruction;
                if (match(aThrow.getValue())) {
                    aThrow.setValue(newInstruction(aThrow.getValue()));
                } else {
                    visit(aThrow.getValue());
                }
            }
            break;
        case ByteCodeConstants.UNARYOP:
            {
                UnaryOperatorInstruction uoi = (UnaryOperatorInstruction)instruction;
                if (match(uoi.getValue())) {
                    uoi.setValue(newInstruction(uoi.getValue()));
                } else {
                    visit(uoi.getValue());
                }
            }
            break;
        case ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
                if (match(boi.getValue1())) {
                    boi.setValue1(newInstruction(boi.getValue1()));
                } else {
                    visit(boi.getValue1());
                }
                if (match(boi.getValue2())) {
                    boi.setValue2(newInstruction(boi.getValue2()));
                } else {
                    visit(boi.getValue2());
                }
            }
            break;
        case Const.CHECKCAST:
            {
                CheckCast checkCast = (CheckCast)instruction;
                if (match(checkCast.getObjectref())) {
                    checkCast.setObjectref(newInstruction(checkCast.getObjectref()));
                } else {
                    visit(checkCast.getObjectref());
                }
            }
            break;
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            {
                StoreInstruction storeInstruction = (StoreInstruction)instruction;
                if (match(storeInstruction.getValueref())) {
                    storeInstruction.setValueref(newInstruction(storeInstruction.getValueref()));
                } else {
                    visit(storeInstruction.getValueref());
                }
            }
            break;
        case ByteCodeConstants.DUPSTORE:
            {
                DupStore dupStore = (DupStore)instruction;
                if (match(dupStore.getObjectref())) {
                    dupStore.setObjectref(newInstruction(dupStore.getObjectref()));
                } else {
                    visit(dupStore.getObjectref());
                }
            }
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            {
                ConvertInstruction ci = (ConvertInstruction)instruction;
                if (match(ci.getValue())) {
                    ci.setValue(newInstruction(ci.getValue()));
                } else {
                    visit(ci.getValue());
                }
            }
            break;
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                if (match(ifCmp.getValue1())) {
                    ifCmp.setValue1(newInstruction(ifCmp.getValue1()));
                } else {
                    visit(ifCmp.getValue1());
                }
                if (match(ifCmp.getValue2())) {
                    ifCmp.setValue2(newInstruction(ifCmp.getValue2()));
                } else {
                    visit(ifCmp.getValue2());
                }
            }
            break;
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            {
                IfInstruction iff = (IfInstruction)instruction;
                if (match(iff.getValue())) {
                    iff.setValue(newInstruction(iff.getValue()));
                } else {
                    visit(iff.getValue());
                }
            }
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i) {
                    visit(branchList.get(i));
                }
            }
            break;
        case Const.INSTANCEOF:
            {
                InstanceOf instanceOf = (InstanceOf)instruction;
                if (match(instanceOf.getObjectref())) {
                    instanceOf.setObjectref(newInstruction(instanceOf.getObjectref()));
                } else {
                    visit(instanceOf.getObjectref());
                }
            }
            break;
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            {
                InvokeNoStaticInstruction insi =
                    (InvokeNoStaticInstruction)instruction;
                if (match(insi.getObjectref())) {
                    insi.setObjectref(newInstruction(insi.getObjectref()));
                } else {
                    visit(insi.getObjectref());
                }
            }
            // intended fall through
        case Const.INVOKESTATIC,
             ByteCodeConstants.INVOKENEW:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                {
                    if (match(list.get(i))) {
                        list.set(i, newInstruction(list.get(i)));
                    } else {
                        visit(list.get(i));
                    }
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            {
                LookupSwitch ls = (LookupSwitch)instruction;
                if (match(ls.getKey())) {
                    ls.setKey(newInstruction(ls.getKey()));
                } else {
                    visit(ls.getKey());
                }
            }
            break;
        case Const.MONITORENTER:
            {
                MonitorEnter monitorEnter = (MonitorEnter)instruction;
                if (match(monitorEnter.getObjectref())) {
                    monitorEnter.setObjectref(newInstruction(monitorEnter.getObjectref()));
                } else {
                    visit(monitorEnter.getObjectref());
                }
            }
            break;
        case Const.MONITOREXIT:
            {
                MonitorExit monitorExit = (MonitorExit)instruction;
                if (match(monitorExit.getObjectref())) {
                    monitorExit.setObjectref(newInstruction(monitorExit.getObjectref()));
                } else {
                    visit(monitorExit.getObjectref());
                }
            }
            break;
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i)
                {
                    if (match(dimensions[i])) {
                        dimensions[i] = newInstruction(dimensions[i]);
                    } else {
                        visit(dimensions[i]);
                    }
                }
            }
            break;
        case Const.NEWARRAY:
            {
                NewArray newArray = (NewArray)instruction;
                if (match(newArray.getDimension())) {
                    newArray.setDimension(newInstruction(newArray.getDimension()));
                } else {
                    visit(newArray.getDimension());
                }
            }
            break;
        case Const.ANEWARRAY:
            {
                ANewArray aNewArray = (ANewArray)instruction;
                if (match(aNewArray.getDimension())) {
                    aNewArray.setDimension(newInstruction(aNewArray.getDimension()));
                } else {
                    visit(aNewArray.getDimension());
                }
            }
            break;
        case Const.POP:
            {
                Pop pop = (Pop)instruction;
                if (match(pop.getObjectref())) {
                    pop.setObjectref(newInstruction(pop.getObjectref()));
                } else {
                    visit(pop.getObjectref());
                }
            }
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                if (match(putField.getObjectref())) {
                    putField.setObjectref(newInstruction(putField.getObjectref()));
                } else {
                    visit(putField.getObjectref());
                }
                if (match(putField.getValueref())) {
                    putField.setValueref(newInstruction(putField.getValueref()));
                } else {
                    visit(putField.getValueref());
                }
            }
            break;
        case Const.PUTSTATIC:
            {
                PutStatic putStatic = (PutStatic)instruction;
                if (match(putStatic.getValueref())) {
                    putStatic.setValueref(newInstruction(putStatic.getValueref()));
                } else {
                    visit(putStatic.getValueref());
                }
            }
            break;
        case ByteCodeConstants.XRETURN:
            {
                ReturnInstruction ri = (ReturnInstruction)instruction;
                if (match(ri.getValueref())) {
                    ri.setValueref(newInstruction(ri.getValueref()));
                } else {
                    visit(ri.getValueref());
                }
            }
            break;
        case Const.TABLESWITCH:
            {
                TableSwitch ts = (TableSwitch)instruction;
                if (match(ts.getKey())) {
                    ts.setKey(newInstruction(ts.getKey()));
                } else {
                    visit(ts.getKey());
                }
            }
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            {
                TernaryOpStore tos = (TernaryOpStore)instruction;
                if (match(tos.getObjectref())) {
                    tos.setObjectref(newInstruction(tos.getObjectref()));
                } else {
                    visit(tos.getObjectref());
                }
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                if (match(to.getTest())) {
                    to.setTest(newInstruction(to.getTest()));
                } else {
                    visit(to.getTest());
                }
                if (match(to.getValue1())) {
                    to.setValue1(newInstruction(to.getValue1()));
                } else {
                    visit(to.getValue1());
                }
                if (match(to.getValue2())) {
                    to.setValue2(newInstruction(to.getValue2()));
                } else {
                    visit(to.getValue2());
                }
            }
            break;
        case ByteCodeConstants.ASSIGNMENT:
            {
                AssignmentInstruction ai = (AssignmentInstruction)instruction;
                if (match(ai.getValue1())) {
                    ai.setValue1(newInstruction(ai.getValue1()));
                } else {
                    visit(ai.getValue1());
                }
                if (match(ai.getValue2())) {
                    ai.setValue2(newInstruction(ai.getValue2()));
                } else {
                    visit(ai.getValue2());
                }
            }
            break;
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                if (match(ali.getArrayref())) {
                    ali.setArrayref(newInstruction(ali.getArrayref()));
                } else {
                    visit(ali.getArrayref());
                }
                if (match(ali.getIndexref())) {
                    ali.setIndexref(newInstruction(ali.getIndexref()));
                } else {
                    visit(ali.getIndexref());
                }
            }
            break;
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            {
                IncInstruction ii = (IncInstruction)instruction;
                if (match(ii.getValue())) {
                    ii.setValue(newInstruction(ii.getValue()));
                } else {
                    visit(ii.getValue());
                }
            }
            break;
        case Const.GETFIELD:
            {
                GetField gf = (GetField)instruction;
                if (match(gf.getObjectref())) {
                    gf.setObjectref(newInstruction(gf.getObjectref()));
                } else {
                    visit(gf.getObjectref());
                }
            }
            break;
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                if (match(iai.getNewArray())) {
                    iai.setNewArray(newInstruction(iai.getNewArray()));
                } else {
                    visit(iai.getNewArray());
                }
                if (iai.getValues() != null) {
                    visit(iai.getValues());
                }
            }
            break;
        case Const.ACONST_NULL,
             ByteCodeConstants.LOAD,
             Const.ALOAD,
             Const.ILOAD,
             Const.BIPUSH,
             ByteCodeConstants.ICONST,
             ByteCodeConstants.LCONST,
             ByteCodeConstants.FCONST,
             ByteCodeConstants.DCONST,
             ByteCodeConstants.DUPLOAD,
             Const.GETSTATIC,
             ByteCodeConstants.OUTERTHIS,
             Const.GOTO,
             Const.IINC,
             Const.JSR,
             Const.LDC,
             Const.LDC2_W,
             Const.NEW,
             Const.NOP,
             Const.SIPUSH,
             Const.RET,
             Const.RETURN,
             Const.INVOKEDYNAMIC,
             ByteCodeConstants.EXCEPTIONLOAD,
             ByteCodeConstants.RETURNADDRESSLOAD:
            break;
        default:
            System.err.println(
                    "Can not replace DupLoad in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }
    }

    public void visit(List<Instruction> instructions)
    {
        for (int idx=instructions.size()-1; idx>=0; --idx)
        {
            Instruction i = instructions.get(idx);

            if (match(i)) {
                instructions.set(idx, newInstruction(i));
            } else {
                visit(i);
            }
        }
    }

    private boolean match(Instruction i)
    {
        return
            i.getOpcode() == this.opcode &&
            ((IndexInstruction)i).getIndex() == this.index;
    }

    private Instruction newInstruction(Instruction i)
    {
        return new GetStatic(
            ByteCodeConstants.OUTERTHIS, i.getOffset(),
            i.getLineNumber(), this.outerThisInstructionIndex);
    }
}
