/**
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
 */
package jd.core.process.analyzer.classfile.visitor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
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
import jd.core.util.SignatureUtil;

/**
 * Ajout de 'cast' sur les instructions 'throw', 'astore', 'invokeXXX',
 * 'putfield', 'putstatic' et 'xreturn'.
 */
public class AddCheckCastVisitor
{
    private final ConstantPool constants;
    private final LocalVariables localVariables;
    private final LocalVariable localVariable;

    public AddCheckCastVisitor(
            ConstantPool constants, LocalVariables localVariables,
            LocalVariable localVariable)
    {
        this.constants = constants;
        this.localVariables = localVariables;
        this.localVariable = localVariable;
    }

    public void visit(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            {
                ArrayLength al = (ArrayLength)instruction;
                visit(al.getArrayref());
            }
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                visit(asi.getArrayref());
                visit(asi.getValueref());
            }
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                visit(ai.getTest());
                if (ai.getMsg() != null) {
                    visit(ai.getMsg());
                }
            }
            break;
        case Const.ATHROW:
            {
                AThrow aThrow = (AThrow)instruction;
                if (match(aThrow.getValue()))
                {
                    LoadInstruction li = (LoadInstruction)aThrow.getValue();
                    LocalVariable lv =
                        this.localVariables.getLocalVariableWithIndexAndOffset(
                            li.getIndex(), li.getOffset());

                    if (lv.getSignatureIndex() == this.constants.getObjectSignatureIndex())
                    {
                        // Add Throwable cast
                        int nameIndex = this.constants.addConstantUtf8(StringConstants.JAVA_LANG_THROWABLE);
                        int classIndex =
                            this.constants.addConstantClass(nameIndex);
                        Instruction i = aThrow.getValue();
                        aThrow.setValue(new CheckCast(
                            Const.CHECKCAST, i.getOffset(),
                            i.getLineNumber(), classIndex, i));
                    }
                }
                else
                {
                    visit(aThrow.getValue());
                }
            }
            break;
        case ByteCodeConstants.UNARYOP:
            visit(((UnaryOperatorInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
                visit(boi.getValue1());
                visit(boi.getValue2());
            }
            break;
        case Const.CHECKCAST:
            visit(((CheckCast)instruction).getObjectref());
            break;
        case ByteCodeConstants.STORE,
             Const.ISTORE:
            visit(((StoreInstruction)instruction).getValueref());
            break;
        case Const.ASTORE:
            {
                StoreInstruction storeInstruction = (StoreInstruction)instruction;
                if (match(storeInstruction.getValueref()))
                {
                    LocalVariable lv =
                        this.localVariables.getLocalVariableWithIndexAndOffset(
                            storeInstruction.getIndex(), storeInstruction.getOffset());

                    // AStore est associé à  une variable correctement typée
                    if (lv.getSignatureIndex() > 0 && lv.getSignatureIndex() != this.constants.getObjectSignatureIndex())
                    {
                        String signature =
                            this.constants.getConstantUtf8(lv.getSignatureIndex());
                        storeInstruction.setValueref(newInstruction(
                            signature, storeInstruction.getValueref()));
                    }
                }
                else
                {
                    visit(storeInstruction.getValueref());
                }
            }
            break;
        case ByteCodeConstants.DUPSTORE:
            visit(((DupStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            visit(((ConvertInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                visit(ifCmp.getValue1());
                visit(ifCmp.getValue2());
            }
            break;
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            visit(((IfInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                {
                    visit(branchList.get(i));
                }
            }
            break;
        case Const.INSTANCEOF:
            visit(((InstanceOf)instruction).getObjectref());
            break;
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            {
                InvokeNoStaticInstruction insi =
                    (InvokeNoStaticInstruction)instruction;
                if (match(insi.getObjectref()))
                {
                    ConstantMethodref cmr = this.constants.getConstantMethodref(insi.getIndex());
                    ConstantClass cc = this.constants.getConstantClass(cmr.getClassIndex());

                    if (this.constants.getObjectClassNameIndex() != cc.getNameIndex())
                    {
                        Instruction i = insi.getObjectref();
                        insi.setObjectref(new CheckCast(
                            Const.CHECKCAST, i.getOffset(),
                            i.getLineNumber(), cmr.getClassIndex(), i));
                    }
                }
                else
                {
                    visit(insi.getObjectref());
                }
            }
            // intended fall through
        case Const.INVOKESTATIC:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                List<String> types = ((InvokeInstruction)instruction)
                    .getListOfParameterSignatures(this.constants);

                Instruction arg;
                for (int i=list.size()-1; i>=0; --i)
                {
                    arg = list.get(i);
                    if (match(arg))
                    {
                        String signature = types.get(i);

                        if (! StringConstants.INTERNAL_OBJECT_SIGNATURE.equals(signature))
                        {
                            list.set(i, newInstruction(signature, arg));
                        }
                    }
                    else
                    {
                        visit(arg);
                    }
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            visit(((LookupSwitch)instruction).getKey());
            break;
        case Const.MONITORENTER:
            visit(((MonitorEnter)instruction).getObjectref());
            break;
        case Const.MONITOREXIT:
            visit(((MonitorExit)instruction).getObjectref());
            break;
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i) {
                    visit(dimensions[i]);
                }
            }
            break;
        case Const.NEWARRAY:
            visit(((NewArray)instruction).getDimension());
            break;
        case Const.ANEWARRAY:
            visit(((ANewArray)instruction).getDimension());
            break;
        case Const.POP:
            visit(((Pop)instruction).getObjectref());
            break;
        case Const.GETFIELD:
            {
                GetField getField = (GetField)instruction;
                if (match(getField.getObjectref()))
                {
                    ConstantFieldref cfr =
                        this.constants.getConstantFieldref(getField.getIndex());
                    ConstantClass cc = this.constants.getConstantClass(cfr.getClassIndex());

                    if (this.constants.getObjectClassNameIndex() != cc.getNameIndex())
                    {
                        Instruction i = getField.getObjectref();
                        getField.setObjectref(new CheckCast(
                            Const.CHECKCAST, i.getOffset(),
                            i.getLineNumber(), cfr.getClassIndex(), i));
                    }
                }
                else
                {
                    visit(getField.getObjectref());
                }
            }
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                if (match(putField.getObjectref()))
                {
                    ConstantFieldref cfr =
                        this.constants.getConstantFieldref(putField.getIndex());
                    ConstantClass cc = this.constants.getConstantClass(cfr.getClassIndex());

                    if (this.constants.getObjectClassNameIndex() != cc.getNameIndex())
                    {
                        Instruction i = putField.getObjectref();
                        putField.setObjectref(new CheckCast(
                            Const.CHECKCAST, i.getOffset(),
                            i.getLineNumber(), cfr.getClassIndex(), i));
                    }
                }
                else
                {
                    visit(putField.getObjectref());
                }
                if (match(putField.getValueref()))
                {
                    ConstantFieldref cfr = constants.getConstantFieldref(putField.getIndex());
                    ConstantNameAndType cnat =
                        constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

                    if (cnat.getSignatureIndex() != this.constants.getObjectSignatureIndex())
                    {
                        String signature =
                            this.constants.getConstantUtf8(cnat.getSignatureIndex());
                        putField.setValueref(newInstruction(
                            signature, putField.getValueref()));
                    }
                }
                else
                {
                    visit(putField.getValueref());
                }
            }
            break;
        case Const.PUTSTATIC:
            {
                PutStatic putStatic = (PutStatic)instruction;
                if (match(putStatic.getValueref()))
                {
                    ConstantFieldref cfr = constants.getConstantFieldref(putStatic.getIndex());
                    ConstantNameAndType cnat =
                        constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

                    if (cnat.getSignatureIndex() != this.constants.getObjectSignatureIndex())
                    {
                        String signature =
                            this.constants.getConstantUtf8(cnat.getSignatureIndex());
                        putStatic.setValueref(newInstruction(
                            signature, putStatic.getValueref()));
                    }
                }
                else
                {
                    visit(putStatic.getValueref());
                }
            }
            break;
        case ByteCodeConstants.XRETURN:
            visit(((ReturnInstruction)instruction).getValueref());
            break;
        case Const.TABLESWITCH:
            visit(((TableSwitch)instruction).getKey());
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            visit(((TernaryOpStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                visit(to.getValue1());
                visit(to.getValue2());
            }
            break;
        case Const.ACONST_NULL,
             ByteCodeConstants.ARRAYLOAD,
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
             ByteCodeConstants.INVOKENEW,
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
             ByteCodeConstants.RETURNADDRESSLOAD,
             Const.IINC,
             ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            break;
        default:
            System.err.println(
                    "Can not add cast in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }
    }

    private boolean match(Instruction i)
    {
        if (i.getOpcode() == Const.ALOAD)
        {
            LoadInstruction li = (LoadInstruction)i;
            if (li.getIndex() == this.localVariable.getIndex())
            {
                LocalVariable lv =
                    this.localVariables.getLocalVariableWithIndexAndOffset(
                            li.getIndex(), li.getOffset());
                return lv == this.localVariable;
            }
        }

        return false;
    }

    private Instruction newInstruction(String signature, Instruction i)
    {
        if (SignatureUtil.isPrimitiveSignature(signature))
        {
            return new ConvertInstruction(
                ByteCodeConstants.CONVERT, i.getOffset(),
                i.getLineNumber(), i, signature);
        }
        int nameIndex;
        if (signature.charAt(0) == 'L')
        {
            String name = SignatureUtil.getInnerName(signature);
            nameIndex = this.constants.addConstantUtf8(name);
        }
        else
        {
            nameIndex = this.constants.addConstantUtf8(signature);
        }
        int classIndex =
            this.constants.addConstantClass(nameIndex);
        return new CheckCast(
            Const.CHECKCAST, i.getOffset(),
            i.getLineNumber(), classIndex, i);
    }
}
