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
package jd.core.process.analyzer.instruction.bytecode.factory;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.BootstrapMethod;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.MethodTypes;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.apache.bcel.Const.ACC_PRIVATE;
import static org.apache.bcel.Const.ACC_SYNTHETIC;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.AttributeBootstrapMethods;
import jd.core.model.instruction.bytecode.instruction.ArrayReference;
import jd.core.model.instruction.bytecode.instruction.ConstructorReference;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.model.instruction.bytecode.instruction.LambdaInstruction;
import jd.core.model.instruction.bytecode.instruction.MethodReference;
import jd.core.model.instruction.bytecode.instruction.StaticMethodReference;
import jd.core.process.analyzer.classfile.ClassFileAnalyzer;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;

public class InvokeDynamicFactory implements InstructionFactory
{
    @Override
    public int create(
            ClassFile classFile, Method method, List<Instruction> list,
            List<Instruction> listForAnalyze,
            Deque<Instruction> stack, byte[] code, int offset,
            int lineNumber, boolean[] jumps)
    {
        ConstantPool constants = classFile.getConstantPool();
        // Remove previous 'getClass()' or cast if exists
        if (! listForAnalyze.isEmpty()) {
            Instruction lastInstruction = listForAnalyze.get(listForAnalyze.size() - 1);

            if (lastInstruction instanceof Invokevirtual) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                Invokevirtual mie = (Invokevirtual) lastInstruction;
                ConstantMethodref mieRef = constants.getConstantMethodref(mie.getIndex());
                ConstantNameAndType mieNameAndType = constants.getConstantNameAndType(mieRef.getNameAndTypeIndex());
                String mieName = constants.getConstantUtf8(mieNameAndType.getNameIndex());
                String mieDesc = constants.getConstantUtf8(mieNameAndType.getSignatureIndex());
                if ("getClass".equals(mieName) && "()Ljava/lang/Class;".equals(mieDesc)) {
                    listForAnalyze.remove(listForAnalyze.size() - 1);
                    list.remove(list.size() - 1);
                }
            }
        }

        final int opcode = code[offset] & 255;
        final int index = (code[offset+1] & 255) << 8 | code[offset+2] & 255;
        
        TypeMaker typeMaker = new TypeMaker(classFile.getLoader());

        ConstantMethodref constantMethodref = constants.getConstantMethodref(index);
        ConstantNameAndType indyCnat = constants.getConstantNameAndType(constantMethodref.getNameAndTypeIndex());
//        String indyMethodName = constants.getConstantUtf8(indyCnat.getNameIndex());
        String indyDescriptor = constants.getConstantUtf8(indyCnat.getSignatureIndex());
        
        MethodTypes indyMethodTypes = typeMaker.makeMethodTypes(indyDescriptor);
        AttributeBootstrapMethods attributeBootstrapMethods = classFile.getAttributeBootstrapMethods();
        List<Instruction> indyParameters = extractParametersFromStack(stack, indyMethodTypes.getParameterTypes());
        BootstrapMethod bootstrapMethod = attributeBootstrapMethods.getBootstrapMethod(constantMethodref.getClassIndex());
        int[] bootstrapArguments = bootstrapMethod.getBootstrapArguments();
//        BaseType parameterTypes = indyMethodTypes.getParameterTypes();
        
        ConstantMethodType cmt0 = constants.getConstantMethodType(bootstrapArguments[0]);
        String descriptor0 = constants.getConstantUtf8(cmt0.getDescriptorIndex());
        TypeMaker.MethodTypes methodTypes0 = typeMaker.makeMethodTypes(descriptor0);
        int parameterCount = methodTypes0.getParameterTypes() == null ? 0 : methodTypes0.getParameterTypes().size();
        ConstantMethodHandle constantMethodHandle1 = constants.getConstantMethodHandle(bootstrapArguments[1]);
        
