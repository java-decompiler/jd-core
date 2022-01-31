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
package jd.core.model.instruction.fast.instruction;

import java.util.List;
import java.util.Objects;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.instruction.bytecode.instruction.BranchInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;

/** While, do-while & if. */
public class FastSwitch extends BranchInstruction
{
    private Instruction test;
    private final Pair[] pairs;

    public FastSwitch(
        int opcode, int offset, int lineNumber, int branch,
        Instruction test, Pair[] pairs)
    {
        super(opcode, offset, lineNumber, branch);
        this.setTest(test);
        this.pairs = pairs;
    }

    public static class Pair implements Comparable<Pair>
    {
        private final boolean defaultFlag;
        private int key;
        private final int offset;
        private List<Instruction> instructions;

        public Pair(boolean defaultFlag, int key, int offset)
        {
            this.defaultFlag = defaultFlag;
            this.key = key;
            this.offset = offset;
            this.instructions = null;
        }

        public boolean isDefault()
        {
            return defaultFlag;
        }

        public int getKey()
        {
            return key;
        }
        public void setKey(int key)
        {
            this.key = key;
        }

        public int getOffset()
        {
            return offset;
        }

        public List<Instruction> getInstructions()
        {
            return instructions;
        }
        public void setInstructions(List<Instruction> instructions)
        {
            this.instructions = instructions;
        }

        @Override
        public int compareTo(Pair p)
        {
            int diffOffset = this.offset - p.offset;

            if (diffOffset != 0) {
                return diffOffset;
            }

            if (isDefault()) {
                return 1;
            }
            if (p.isDefault()) {
                return -1;
            }
            return this.key - p.key;
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return compareTo((Pair) obj) == 0;
        }
    }

    @Override
    public String getReturnedSignature(
            ConstantPool constants, LocalVariables localVariables)
    {
        return null;
    }

    public Pair[] getPairs() {
        return pairs;
    }

    public Pair getPair(int i) {
        return pairs[i];
    }

    public Instruction getTest() {
        return test;
    }

    public void setTest(Instruction test) {
        this.test = test;
    }
}
