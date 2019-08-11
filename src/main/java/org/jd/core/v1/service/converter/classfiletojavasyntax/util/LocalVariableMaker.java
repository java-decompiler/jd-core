/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Field;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.*;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameters;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFormalParameter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration.*;

public class LocalVariableMaker {
    protected LocalVariableSet localVariableSet = new LocalVariableSet();
    protected HashSet<String> names = new HashSet<>();
    protected HashSet<String> blackListNames = new HashSet<>();
    protected Frame currentFrame = new RootFrame();
    protected AbstractLocalVariable[] localVariableCache;

    protected ObjectTypeMaker objectTypeMaker;
    protected SignatureParser signatureParser;
    protected FormalParameters formalParameters;

    protected PopulateBlackListNamesVisitor populateBlackListNamesVisitor = new PopulateBlackListNamesVisitor(blackListNames);
    protected CreateParameterVisitor createParameterVisitor;
    protected CreateLocalVariableVisitor createLocalVariableVisitor;

    @SuppressWarnings("unchecked")
    public LocalVariableMaker(ObjectTypeMaker objectTypeMaker, SignatureParser signatureParser, ClassFileConstructorOrMethodDeclaration comdwln, boolean constructor, List<Type> parameterTypes) {
        ClassFile classFile = comdwln.getClassFile();
        Method method = comdwln.getMethod();

        this.objectTypeMaker = objectTypeMaker;
        this.signatureParser = signatureParser;
        this.createParameterVisitor = new CreateParameterVisitor(objectTypeMaker);
        this.createLocalVariableVisitor = new CreateLocalVariableVisitor(objectTypeMaker);

        // Initialize local black list variable names
        if (classFile.getFields() != null) {
            for (Field field : classFile.getFields()) {
                String descriptor = field.getDescriptor();

                if (descriptor.charAt(descriptor.length() - 1) == ';') {
                    objectTypeMaker.makeFromDescriptor(descriptor).accept(populateBlackListNamesVisitor);
                }

                blackListNames.add(field.getName());
            }
        }

        objectTypeMaker.makeFromInternalTypeName(classFile.getInternalTypeName()).accept(populateBlackListNamesVisitor);

        if (classFile.getSuperTypeName() != null) {
            objectTypeMaker.makeFromInternalTypeName(classFile.getSuperTypeName()).accept(populateBlackListNamesVisitor);
        }

        if (classFile.getInterfaceTypeNames() != null) {
            for (String interfaceTypeName : classFile.getInterfaceTypeNames()) {
                objectTypeMaker.makeFromInternalTypeName(interfaceTypeName).accept(populateBlackListNamesVisitor);
            }
        }

        if (parameterTypes != null) {
            for (Type type : parameterTypes) {
                type.accept(populateBlackListNamesVisitor);
            }
        }

        // Initialize local variables from 'LocalVariableTable' and 'LocalVariableTypeTable' attributes
        initLocalVariablesFromAttributes(method);

        // Initialize local variables from access flags & signature
        int firstVariableIndex = 0;

        if ((method.getAccessFlags() & FLAG_STATIC) == 0) {
            if (localVariableSet.root(0) == null) {
                // Local variable missing
                localVariableSet.add(0, new ObjectLocalVariable(objectTypeMaker, 0, 0, objectTypeMaker.makeFromInternalTypeName(classFile.getInternalTypeName()), "this"));
                blackListNames.add("this");
            }
            firstVariableIndex = 1;
        }

        if (constructor) {
            if ((classFile.getAccessFlags() & FLAG_ENUM) != 0) {
                if (localVariableSet.root(1) == null) {
                    // Local variable missing
                    localVariableSet.add(1, new ObjectLocalVariable(objectTypeMaker, 1, 0, ObjectType.TYPE_STRING, "this$enum$name"));
                    blackListNames.add("this$enum$name");
                }
                if (localVariableSet.root(2) == null) {
                    // Local variable missing
                    localVariableSet.add(2, new PrimitiveLocalVariable(2, 0, PrimitiveType.TYPE_INT, "this$enum$index"));
                    blackListNames.add("this$enum$index");
                }
            } else if ((classFile.getOuterClassFile() != null) && ((classFile.getAccessFlags() & FLAG_STATIC) == 0)) {
                if (localVariableSet.root(1) == null) {
                    // Local variable missing
                    localVariableSet.add(1, new ObjectLocalVariable(objectTypeMaker, 1, 0, objectTypeMaker.makeFromInternalTypeName(classFile.getOuterClassFile().getInternalTypeName()), "this$0"));
                    blackListNames.add("this$0");
                }
            }
        }

        if ((parameterTypes != null) && !parameterTypes.isEmpty()) {
            int lastParameterIndex = parameterTypes.size() - 1;
            boolean varargs = ((method.getAccessFlags() & FLAG_VARARGS) != 0);

            initLocalVariablesFromParameterTypes(classFile, parameterTypes, varargs, firstVariableIndex, lastParameterIndex);

            // Create list of parameterTypes
            formalParameters = new FormalParameters();

            AttributeParameterAnnotations rvpa = method.getAttribute("RuntimeVisibleParameterAnnotations");
            AttributeParameterAnnotations ripa = method.getAttribute("RuntimeInvisibleParameterAnnotations");

            if ((rvpa == null) && (ripa == null)) {
                for (int parameterIndex=0, variableIndex=firstVariableIndex; parameterIndex<=lastParameterIndex; parameterIndex++, variableIndex++) {
                    AbstractLocalVariable lv = localVariableSet.root(variableIndex);

                    formalParameters.add(new ClassFileFormalParameter(lv, varargs && (parameterIndex==lastParameterIndex)));

                    if (PrimitiveType.TYPE_LONG.equals(lv.getType()) || PrimitiveType.TYPE_DOUBLE.equals(lv.getType())) {
                        variableIndex++;
                    }
                }
            } else {
                Annotations[] visiblesArray = (rvpa == null) ? null : rvpa.getParameterAnnotations();
                Annotations[] invisiblesArray = (ripa == null) ? null : ripa.getParameterAnnotations();
                AnnotationConverter annotationConverter = new AnnotationConverter(objectTypeMaker);

                for (int parameterIndex=0, variableIndex=firstVariableIndex; parameterIndex<=lastParameterIndex; parameterIndex++, variableIndex++) {
                    AbstractLocalVariable lv = localVariableSet.root(variableIndex);

                    Annotations visibles = ((visiblesArray == null) || (visiblesArray.length <= parameterIndex)) ? null : visiblesArray[parameterIndex];
                    Annotations invisibles = ((invisiblesArray == null) || (invisiblesArray.length <= parameterIndex)) ? null : invisiblesArray[parameterIndex];
                    BaseAnnotationReference annotationReferences = annotationConverter.convert(visibles, invisibles);

                    formalParameters.add(new ClassFileFormalParameter(annotationReferences, lv, varargs && (parameterIndex==lastParameterIndex)));

                    if (PrimitiveType.TYPE_LONG.equals(lv.getType()) || PrimitiveType.TYPE_DOUBLE.equals(lv.getType())) {
                        variableIndex++;
                    }
                }
            }
        }

        // Initialize root frame and cache
        localVariableCache = localVariableSet.initialize(currentFrame);
    }

