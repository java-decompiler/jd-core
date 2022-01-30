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
package jd.core.process.analyzer.instruction.bytecode.util;

import org.apache.bcel.Const;

import java.util.Arrays;

import static jd.core.model.instruction.bytecode.ByteCodeConstants.COMPLEXIF;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.ICONST;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.IF;
import static jd.core.model.instruction.bytecode.ByteCodeConstants.IFXNULL;
import static org.apache.bcel.Const.ACONST_NULL;
import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.ALOAD_0;
import static org.apache.bcel.Const.ALOAD_1;
import static org.apache.bcel.Const.ALOAD_2;
import static org.apache.bcel.Const.ALOAD_3;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.ASTORE_0;
import static org.apache.bcel.Const.ASTORE_1;
import static org.apache.bcel.Const.ASTORE_2;
import static org.apache.bcel.Const.ASTORE_3;
import static org.apache.bcel.Const.BIPUSH;
import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.GETFIELD;
import static org.apache.bcel.Const.GOTO;
import static org.apache.bcel.Const.INVOKEINTERFACE;
import static org.apache.bcel.Const.INVOKESPECIAL;
import static org.apache.bcel.Const.INVOKESTATIC;
import static org.apache.bcel.Const.INVOKEVIRTUAL;
import static org.apache.bcel.Const.SIPUSH;

import jd.core.model.instruction.bytecode.ByteCodeConstants;

public final class ByteCodeUtil
{
    private ByteCodeUtil() {
        super();
    }

    public static int nextTableSwitchOffset(byte[] code, int index)
    {
        // Skip padding
        int i = index+4 & 0xFFFC;

        i += 4;

        final int low =
            (code[i  ] & 255) << 24 | (code[i+1] & 255) << 16 |
            (code[i+2] & 255) << 8 |  code[i+3] & 255;

        i += 4;

        final int high =
            (code[i  ] & 255) << 24 | (code[i+1] & 255) << 16 |
            (code[i+2] & 255) << 8 |  code[i+3] & 255;

        i += 4;
        i += 4 * (high - low + 1);

        return i - 1;
    }

    public static int nextLookupSwitchOffset(byte[] code, int index)
    {
        // Skip padding
        int i = index+4 & 0xFFFC;

        i += 4;

        final int npairs =
            (code[i  ] & 255) << 24 | (code[i+1] & 255) << 16 |
            (code[i+2] & 255) << 8 |  code[i+3] & 255;

        i += 4;
        i += 8*npairs;

        return i - 1;
    }

    public static int nextWideOffset(byte[] code, int index)
    {
        final int opcode = code[index+1] & 255;

        return index + (opcode == Const.IINC ? 5 : 3);
    }

    public static int nextInstructionOffset(byte[] code, int index)
    {
        final int opcode = code[index] & 255;

        switch (opcode)
        {
        case Const.TABLESWITCH:
            return nextTableSwitchOffset(code, index);

        case Const.LOOKUPSWITCH:
            return nextLookupSwitchOffset(code, index);

        case Const.WIDE:
            return nextWideOffset(code, index);

        default:
            return index + 1 + Const.getNoOfOperands(opcode);
        }
    }

    public static boolean jumpTo(byte[] code, int offset, int targetOffset) {
        if (offset != -1) {
            int codeLength = code.length;

            for (int i = 0; i < 10; i++) {
                if (offset == targetOffset) {
                    return true;
                }
                if (offset >= codeLength) {
                    break;
                }

                int opcode = code[offset] & 255;

                if (opcode == Const.GOTO) {
                    offset += (short) ((code[offset + 1] & 255) << 8 | code[offset + 2] & 255);
                } else if (opcode == Const.GOTO_W) {

                    offset += (code[offset + 1] & 255) << 24 | (code[offset + 2] & 255) << 16
                            | (code[offset + 3] & 255) << 8 | code[offset + 4] & 255;
                } else {
                    break;
                }
            }
        }

        return false;
    }


