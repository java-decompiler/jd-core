/*******************************************************************************
 * Copyright (C) 2022 GPLv3
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
package jd.core.model.instruction.bytecode.instruction;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.printer.Printer;
import jd.core.process.writer.SourceWriteable;
import jd.core.process.writer.visitor.SourceWriterVisitor;

public class MethodReference extends Instruction implements SourceWriteable {

    private final List<Instruction> parameters;
    private final String internalTypeName;
    private final String name;
    private final String descriptor;

    public MethodReference(int opcode, int offset, int lineNumber, List<Instruction> parameters, String internalTypeName, String name, String descriptor) {
        super(opcode, offset, lineNumber);
        this.parameters = parameters;
        this.internalTypeName = internalTypeName;
        this.name = name;
        this.descriptor = descriptor;
    }

    @Override
    public String getReturnedSignature(ConstantPool constants, LocalVariables localVariables) {
        throw new UnsupportedOperationException();
    }

    public String getInternalTypeName() {
        return internalTypeName;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public List<Instruction> getParameters() {
        return parameters;
    }

    @Override
    public void write(Printer printer, SourceWriterVisitor visitor) {
        if (!parameters.isEmpty()) {
            Instruction parameter = parameters.get(0);
            if (parameter instanceof DupLoad) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                DupLoad dupLoad = (DupLoad) parameter;
                visitor.visit(dupLoad.getDupStore().getObjectref());
            }
        }
        printer.print("::");
        printer.print(name);
    }
}