    protected void initLocalVariablesFromAttributes(Method method) {
        AttributeCode code = method.getAttribute("Code");

        // Init local variables from attributes
        if (code != null) {
            AttributeLocalVariableTable localVariableTable = code.getAttribute("LocalVariableTable");

            if (localVariableTable != null) {
                boolean staticFlag = (method.getAccessFlags() & FLAG_STATIC) != 0;

                for (org.jd.core.v1.model.classfile.attribute.LocalVariable localVariable : localVariableTable.getLocalVariableTable()) {
                    int index = localVariable.getIndex();
                    int startPc = (!staticFlag && index==0) ? 0 : localVariable.getStartPc();
                    String descriptor = localVariable.getDescriptor();
                    String name = localVariable.getName();
                    AbstractLocalVariable lv;

                    if (descriptor.charAt(descriptor.length() - 1) == ';') {
                        lv = new ObjectLocalVariable(objectTypeMaker, index, startPc, objectTypeMaker.makeFromDescriptor(descriptor), name);
                    } else {
                        int dimension = SignatureParser.countDimension(descriptor);

                        if (dimension == 0) {
                            lv = new PrimitiveLocalVariable(index, startPc, PrimitiveType.getPrimitiveType(descriptor.charAt(0)), name);
                        } else {
                            lv = new ObjectLocalVariable(objectTypeMaker, index, startPc, signatureParser.parseTypeSignature(descriptor.substring(dimension)).createType(dimension), name);
                        }
                    }

                    localVariableSet.add(index, lv);
                    blackListNames.add(name);
                    names.add(name);
                }
            }

            AttributeLocalVariableTypeTable localVariableTypeTable = code.getAttribute("LocalVariableTypeTable");

            if (localVariableTypeTable != null) {
                UpdateTypeVisitor updateTypeVisitor = new UpdateTypeVisitor(localVariableSet);

                for (LocalVariableType lv : localVariableTypeTable.getLocalVariableTypeTable()) {
                    updateTypeVisitor.setLocalVariableType(lv);
                    signatureParser.parseTypeSignature(lv.getSignature()).accept(updateTypeVisitor);
                }
            }
        }
    }

