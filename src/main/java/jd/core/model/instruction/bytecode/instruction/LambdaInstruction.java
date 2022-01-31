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

import org.jd.core.v1.model.javasyntax.type.Type;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;

public class LambdaInstruction extends Instruction {

    private final List<String> parameterNames;
    private final Type returnType;
    private final Method method;
    private final ClassFile classFile;

    public LambdaInstruction(int opcode, int offset, int lineNumber, List<String> parameterNames, Type returnType,  Method method, ClassFile classFile) {
        super(opcode, offset, lineNumber);
        this.parameterNames = parameterNames;
        this.returnType = returnType;
        this.method = method;
        this.classFile = classFile;
    }

    @Override
    public String getReturnedSignature(ConstantPool constants, LocalVariables localVariables) {
        return returnType.getDescriptor();
    }

    public List<Instruction> getInstructions() {
        return method.getFastNodes();
    }
    
    public Method getMethod() {
        return method;
    }
    
    public ClassFile getClassFile() {
        return classFile;
    }
    
    public List<String> getParameterNames() {
        return parameterNames;
    }
}