        ConstantMethodref cmr1 = constants.getConstantMethodref(constantMethodHandle1.getReferenceIndex());
        String typeName = constants.getConstantClassName(cmr1.getClassIndex());
        ConstantNameAndType cnat1 = constants.getConstantNameAndType(cmr1.getNameAndTypeIndex());
        String name1 = constants.getConstantUtf8(cnat1.getNameIndex());
        String descriptor1 = constants.getConstantUtf8(cnat1.getSignatureIndex());
        String internalTypeName = classFile.getInternalClassName();
        
        
        boolean arrayRef = false;
        Method lambdaMethod = null;
        if (typeName.equals(internalTypeName.substring(1,  internalTypeName.length() - 1))) {
            for (Method methodDeclaration : classFile.getMethods()) {
                String methodName = constants.getConstantUtf8(methodDeclaration.getNameIndex());
                String methodDesc = constants.getConstantUtf8(methodDeclaration.getDescriptorIndex());
                if ((methodDeclaration.getAccessFlags() & (ACC_SYNTHETIC|ACC_PRIVATE)) == (ACC_SYNTHETIC|ACC_PRIVATE) && methodName.equals(name1) && methodDesc.equals(descriptor1)) {
                    if (ByteCodeUtil.getArrayRefIndex(methodDeclaration.getCode())) {
                        arrayRef = true;
                        break;
                    }
                    lambdaMethod = methodDeclaration;
                }
            }
        }
        
        if (lambdaMethod != null && name1.startsWith("lambda$")) {
            ClassFileAnalyzer.preAnalyzeSingleMethod(classFile, null, -1, lambdaMethod);
            ClassFileAnalyzer.analyzeSingleMethod(null, classFile, null, lambdaMethod);
            LocalVariables localVariables = lambdaMethod.getLocalVariables();
            List<String> localVariableNames = localVariables == null ? Collections.emptyList() : localVariables.getNames(constants);
            ClassFileAnalyzer.postAnalyzeSingleMethod(classFile, lambdaMethod);
            List<String> lambdaParameterNames = prepareLambdaParameterNames(localVariableNames, parameterCount);
            stack.push(new LambdaInstruction(opcode, offset, lineNumber, lambdaParameterNames, indyMethodTypes.getReturnedType(), lambdaMethod, classFile));
            return Const.getNoOfOperands(opcode);

        }
        
        if (indyParameters == null) {
            // Create static method reference
            ObjectType ot = typeMaker.makeFromInternalTypeName(typeName);

            if (arrayRef) {
                stack.push(new ArrayReference(opcode, offset, lineNumber, ot, descriptor1));
            } else if (StringConstants.INSTANCE_CONSTRUCTOR.equals(name1)) {
                stack.push(new ConstructorReference(opcode, offset, lineNumber, ot, descriptor1));
            } else {
                stack.push(new StaticMethodReference(opcode, offset, lineNumber, new ObjectTypeReferenceExpression(lineNumber, ot), typeName, name1, descriptor1));
            }
            
            return Const.getNoOfOperands(opcode);
        }

        // Create method reference
        stack.push(new MethodReference(opcode, offset, lineNumber, indyParameters, typeName, name1, descriptor1));
        
        return Const.getNoOfOperands(opcode);
    }

    private static List<Instruction> extractParametersFromStack(Deque<Instruction> stack, BaseType parameterTypes) {
        if (parameterTypes == null) {
            return null;
        }

        switch (parameterTypes.size()) {
            case 0:
                return null;
            case 1:
                Instruction parameter = stack.pop();
//                if (parameter.isNewArray()) {
//                    parameter = NewArrayMaker.make(statements, parameter);
//                }
                return Collections.singletonList(parameter);
            default:
                List<Instruction> parameters = new ArrayList<>();
                int count = parameterTypes.size() - 1;

                for (int i=count; i>=0; --i) {
                    if (!stack.isEmpty()) {
                        parameter = stack.pop();
//                        if (parameter.isNewArray()) {
//                            parameter = NewArrayMaker.make(statements, parameter);
//                        }
                        parameters.add(parameter);
                    }
                }

                Collections.reverse(parameters);
                return parameters;
        }
    }
    
    private static List<String> prepareLambdaParameterNames(List<String> formalParameters, int parameterCount) {
        if (formalParameters == null || parameterCount == 0) {
            return Collections.emptyList();
        }
        if (formalParameters.size() == parameterCount) {
            return formalParameters;
        }
        return formalParameters.subList(formalParameters.size() - parameterCount, formalParameters.size());
    }

}
