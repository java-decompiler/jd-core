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
package jd.core.process.analyzer.instruction.bytecode;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.LineNumber;
import org.jd.core.v1.model.classfile.attribute.CodeException;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ExceptionLoad;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.ReturnAddressLoad;
import jd.core.process.analyzer.instruction.bytecode.factory.InstructionFactory;
import jd.core.process.analyzer.instruction.bytecode.factory.InstructionFactoryConstants;
import jd.core.process.analyzer.instruction.bytecode.factory.UnexpectedOpcodeException;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.util.IntSet;
import jd.core.util.SignatureUtil;

public final class InstructionListBuilder
{
    private InstructionListBuilder() {
        super();
    }

    private static final CodeExceptionComparator COMPARATOR =
            new CodeExceptionComparator();

    public static void build(
            ClassFile classFile, Method method,
            List<Instruction> list,
            List<Instruction> listForAnalyze)
    {
        byte[] code = method.getCode();

        if (code != null)
        {
            int offset = 0;

            try
            {
                final int length = code.length;

                // Declaration du tableau de sauts utile pour reconstruire les
                // instructions de pre et post incrementation : si une
                // instruction 'iinc' est une instruction vers laquelle on
                // saute, elle ne sera pas agregée a l'instruction precedante
                // ou suivante.
                boolean[] jumps = new boolean[length];

                // Declaration du tableau des sauts vers les sous procedures
                // (jsr ... ret). A chaque début de sous procedures, une pseudo
                // adresse de retour doit être inseree sur la pile.
                IntSet offsetSet = new IntSet();

                // Population des deux tableaux dans la même passe.
                populateJumpsArrayAndSubProcOffsets(code, length, jumps, offsetSet);

                // Initialisation de variables additionnelles pour le traitement
                // des sous procedures.
                int[] subProcOffsets = offsetSet.toArray();
                int subProcOffsetsIndex = 0;
                int subProcOffset =
                        subProcOffsets == null ? -1 : subProcOffsets[0];

                // Declaration de variables additionnelles pour le traitement
                // des blocs 'catch' et 'finally'.
                final Deque<Instruction> stack = new ArrayDeque<>();
                final CodeException[] codeExceptions = method.getCodeExceptions();
                int codeExceptionsIndex = 0;
                int exceptionOffset;
                ConstantPool constants = classFile.getConstantPool();

                if (codeExceptions == null)
                {
                    exceptionOffset = -1;
                }
                else
                {
                    // Sort codeExceptions by handlerPc
                    Arrays.sort(codeExceptions, COMPARATOR);
                    exceptionOffset = codeExceptions[0].handlerPc();
                }

                // Declaration de variables additionnelles pour le traitement
                // des numéros de ligne
                LineNumber[] lineNumbers = method.getLineNumbers();
                int lineNumbersIndex = 0;
                int lineNumber;
                int nextLineOffset;

                if (lineNumbers == null)
                {
                    lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
                    nextLineOffset = -1;
                }
                else
                {
                    LineNumber ln = lineNumbers[lineNumbersIndex];
                    lineNumber = ln.getLineNumber();
                    nextLineOffset = -1;

                    int startPc = ln.getStartPC();
                    while (++lineNumbersIndex < lineNumbers.length)
                    {
                        ln = lineNumbers[lineNumbersIndex];
                        if (ln.getStartPC() != startPc)
                        {
                            nextLineOffset = ln.getStartPC();
                            break;
                        }
                        lineNumber = ln.getLineNumber();
                    }
                }

                // Boucle principale : agregation des instructions
                for (offset=0; offset<length; ++offset)
                {
                    int opcode = code[offset] & 255;
                    InstructionFactory factory =
                            InstructionFactoryConstants.getInstructionFactory(opcode);

                    if (factory == null) {
                        String msg = "No factory for " +
                                Const.getOpcodeName(opcode);
                        System.err.println(msg);
                        throw new UnexpectedOpcodeException(opcode);
                    }
                    // Ajout de ExceptionLoad
                    if (offset == exceptionOffset && codeExceptions != null)
                    {
                        // Ajout d'une pseudo instruction de lecture
                        // d'exception en début de bloc catch
                        int catchType =
                                codeExceptions[codeExceptionsIndex].catchType();
                        int signatureIndex;

                        if (catchType == 0)
                        {
                            signatureIndex = 0;
                        }
                        else
                        {
                            String catchClassName = SignatureUtil.createTypeName(
                                    constants.getConstantClassName(catchType));
                            signatureIndex = constants.addConstantUtf8(
                                    catchClassName);
                        }

                        ExceptionLoad el = new ExceptionLoad(
                                ByteCodeConstants.EXCEPTIONLOAD,
                                offset, lineNumber, signatureIndex);
                        stack.push(el);
                        listForAnalyze.add(el);

                        // Search next exception offset
                        int nextOffsetException;
                        for (;;)
                        {
                            if (++codeExceptionsIndex >= codeExceptions.length)
                            {
                                nextOffsetException = -1;
                                break;
                            }

                            nextOffsetException =
                                    codeExceptions[codeExceptionsIndex].handlerPc();

                            if (nextOffsetException != exceptionOffset) {
                                break;
                            }
                        }
                        exceptionOffset = nextOffsetException;
                    }

                    // Ajout de ReturnAddressLoad
                    if (offset == subProcOffset)
                    {
                        // Ajout d'une pseudo adresse de retour en début de
                        // sous procedure. Lors de l'execution, cette
                        // adresse est normalement placée sur la pile par
                        // l'instruction JSR.
                        stack.push(new ReturnAddressLoad(
                                ByteCodeConstants.RETURNADDRESSLOAD,
                                offset, lineNumber));

                        if (subProcOffsets != null) {
                            if (++subProcOffsetsIndex >= subProcOffsets.length) {
                                subProcOffset = -1;
                            } else {
                                subProcOffset = subProcOffsets[subProcOffsetsIndex];
                            }
                        }
                    }

                    // Traitement des numéros de ligne
                    if (lineNumbers != null && offset == nextLineOffset)
                    {
                        LineNumber ln = lineNumbers[lineNumbersIndex];
                        lineNumber = ln.getLineNumber();
                        nextLineOffset = -1;

                        int startPc = ln.getStartPC();
                        while (++lineNumbersIndex < lineNumbers.length)
                        {
                            ln = lineNumbers[lineNumbersIndex];
                            if (ln.getStartPC() != startPc)
                            {
                                nextLineOffset = ln.getStartPC();
                                break;
                            }
                            lineNumber = ln.getLineNumber();
                        }
                    }

                    // Generation d'instruction
                    offset += factory.create(
                            classFile, method, list, listForAnalyze, stack,
                            code, offset, lineNumber, jumps);
                }

                if (! stack.isEmpty())
                {
                    final String className = classFile.getClassName();
                    final String methodName =
                            classFile.getConstantPool().getConstantUtf8(method.getNameIndex());
                    System.err.println(
                            "'" + className + '.' + methodName +
                            "' build error: stack not empty. stack=" + stack);
                }
            }
            catch (Exception e)
            {
                // Bad byte code ... generated, for example, by Eclipse Java
                // Compiler or Harmony:
                // Byte code:
                //   0: aload_0
                //   1: invokevirtual 16    TryCatchFinallyClassForTest:before    ()V
                //   4: iconst_1
                //   5: ireturn
                //   6: astore_1       <----- Error: EmptyStackException
                //   7: aload_0
                //   8: invokevirtual 19    TryCatchFinallyClassForTest:inCatch1    ()V
                //   11: aload_0
                //   12: invokevirtual 22    TryCatchFinallyClassForTest:after    ()V
                //   15: iconst_2
                //   16: ireturn
                throw new InstructionListException(classFile, method, offset, e);
            }
        }
    }