    protected void initLocalVariablesFromParameterTypes(ClassFile classFile, List<Type> parameterTypes, boolean varargs, int firstVariableIndex, int lastParameterIndex) {
        HashMap<Type, Boolean> typeMap = new HashMap<>();

        for (int parameterIndex=0; parameterIndex<=lastParameterIndex; parameterIndex++) {
            Type type = parameterTypes.get(parameterIndex);
            typeMap.put(type, Boolean.valueOf(typeMap.containsKey(type)));
        }

        String parameterNamePrefix = "param";

        if (classFile.getOuterClassFile() != null) {
            int innerTypeDepth = 1;
            ObjectType type = objectTypeMaker.makeFromInternalTypeName(classFile.getOuterClassFile().getInternalTypeName());

            while ((type != null) && (type.getClass() == InnerObjectType.class)) {
                innerTypeDepth++;
                type = ((InnerObjectType)type).getOuterType();
            }

            parameterNamePrefix += innerTypeDepth;
        }

        StringBuilder sb = new StringBuilder();
        GenerateParameterSuffixNameVisitor generateParameterSuffixNameVisitor = new GenerateParameterSuffixNameVisitor();

        for (int parameterIndex=0, variableIndex=firstVariableIndex; parameterIndex<=lastParameterIndex; parameterIndex++, variableIndex++) {
            Type type = parameterTypes.get(parameterIndex);
            AbstractLocalVariable lv = localVariableSet.root(variableIndex);

            if (lv == null) {
                sb.setLength(0);
                sb.append(parameterNamePrefix);

                if ((parameterIndex == lastParameterIndex) && varargs) {
                    sb.append("VarArgs");
//                } else if (lastType.getDimension() > 1) {
//                    sb.append("ArrayOfArray");
                } else {
                    if (type.getDimension() > 0) {
                        sb.append("ArrayOf");
                    }
                    type.accept(generateParameterSuffixNameVisitor);
                    sb.append(generateParameterSuffixNameVisitor.getSuffix());
                }

                int length = sb.length();
                int counter = 1;

                if (typeMap.get(type)) {
                    sb.append(counter++);
                }

                String name = sb.toString();

                while (blackListNames.contains(name)) {
                    sb.setLength(length);
                    sb.append(counter++);
                    name = sb.toString();
                }

                blackListNames.add(name);
                createParameterVisitor.init(variableIndex, name);
                type.accept(createParameterVisitor);

                AbstractLocalVariable alv = createParameterVisitor.getLocalVariable();

                alv.setDeclared(true);
                localVariableSet.add(variableIndex, alv);
            }

            if (PrimitiveType.TYPE_LONG.equals(type) || PrimitiveType.TYPE_DOUBLE.equals(type)) {
                variableIndex++;
            }
        }
    }

    public AbstractLocalVariable getLocalVariable(int index, int offset) {
        AbstractLocalVariable lv = localVariableCache[index];

        if (lv == null) {
            lv = currentFrame.getLocalVariable(index);
//            assert lv != null : "getLocalVariable : local variable not found";
            if (lv == null) {
                lv = new ObjectLocalVariable(objectTypeMaker, index, offset, ObjectType.TYPE_OBJECT, "SYNTHETIC_LOCAL_VARIABLE_"+index, true);
            }
        } else if (lv.getFrame() != currentFrame) {
            Frame frame = searchCommonParentFrame(lv.getFrame(), currentFrame);
            frame.mergeLocalVariable(lv);

            if (lv.getFrame() != frame) {
                lv.getFrame().removeLocalVariable(lv);
                frame.addLocalVariable(lv);
            }
        }

        lv.setToOffset(offset);

        return lv;
    }

    protected AbstractLocalVariable searchLocalVariable(int index, int offset) {
        AbstractLocalVariable lv = localVariableSet.get(index, offset);

        if (lv == null) {
            lv = currentFrame.getLocalVariable(index);
        } else {
            AbstractLocalVariable lv2 = currentFrame.getLocalVariable(index);

            if ((lv2 != null) && (lv.getFromOffset() < lv2.getFromOffset())) {
                lv = lv2;
            } else {
                localVariableSet.remove(index, offset);
            }
        }

        return lv;
    }

