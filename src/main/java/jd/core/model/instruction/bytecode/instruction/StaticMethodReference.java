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

import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.printer.Printer;
import jd.core.process.writer.SourceWriteable;
import jd.core.process.writer.visitor.SourceWriterVisitor;

public class StaticMethodReference extends Instruction implements SourceWriteable {

    private final ObjectTypeReferenceExpression objectType;
    private final String typeName;
    private final String name;
    private final String descriptor;

    public StaticMethodReference(int opcode, int offset, int lineNumber, ObjectTypeReferenceExpression objectType, String typeName, String name, String descriptor) {
        super(opcode, offset, lineNumber);
        this.objectType = objectType;
        this.typeName = typeName;
        this.name = name;
        this.descriptor = descriptor;
    }

    public ObjectTypeReferenceExpression getObjectType() {
        return objectType;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getName() {
        return name;
    }
    
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public String getReturnedSignature(ConstantPool constants, LocalVariables localVariables) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Printer printer, SourceWriterVisitor visitor) {
        printer.print(objectType.getObjectType().getName());
        printer.print("::");
        printer.print(name);
    }
}
