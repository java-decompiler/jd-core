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
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LambdaInstruction;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.Switch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;

public abstract class BaseInstructionSplitterVisitor
{
    protected ClassFile classFile;
    private ConstantPool constants;

    protected BaseInstructionSplitterVisitor() {}

    public void start(ClassFile classFile)
    {
        this.classFile = classFile;
        this.constants = classFile == null ? null : classFile.getConstantPool();
    }

    public void visit(Instruction instruction)
    {
        visit(null, instruction);
    }

    protected void visit(Instruction parent, Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            visit(instruction, ((ArrayLength)instruction).getArrayref());
            break;
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                visit(instruction, ali.getArrayref());
                visit(instruction, ali.getIndexref());
            }
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                visit(instruction, asi.getArrayref());
                visit(instruction, asi.getIndexref());
                visit(instruction, asi.getValueref());
            }
            break;
        case Const.ANEWARRAY:
            visit(instruction, ((ANewArray)instruction).getDimension());
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                visit(instruction, ai.getTest());
                if (ai.getMsg() != null) {
                    visit(instruction, ai.getMsg());
                }
            }
            break;
        case ByteCodeConstants.ASSIGNMENT,
             ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi =
                    (BinaryOperatorInstruction)instruction;
                visit(instruction, boi.getValue1());
                visit(instruction, boi.getValue2());
            }
            break;
        case Const.ATHROW:
            visit(instruction, ((AThrow)instruction).getValue());
            break;
        case ByteCodeConstants.UNARYOP:
            visit(instruction, ((UnaryOperatorInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            visit(instruction, ((ConvertInstruction)instruction).getValue());
            break;
        case Const.CHECKCAST:
            visit(instruction, ((CheckCast)instruction).getObjectref());
            break;
        case FastConstants.DECLARE:
            {
                FastDeclaration fd = (FastDeclaration)instruction;
                if (fd.getInstruction() != null) {
                    visit(instruction, fd.getInstruction());
                }
            }
            break;
        case Const.GETFIELD:
            visit(instruction, ((GetField)instruction).getObjectref());
            break;
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            visit(instruction, ((IfInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ic = (IfCmp)instruction;
                visit(instruction, ic.getValue1());
                visit(instruction, ic.getValue2());
            }
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                int length = branchList.size();
                for (int i=0; i<length; i++) {
                    visit(instruction, branchList.get(i));
                }
            }
            break;
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            visit(instruction, ((IncInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.INVOKENEW:
            {
                InvokeNew in = (InvokeNew)instruction;
                List<Instruction> args = in.getArgs();
                int length = args.size();
                for (int i=0; i<length; i++) {
                    visit(instruction, args.get(i));
                }

                ConstantMethodref cmr =
                    this.constants.getConstantMethodref(in.getIndex());
                String internalClassName =
                    this.constants.getConstantClassName(cmr.getClassIndex());
                String prefix =
                    this.classFile.getThisClassName() +
                    StringConstants.INTERNAL_INNER_SEPARATOR;

                if (internalClassName.startsWith(prefix))
                {
                    ClassFile innerClassFile =
                        this.classFile.getInnerClassFile(internalClassName);

                    if (innerClassFile != null &&
                        innerClassFile.getInternalAnonymousClassName() != null)
                    {
                        // Anonymous new invoke
                        visitAnonymousNewInvoke(
                            parent==null ? in : parent, in, innerClassFile);
                    }
                    //else
                    //{
                        // Inner class new invoke
                    //}
                }
                //else
                //{
                    // Normal new invoke
                //}
            }
            break;
        case Const.INVOKEDYNAMIC:
            if (instruction instanceof LambdaInstruction) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                LambdaInstruction in = (LambdaInstruction) instruction;
                // Anonymous lambda
                visitAnonymousLambda(parent==null ? in : parent, in);
            }
            break;
        case Const.INSTANCEOF:
            visit(instruction, ((InstanceOf)instruction).getObjectref());
            break;
        case Const.INVOKEINTERFACE,
             Const.INVOKEVIRTUAL,
             Const.INVOKESPECIAL:
            visit(instruction, ((InvokeNoStaticInstruction)instruction).getObjectref());
            // intended fall through
        case Const.INVOKESTATIC:
            {
                List<Instruction> args = ((InvokeInstruction)instruction).getArgs();
                int length = args.size();
                for (int i=0; i<length; i++) {
                    visit(instruction, args.get(i));
                }
            }
            break;
        case Const.LOOKUPSWITCH,
             Const.TABLESWITCH:
            visit(instruction, ((Switch)instruction).getKey());
            break;
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions =
                    ((MultiANewArray)instruction).getDimensions();
                int length = dimensions.length;
                for (int i=0; i<length; i++) {
                    visit(instruction, dimensions[i]);
                }
            }
            break;
        case Const.NEWARRAY:
            visit(instruction, ((NewArray)instruction).getDimension());
            break;
        case Const.POP:
            visit(instruction, ((Pop)instruction).getObjectref());
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                visit(instruction, putField.getObjectref());
                visit(instruction, putField.getValueref());
            }
            break;
        case Const.PUTSTATIC:
            visit(instruction, ((PutStatic)instruction).getValueref());
            break;
        case ByteCodeConstants.XRETURN:
            visit(instruction, ((ReturnInstruction)instruction).getValueref());
            break;
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            visit(instruction, ((StoreInstruction)instruction).getValueref());
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            visit(instruction, ((TernaryOpStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator tp = (TernaryOperator)instruction;
                visit(instruction, tp.getTest());
                visit(instruction, tp.getValue1());
                visit(instruction, tp.getValue2());
            }
            break;
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                visit(instruction, iai.getNewArray());
                List<Instruction> values = iai.getValues();
                int length = values.size();
                for (int i=0; i<length; i++) {
                    visit(instruction, values.get(i));
                }
            }
            break;
        }
    }

    public abstract void visitAnonymousNewInvoke(Instruction parent, InvokeNew in, ClassFile innerClass);
    
    public abstract void visitAnonymousLambda(Instruction parent, LambdaInstruction in);
}