    public AbstractLocalVariable getLocalVariableInAssignment(int index, int offset, Type valueType) {
        AbstractLocalVariable lv = searchLocalVariable(index, offset);

        if (lv == null) {
            // Create a new local variable
            createLocalVariableVisitor.init(index, offset);
            valueType.accept(createLocalVariableVisitor);
            lv = createLocalVariableVisitor.getLocalVariable();
        } else if (lv.isAssignableFrom(valueType)) {
            // Reduce type
            lv.typeOnRight(valueType);
        } else {
            // Not assignable -> Create a new local variable
            createLocalVariableVisitor.init(index, offset);
            valueType.accept(createLocalVariableVisitor);
            lv = createLocalVariableVisitor.getLocalVariable();
        }

        lv.setToOffset(offset);
        store(lv);

        return lv;
    }

    public AbstractLocalVariable getLocalVariableInNullAssignment(int index, int offset, Type valueType) {
        AbstractLocalVariable lv = searchLocalVariable(index, offset);

        if (lv == null) {
            // Create a new local variable
            createLocalVariableVisitor.init(index, offset);
            valueType.accept(createLocalVariableVisitor);
            lv = createLocalVariableVisitor.getLocalVariable();
        } else {
            Type type = lv.getType();

            if ((type.getDimension() == 0) && type.isPrimitive()) {
                // Not assignable -> Create a new local variable
                createLocalVariableVisitor.init(index, offset);
                valueType.accept(createLocalVariableVisitor);
                lv = createLocalVariableVisitor.getLocalVariable();
            }
        }

        lv.setToOffset(offset);
        store(lv);

        return lv;
    }

    public AbstractLocalVariable getLocalVariableInAssignment(int index, int offset, AbstractLocalVariable valueLocalVariable) {
        AbstractLocalVariable lv = searchLocalVariable(index, offset);

        if (lv == null) {
            // Create a new local variable
            createLocalVariableVisitor.init(index, offset);
            valueLocalVariable.accept(createLocalVariableVisitor);
            lv = createLocalVariableVisitor.getLocalVariable();
        } else if (!lv.isAssignableFrom(valueLocalVariable)) {
            // Not assignable -> Create a new local variable
            createLocalVariableVisitor.init(index, offset);
            valueLocalVariable.accept(createLocalVariableVisitor);
            lv = createLocalVariableVisitor.getLocalVariable();
        }

        lv.variableOnRight(valueLocalVariable);
        lv.setToOffset(offset);
        store(lv);

        return lv;
    }

    public AbstractLocalVariable getExceptionLocalVariable(int index, int offset, ObjectType type) {
        AbstractLocalVariable lv;

        if (index == -1) {
            currentFrame.setExceptionLocalVariable(lv = new ObjectLocalVariable(objectTypeMaker, index, offset, type, null, true));
        } else {
            lv = localVariableSet.remove(index, offset);

            if (lv == null) {
                lv = new ObjectLocalVariable(objectTypeMaker, index, offset, type, null, true);
            } else {
                lv.setDeclared(true);
            }

            currentFrame.addLocalVariable(lv);
        }

        return lv;
    }

    public void removeLocalVariable(AbstractLocalVariable lv) {
        int index = lv.getIndex();

        if (index < localVariableCache.length) {
            // Remove from cache
            localVariableCache[index] = null;
            // Remove from current frame
            currentFrame.removeLocalVariable(lv);
        }
    }

    protected void store(AbstractLocalVariable lv) {
        // Store to cache
        int index = lv.getIndex();

        if (index >= localVariableCache.length) {
            AbstractLocalVariable[] tmp = localVariableCache;
            localVariableCache = new AbstractLocalVariable[index * 2];
            System.arraycopy(tmp, 0, localVariableCache, 0, tmp.length);
        }

        localVariableCache[index] = lv;

        // Store to current frame
        if (lv.getFrame() == null) {
            currentFrame.addLocalVariable(lv);
        }
    }

    public boolean containsName(String name) {
        return names.contains(name);
    }

    public void make() {
        currentFrame.createNames(blackListNames);
        currentFrame.createDeclarations();
    }

    public BaseFormalParameter getFormalParameters() {
        return formalParameters;
    }

    public void pushFrame(Statements statements) {
        Frame parent = currentFrame;
        currentFrame = new Frame(currentFrame, statements);
        parent.addChild(currentFrame);
    }

    public void popFrame() {
        currentFrame.close();
        currentFrame = currentFrame.getParent();
    }

    protected Frame searchCommonParentFrame(Frame frame1, Frame frame2) {
        HashSet<Frame> set = new HashSet<>();

        while (frame1 != null) {
            set.add(frame1);
            frame1 = frame1.getParent();
        }

        while (frame2 != null) {
            if (set.contains(frame2)) {
                return frame2;
            }
            frame2 = frame2.getParent();
        }

        return null;
    }
}
