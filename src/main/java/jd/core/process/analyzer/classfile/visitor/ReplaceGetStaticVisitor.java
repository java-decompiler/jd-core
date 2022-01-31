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

public class ReplaceGetStaticVisitor
{
    private final int index;
    private final Instruction newInstruction;
    private Instruction parentFound;

    public ReplaceGetStaticVisitor(int index, Instruction newInstruction)
    {
        this.index = index;
        this.newInstruction = newInstruction;
        this.parentFound = null;
    }

    public void visit(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            {
                ArrayLength al = (ArrayLength)instruction;
                if (match(al, al.getArrayref())) {
                    al.setArrayref(this.newInstruction);
                } else {
                    visit(al.getArrayref());
                }
            }
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                if (match(asi, asi.getArrayref()))
                {
                    asi.setArrayref(this.newInstruction);
                }
                else
                {
                    visit(asi.getArrayref());

                    if (this.parentFound == null)
                    {
                        if (match(asi, asi.getIndexref()))
                        {
                            asi.setIndexref(this.newInstruction);
                        }
                        else
                        {
                            visit(asi.getIndexref());

                            if (this.parentFound == null)
                            {
                                if (match(asi, asi.getValueref())) {
                                    asi.setValueref(this.newInstruction);
                                } else {
                                    visit(asi.getValueref());
                                }
                            }
                        }
                    }
                }
            }
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                if (match(ai, ai.getTest()))
                {
                    ai.setTest(this.newInstruction);
                }
                else
                {
                    visit(ai.getTest());

                    if (this.parentFound == null && ai.getMsg() != null)
                    {
                        if (match(ai, ai.getMsg())) {
                            ai.setMsg(this.newInstruction);
                        } else {
                            visit(ai.getMsg());
                        }
                    }
                }
            }
            break;
        case Const.ATHROW:
            {
                AThrow aThrow = (AThrow)instruction;
                if (match(aThrow, aThrow.getValue())) {
                    aThrow.setValue(this.newInstruction);
                } else {
                    visit(aThrow.getValue());
                }
            }
            break;
        case ByteCodeConstants.UNARYOP:
            {
                UnaryOperatorInstruction uoi = (UnaryOperatorInstruction)instruction;
                if (match(uoi, uoi.getValue())) {
                    uoi.setValue(this.newInstruction);
                } else {
                    visit(uoi.getValue());
                }
            }
            break;
        case ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
                if (match(boi, boi.getValue1()))
                {
                    boi.setValue1(this.newInstruction);
                }
                else
                {
                    visit(boi.getValue1());

                    if (this.parentFound == null)
                    {
                        if (match(boi, boi.getValue2())) {
                            boi.setValue2(this.newInstruction);
                        } else {
                            visit(boi.getValue2());
                        }
                    }
                }
            }
            break;
        case Const.CHECKCAST:
            {
                CheckCast checkCast = (CheckCast)instruction;
                if (match(checkCast, checkCast.getObjectref())) {
                    checkCast.setObjectref(this.newInstruction);
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
                if (match(storeInstruction, storeInstruction.getValueref())) {
                    storeInstruction.setValueref(this.newInstruction);
                } else {
                    visit(storeInstruction.getValueref());
                }
            }
            break;
        case ByteCodeConstants.DUPSTORE:
            {
                DupStore dupStore = (DupStore)instruction;
                if (match(dupStore, dupStore.getObjectref())) {
                    dupStore.setObjectref(this.newInstruction);
                } else {
                    visit(dupStore.getObjectref());
                }
            }
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            {
                ConvertInstruction ci = (ConvertInstruction)instruction;
                if (match(ci, ci.getValue())) {
                    ci.setValue(this.newInstruction);
                } else {
                    visit(ci.getValue());
                }
            }
            break;
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                if (match(ifCmp, ifCmp.getValue1()))
                {
                    ifCmp.setValue1(this.newInstruction);
                }
                else
                {
                    visit(ifCmp.getValue1());

                    if (this.parentFound == null)
                    {
                        if (match(ifCmp, ifCmp.getValue2())) {
                            ifCmp.setValue2(this.newInstruction);
                        } else {
                            visit(ifCmp.getValue2());
                        }
                    }
                }
            }
            break;
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            {
                IfInstruction iff = (IfInstruction)instruction;
                if (match(iff, iff.getValue())) {
                    iff.setValue(this.newInstruction);
                } else {
                    visit(iff.getValue());
                }
            }
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0 && this.parentFound == null; --i)
                {
                    visit(branchList.get(i));
                }
            }
            break;
        case Const.INSTANCEOF:
            {
                InstanceOf instanceOf = (InstanceOf)instruction;
                if (match(instanceOf, instanceOf.getObjectref())) {
                    instanceOf.setObjectref(this.newInstruction);
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
                if (match(insi, insi.getObjectref())) {
                    insi.setObjectref(this.newInstruction);
                } else {
                    visit(insi.getObjectref());
                }
            }
            // intended fall through
        case Const.INVOKESTATIC,
             ByteCodeConstants.INVOKENEW:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                for (int i=list.size()-1; i>=0 && this.parentFound == null; --i)
                {
                    if (match(instruction, list.get(i))) {
                        list.set(i, this.newInstruction);
                    } else {
                        visit(list.get(i));
                    }
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            {
                LookupSwitch ls = (LookupSwitch)instruction;
                if (match(ls, ls.getKey())) {
                    ls.setKey(this.newInstruction);
                } else {
                    visit(ls.getKey());
                }
            }
            break;
        case Const.MONITORENTER:
            {
                MonitorEnter monitorEnter = (MonitorEnter)instruction;
                if (match(monitorEnter, monitorEnter.getObjectref())) {
                    monitorEnter.setObjectref(this.newInstruction);
                } else {
                    visit(monitorEnter.getObjectref());
                }
            }
            break;
        case Const.MONITOREXIT:
            {
                MonitorExit monitorExit = (MonitorExit)instruction;
                if (match(monitorExit, monitorExit.getObjectref())) {
                    monitorExit.setObjectref(this.newInstruction);
                } else {
                    visit(monitorExit.getObjectref());
                }
            }
            break;
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0 && this.parentFound == null; --i)
                {
                    if (match(instruction, dimensions[i])) {
                        dimensions[i] = this.newInstruction;
                    } else {
                        visit(dimensions[i]);
                    }
                }
            }
            break;
        case Const.NEWARRAY:
            {
                NewArray newArray = (NewArray)instruction;
                if (match(newArray, newArray.getDimension())) {
                    newArray.setDimension(this.newInstruction);
                } else {
                    visit(newArray.getDimension());
                }
            }
            break;
        case Const.ANEWARRAY:
            {
                ANewArray aNewArray = (ANewArray)instruction;
                if (match(aNewArray, aNewArray.getDimension())) {
                    aNewArray.setDimension(this.newInstruction);
                } else {
                    visit(aNewArray.getDimension());
                }
            }
            break;
        case Const.POP:
            {
                Pop pop = (Pop)instruction;
                if (match(pop, pop.getObjectref())) {
                    pop.setObjectref(this.newInstruction);
                } else {
                    visit(pop.getObjectref());
                }
            }
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                if (match(putField, putField.getObjectref()))
                {
                    putField.setObjectref(this.newInstruction);
                }
                else
                {
                    visit(putField.getObjectref());

                    if (this.parentFound == null)
                    {
                        if (match(putField, putField.getValueref())) {
                            putField.setValueref(this.newInstruction);
                        } else {
                            visit(putField.getValueref());
                        }
                    }
                }
            }
            break;
        case Const.PUTSTATIC:
            {
                PutStatic putStatic = (PutStatic)instruction;
                if (match(putStatic, putStatic.getValueref())) {
                    putStatic.setValueref(this.newInstruction);
                } else {
                    visit(putStatic.getValueref());
                }
            }
            break;
        case ByteCodeConstants.XRETURN:
            {
                ReturnInstruction ri = (ReturnInstruction)instruction;
                if (match(ri, ri.getValueref())) {
                    ri.setValueref(this.newInstruction);
                } else {
                    visit(ri.getValueref());
                }
            }
            break;
        case Const.TABLESWITCH:
            {
                TableSwitch ts = (TableSwitch)instruction;
                if (match(ts, ts.getKey())) {
                    ts.setKey(this.newInstruction);
                } else {
                    visit(ts.getKey());
                }
            }
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            {
                TernaryOpStore tos = (TernaryOpStore)instruction;
                if (match(tos, tos.getObjectref())) {
                    tos.setObjectref(this.newInstruction);
                } else {
                    visit(tos.getObjectref());
                }
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                if (match(to, to.getTest()))
                {
                    to.setTest(this.newInstruction);
                }
                else
                {
                    visit(to.getTest());

                    if (this.parentFound == null)
                    {
                        if (match(to, to.getValue1()))
                        {
                            to.setValue1(this.newInstruction);
                        }
                        else
                        {
                            visit(to.getValue1());

                            if (this.parentFound == null)
                            {
                                if (match(to, to.getValue2())) {
                                    to.setValue2(this.newInstruction);
                                } else {
                                    visit(to.getValue2());
                                }
                            }
                        }
                    }
                }
            }
            break;
        case ByteCodeConstants.ASSIGNMENT:
            {
                AssignmentInstruction ai = (AssignmentInstruction)instruction;
                if (match(ai, ai.getValue1()))
                {
                    ai.setValue1(this.newInstruction);
                }
                else
                {
                    visit(ai.getValue1());

                    if (this.parentFound == null)
                    {
                        if (match(ai, ai.getValue2())) {
                            ai.setValue2(this.newInstruction);
                        } else {
                            visit(ai.getValue2());
                        }
                    }
                }
            }
            break;
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                if (match(ali, ali.getArrayref()))
                {
                    ali.setArrayref(this.newInstruction);
                }
                else
                {
                    visit(ali.getArrayref());

                    if (this.parentFound == null)
                    {
                        if (match(ali, ali.getIndexref())) {
                            ali.setIndexref(this.newInstruction);
                        } else {
                            visit(ali.getIndexref());
                        }
                    }
                }
            }
            break;
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            {
                IncInstruction ii = (IncInstruction)instruction;
                if (match(ii, ii.getValue())) {
                    ii.setValue(this.newInstruction);
                } else {
                    visit(ii.getValue());
                }
            }
            break;
        case Const.GETFIELD:
            {
                GetField gf = (GetField)instruction;
                if (match(gf, gf.getObjectref())) {
                    gf.setObjectref(this.newInstruction);
                } else {
                    visit(gf.getObjectref());
                }
            }
            break;
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                if (match(iai, iai.getNewArray()))
                {
                    iai.setNewArray(this.newInstruction);
                }
                else
                {
                    visit(iai.getNewArray());

                    if (this.parentFound == null && iai.getValues() != null) {
                        visit(iai.getValues());
                    }
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

    private void visit(List<Instruction> instructions)
    {
        for (int i=instructions.size()-1; i>=0; --i) {
            visit(instructions.get(i));
        }
    }

    /**
     * @return le dernier parent sur lequel une substitution a été faite
     */
    public Instruction getParentFound()
    {
        return this.parentFound;
    }

    private boolean match(Instruction parent, Instruction i)
    {
        if (i.getOpcode() == Const.GETSTATIC &&
            ((GetStatic)i).getIndex() == this.index)
        {
            this.parentFound = parent;
            return true;
        }

        return false;
    }
}