    private static void populateJumpsArrayAndSubProcOffsets(
            byte[] code, int length, boolean[] jumps, IntSet offsetSet)
    {
        for (int offset=0; offset<length; ++offset)
        {
            int jumpOffset;
            int opcode = code[offset] & 255;

            switch (Const.getNoOfOperands(opcode))
            {
            case 0:
                break;
            case 2:
                switch (opcode)
                {
                case Const.IFEQ,
                     Const.IFNE,
                     Const.IFLT,
                     Const.IFGE,
                     Const.IFGT,
                     Const.IFLE,

                     Const.IF_ICMPEQ,
                     Const.IF_ICMPNE,
                     Const.IF_ICMPLT,
                     Const.IF_ICMPGE,
                     Const.IF_ICMPGT,
                     Const.IF_ICMPLE,

                     Const.IF_ACMPEQ,
                     Const.IF_ACMPNE,

                     Const.IFNONNULL,
                     Const.IFNULL,

                     Const.GOTO:
                    jumpOffset = offset +
                    (short)( (code[++offset] & 255) << 8 |
                            code[++offset] & 255 );
                    jumps[jumpOffset] = true;
                    break;
                case Const.JSR:
                    jumpOffset = offset +
                    (short)( (code[++offset] & 255) << 8 |
                            code[++offset] & 255 );
                    offsetSet.add(jumpOffset);
                    break;
                default:
                    offset += 2;
                }
                break;

            case 4:
                switch (opcode)
                {
                case Const.GOTO_W:
                    jumpOffset = offset +
                    ((code[++offset] & 255) << 24) |
                    (code[++offset] & 255) << 16 |
                    (code[++offset] & 255) << 8 |
                    code[++offset] & 255;
                    jumps[jumpOffset] = true;
                    break;
                case Const.JSR_W:
                    jumpOffset = offset +
                    ((code[++offset] & 255) << 24) |
                    (code[++offset] & 255) << 16 |
                    (code[++offset] & 255) << 8 |
                    code[++offset] & 255;
                    offsetSet.add(jumpOffset);
                    break;
                default:
                    offset += 4;
                }
                break;
            default:
                offset = switch (opcode) {
                case Const.TABLESWITCH -> ByteCodeUtil.nextTableSwitchOffset(code, offset);
                case Const.LOOKUPSWITCH -> ByteCodeUtil.nextLookupSwitchOffset(code, offset);
                case Const.WIDE -> ByteCodeUtil.nextWideOffset(code, offset);
                default -> offset + Const.getNoOfOperands(opcode);
                };
            }
        }
    }

    private static class CodeExceptionComparator
    implements java.io.Serializable, Comparator<CodeException>
    {
        /**
         * Comparators should be Serializable: A non-serializable Comparator can prevent an otherwise-Serializable ordered collection from being serializable.
         */
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(CodeException ce1, CodeException ce2)
        {
            if (ce1.handlerPc() != ce2.handlerPc()) {
                return ce1.handlerPc() - ce2.handlerPc();
            }

            if (ce1.endPc() != ce2.endPc()) {
                return ce1.endPc() - ce2.endPc();
            }

            if (ce1.startPc() != ce2.startPc()) {
                return ce1.startPc() - ce2.startPc();
            }

            return ce1.index() - ce2.index();
        }
    }
}