    public static byte[] cleanUpByteCode(byte[] code) {
        /*
         * Matching a bytecode pattern that is probably the result of bytecode
         * manipulation and preventing the decompiler from structuring the source code
         * properly. Getting rid of all that local variable clutter confusing the decompiler.
         */
        for (int i = 0; i < code.length; i++) {
            int offset = i;
            // check if opCode is ALOAD or in ALOAD_0..3 which is the beginning of the pattern
            if (!opCodeIn(code, offset, ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3)) {
                continue;
            }
            if (getOpCode(code, offset) == ALOAD) {
                // skip ALOAD
                offset++;
                if (opCodeIn(code, offset, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3)) {
                    // false start, this is the actual beginning, not the local variable index of an ALOAD
                    continue;
                }
            }
            // skip ALOAD local variable index parameter or skip ALOAD_0..3
            offset++;
            if (offset >= code.length) {
                continue;
            }
            while (offset < code.length && opCodeIn(code, offset, GETFIELD, INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE, CHECKCAST)) {
                // skip GETFIELD, INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE, CHECKCAST and parameters
                offset += 1 + Const.getNoOfOperands(getOpCode(code, offset));
            }
            if (offset >= code.length || !opCodeIn(code, offset, ASTORE, ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3)) {
                continue;
            }
            final int paramEndIdx = offset;
            final int astore1stIdxParam;
            if (getOpCode(code, offset) == ASTORE) {
                // skip ASTORE
                offset++;
                // store ASTORE parameter
                astore1stIdxParam = code[offset];
            } else {
                // store ASTORE_0..3 parameter
                astore1stIdxParam = code[offset] - ASTORE_0;
            }
            // skip ASTORE parameter or skip ALOAD_0..3
            offset++;
            if (offset >= code.length || getOpCode(code, offset) != ACONST_NULL) {
                continue;
            }
            offset++;
            if (offset >= code.length || !opCodeIn(code, offset, ASTORE, ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3)) {
                continue;
            }
            offset += 1 + Const.getNoOfOperands(getOpCode(code, offset));
            if (offset >= code.length || !opCodeIn(code, offset, ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3)) {
                continue;
            }
            if (getOpCode(code, offset) == ALOAD) {
                // skip ALOAD
                offset++;
                // check ALOAD local variable index parameter matches that of ASTORE
                if (astore1stIdxParam != code[offset]) {
                    continue;
                }
            } else // check ALOAD local variable index parameter matches that of ASTORE
            if (astore1stIdxParam != code[offset] - ALOAD_0) {
                continue;
            }
            // skip ALOAD parameter or skip ALOAD_0..3
            offset++;
            if (offset >= code.length || getOpCode(code, offset) != INVOKEVIRTUAL) {
                continue;
            }
            final int invokeVirtualIdx = offset;
            // skip INVOKEVIRTUAL and parameters
            offset += 3;
            if (!opCodeIn(code, offset, ASTORE, ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3)) {
                continue;
            }
            offset += 1 + Const.getNoOfOperands(getOpCode(code, offset));
            if (offset >= code.length || !opCodeIn(code, offset, ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3)) {
                continue;
            }
            if (getOpCode(code, offset) == ALOAD) {
                // skip ALOAD
                offset++;
                // check ALOAD local variable index parameter matches that of ASTORE
                if (astore1stIdxParam != code[offset]) {
                    continue;
                }
            } else // check ALOAD local variable index parameter matches that of ASTORE
            if (astore1stIdxParam != code[offset] - ALOAD_0) {
                continue;
            }
            // skip ALOAD parameter or skip ALOAD_0..3
            offset++;
            if (offset >= code.length) {
                continue;
            }
            if (!opCodeIn(code, offset, ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3)) {
                continue;
            }
            if (getOpCode(code, offset) == ALOAD) {
                // skip ALOAD
                offset++;
                // check ALOAD local variable index parameter matches that of ASTORE
                if (astore1stIdxParam + 2 != code[offset]) {
                    continue;
                }
            } else // check ALOAD local variable index parameter matches that of ASTORE
            if (astore1stIdxParam + 2 != code[offset] - ALOAD_0) {
                continue;
            }
            // skip ALOAD parameter or skip ALOAD_0..3
            offset++;
            if (offset >= code.length) {
                continue;
            }
            if (getOpCode(code, offset) != INVOKESTATIC) {
                continue;
            }
            final int invokeStaticIdx = offset;
            // skip INVOKESTATIC and parameters
            offset += 3;
            if (!opCodeIn(code, offset, ASTORE, ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3)) {
                continue;
            }
            offset += 1 + Const.getNoOfOperands(getOpCode(code, offset));
            if (offset >= code.length) {
                continue;
            }
            if (!opCodeIn(code, offset, ALOAD, ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3)) {
                continue;
            }
            if (getOpCode(code, offset) == ALOAD) {
                // skip ALOAD
                offset++;
                // check ALOAD local variable index parameter matches that of ASTORE
                if (astore1stIdxParam + 1 != code[offset]) {
                    continue;
                }
            } else // check ALOAD local variable index parameter matches that of ASTORE
            if (astore1stIdxParam + 1 != code[offset] - ALOAD_0) {
                continue;
            }
            // skip ALOAD parameter or skip ALOAD_0..3
            offset++;
            // at this point, pattern is fully matched
            int paramLength = paramEndIdx - i;
            int newParamEndIdx = paramEndIdx + paramLength;
            int newInvokeVirtualIdx = offset - 6;
            int newInvokeStaticIdx = offset - 3;
            // check available space before applying changes
            boolean canCopy = newInvokeVirtualIdx >= newParamEndIdx;
            int clearFromIdx;
            if (canCopy) {
                clearFromIdx = newParamEndIdx;
            } else {
                int astoreIdx = paramEndIdx;
                // subtract 33 for ASTORE to ALOAD conversion
                byte aloadCode = (byte) (code[astoreIdx] - 33);
                if (getOpCode(code, astoreIdx) == ASTORE) {
                    // rarest case
                    // skip ASTORE and parameter
                    // copy ALOAD and parameter twice
                    // for following invocations
                    code[astoreIdx + 2] = aloadCode;
                    code[astoreIdx + 3] = code[astoreIdx + 1];
                    code[astoreIdx + 4] = code[astoreIdx + 2];
                    code[astoreIdx + 5] = code[astoreIdx + 3];
                    clearFromIdx = astoreIdx + 6;
                } else {
                    // copy ALOAD code twice for following invocations
                    code[astoreIdx + 1] = aloadCode;
                    code[astoreIdx + 2] = code[astoreIdx + 1];
                    clearFromIdx = astoreIdx + 3;
                }
            }
            // move invoke virtual/static down to make space to copy parameters
            System.arraycopy(code, invokeStaticIdx, code, newInvokeStaticIdx, 3);
            System.arraycopy(code, invokeVirtualIdx, code, newInvokeVirtualIdx, 3);
            // copy parameters
            if (canCopy) {
                System.arraycopy(code, i, code, paramEndIdx, paramLength);
            }
            // clear what's left in the middle
            Arrays.fill(code, clearFromIdx, newInvokeVirtualIdx, (byte)Const.NOP);
        }

        return code;
    }

    public static boolean opCodeIn(byte[] code, int index, int... values) {
        return Arrays.binarySearch(values, getOpCode(code, index)) >= 0;
    }

    public static int getOpCode(byte[] code, int index) {
        return code[index] & 255;
    }

    public static boolean isLoadIntValue(int opcode) {
        return opcode == ICONST || opcode == BIPUSH || opcode == SIPUSH;
    }

    public static boolean isIfInstruction(int opcode, boolean includeComplex) {
        return opcode >= IF && opcode <= IFXNULL || includeComplex && opcode == COMPLEXIF;
    }

    public static boolean isIfOrGotoInstruction(int opcode, boolean includeComplex) {
        return isIfInstruction(opcode, includeComplex) || opcode == GOTO;
    }

    public static int getCmpPriority(int cmp) {
        return cmp == ByteCodeConstants.CMP_EQ || cmp == ByteCodeConstants.CMP_NE ? 7 : 6;
    }

    public static boolean getArrayRefIndex(byte[] code) {
        return code.length == 5 
                && (code[0] & 255) == Const.ILOAD_0 
                && (code[1] & 255) == Const.ANEWARRAY
                && (code[4] & 255) == Const.ARETURN;
    }
}
