/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.LocalVariableTypeTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.ParameterAnnotationEntry;
import org.apache.bcel.classfile.RuntimeInvisibleParameterAnnotations;
import org.apache.bcel.classfile.RuntimeVisibleParameterAnnotations;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameters;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFormalParameter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.Frame;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.LocalVariableSet;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.PrimitiveLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.RootFrame;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.CreateLocalVariableVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.CreateParameterVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.GenerateParameterSuffixNameVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.PopulateBlackListNamesVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchInTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.UpdateTypeVisitor;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.bcel.Const.ACC_STATIC;

public class LocalVariableMaker {
    private final LocalVariableSet localVariableSet = new LocalVariableSet();
    private final Set<String> names = new HashSet<>();
    private final Set<String> blackListNames = new HashSet<>();
    private Frame currentFrame = new RootFrame();
    private AbstractLocalVariable[] localVariableCache;

    private final TypeMaker typeMaker;
    private final Map<String, BaseType> typeBounds;
    private final FormalParameters formalParameters;

    private final PopulateBlackListNamesVisitor populateBlackListNamesVisitor = new PopulateBlackListNamesVisitor(blackListNames);
    private final SearchInTypeArgumentVisitor searchInTypeArgumentVisitor = new SearchInTypeArgumentVisitor();
    private final CreateParameterVisitor createParameterVisitor;
    private final CreateLocalVariableVisitor createLocalVariableVisitor;

    public LocalVariableMaker(TypeMaker typeMaker, ClassFileConstructorOrMethodDeclaration comd, boolean constructor) {
        ClassFile classFile = comd.getClassFile();
        Method method = comd.getMethod();
        BaseType parameterTypes = comd.getParameterTypes();

        this.typeMaker = typeMaker;
        this.typeBounds = comd.getTypeBounds();
        this.createParameterVisitor = new CreateParameterVisitor(typeMaker);
        this.createLocalVariableVisitor = new CreateLocalVariableVisitor(typeMaker);

        // Initialize local black list variable names
        String descriptor;
        for (Field field : classFile.getFields()) {
            descriptor = field.getSignature();

            if (descriptor.charAt(descriptor.length() - 1) == ';') {
                typeMaker.makeFromDescriptor(descriptor).accept(populateBlackListNamesVisitor);
            }
        }

        typeMaker.makeFromInternalTypeName(classFile.getInternalTypeName()).accept(populateBlackListNamesVisitor);

        if (classFile.getSuperTypeName() != null) {
            typeMaker.makeFromInternalTypeName(classFile.getSuperTypeName()).accept(populateBlackListNamesVisitor);
        }

        for (String interfaceTypeName : classFile.getInterfaceTypeNames()) {
            typeMaker.makeFromInternalTypeName(interfaceTypeName).accept(populateBlackListNamesVisitor);
        }

        if (parameterTypes != null) {
            if (parameterTypes.isList()) {
                for (Type type : parameterTypes) {
                    type.accept(populateBlackListNamesVisitor);
                }
            } else {
                parameterTypes.getFirst().accept(populateBlackListNamesVisitor);
            }
        }

        // Initialize local variables from 'LocalVariableTable' and 'LocalVariableTypeTable' attributes
        initLocalVariablesFromAttributes(method);

        // Initialize local variables from access flags & signature
        int firstVariableIndex = 0;

        if ((method.getAccessFlags() & ACC_STATIC) == 0) {
            if (localVariableSet.root(0) == null) {
                // Local variable missing
                localVariableSet.add(0, new ObjectLocalVariable(typeMaker, 0, 0, typeMaker.makeFromInternalTypeName(classFile.getInternalTypeName()), "this"));
            }
            firstVariableIndex = 1;
        }

        if (constructor) {
            if (classFile.isEnum()) {
                if (localVariableSet.root(1) == null) {
                    // Local variable missing
                    localVariableSet.add(1, new ObjectLocalVariable(typeMaker, 1, 0, ObjectType.TYPE_STRING, "this$enum$name"));
                }
                if (localVariableSet.root(2) == null) {
                    // Local variable missing
                    localVariableSet.add(2, new PrimitiveLocalVariable(2, 0, PrimitiveType.TYPE_INT, "this$enum$index"));
                }
            } else if (classFile.getOuterClassFile() != null && !classFile.isStatic() && localVariableSet.root(1) == null) {
                // Local variable missing
                localVariableSet.add(1, new ObjectLocalVariable(typeMaker, 1, 0, typeMaker.makeFromInternalTypeName(classFile.getOuterClassFile().getInternalTypeName()), "this$0"));
            }
        }

        FormalParameters fp = null;
        
        if (parameterTypes != null) {
            int lastParameterIndex = parameterTypes.size() - 1;
            boolean varargs = (method.getAccessFlags() & Const.ACC_VARARGS) != 0;

            initLocalVariablesFromParameterTypes(classFile, parameterTypes, varargs, firstVariableIndex, lastParameterIndex);

            // Create list of parameterTypes
            fp = new FormalParameters();

            RuntimeVisibleParameterAnnotations rvpa = (RuntimeVisibleParameterAnnotations) Stream.of(method.getAttributes()).filter(RuntimeVisibleParameterAnnotations.class::isInstance).findAny().orElse(null);
            RuntimeInvisibleParameterAnnotations ripa = (RuntimeInvisibleParameterAnnotations) Stream.of(method.getAttributes()).filter(RuntimeInvisibleParameterAnnotations.class::isInstance).findAny().orElse(null);

            if (rvpa == null && ripa == null) {
                AbstractLocalVariable lv;
                for (int parameterIndex=0, variableIndex=firstVariableIndex; parameterIndex<=lastParameterIndex; parameterIndex++, variableIndex++) {
                    lv = localVariableSet.root(variableIndex);
                    if (lv != null) {
                        fp.add(new ClassFileFormalParameter(lv, varargs && parameterIndex == lastParameterIndex));

                        if (PrimitiveType.TYPE_LONG.equals(lv.getType()) || PrimitiveType.TYPE_DOUBLE.equals(lv.getType())) {
                            variableIndex++;
                        }
                    }
                }
            } else {
                ParameterAnnotationEntry[] visiblesArray = rvpa == null ? null : rvpa.getParameterAnnotationEntries();
                ParameterAnnotationEntry[] invisiblesArray = ripa == null ? null : ripa.getParameterAnnotationEntries();
                AnnotationConverter annotationConverter = new AnnotationConverter(typeMaker);

                AbstractLocalVariable lv;
                ParameterAnnotationEntry visibles;
                ParameterAnnotationEntry invisibles;
                BaseAnnotationReference annotationReferences;
                for (int parameterIndex=0, variableIndex=firstVariableIndex; parameterIndex<=lastParameterIndex; parameterIndex++, variableIndex++) {
                    lv = localVariableSet.root(variableIndex);

                    visibles = visiblesArray == null || visiblesArray.length <= parameterIndex ? null : visiblesArray[parameterIndex];
                    invisibles = invisiblesArray == null || invisiblesArray.length <= parameterIndex ? null : invisiblesArray[parameterIndex];
                    AnnotationEntry[] visibleEntries = visibles == null ? null : visibles.getAnnotationEntries();
                    AnnotationEntry[] invisibleEntries = invisibles == null ? null : invisibles.getAnnotationEntries();
                    annotationReferences = annotationConverter.convert(visibleEntries, invisibleEntries);

                    fp.add(new ClassFileFormalParameter(annotationReferences, lv, varargs && parameterIndex==lastParameterIndex));

                    if (PrimitiveType.TYPE_LONG.equals(lv.getType()) || PrimitiveType.TYPE_DOUBLE.equals(lv.getType())) {
                        variableIndex++;
                    }
                }
            }
        }
        
        this.formalParameters = fp;

        // Initialize root frame and cache
        localVariableCache = localVariableSet.initialize(currentFrame);
    }

    protected void initLocalVariablesFromAttributes(Method method) {
        Code code = method.getCode();

        // Init local variables from attributes
        if (code != null) {
            LocalVariableTable localVariableTable = code.getLocalVariableTable();

            if (localVariableTable != null) {
                boolean staticFlag = (method.getAccessFlags() & ACC_STATIC) != 0;

                int index;
                int startPc;
                String descriptor;
                String name;
                AbstractLocalVariable lv;
                for (LocalVariable localVariable : localVariableTable.getLocalVariableTable()) {
                    index = localVariable.getIndex();
                    startPc = !staticFlag && index==0 ? 0 : localVariable.getStartPC();
                    descriptor = localVariable.getSignature();
                    name = localVariable.getName();
                    if (descriptor.charAt(descriptor.length() - 1) == ';') {
                        lv = new ObjectLocalVariable(typeMaker, index, startPc, typeMaker.makeFromDescriptor(descriptor), name);
                    } else {
                        int dimension = TypeMaker.countDimension(descriptor);

                        if (dimension == 0) {
                            lv = new PrimitiveLocalVariable(index, startPc, PrimitiveType.getPrimitiveType(descriptor.charAt(0)), name);
                        } else {
                            lv = new ObjectLocalVariable(typeMaker, index, startPc, typeMaker.makeFromSignature(descriptor.substring(dimension)).createType(dimension), name);
                        }
                    }

                    localVariableSet.add(index, lv);
                    names.add(name);
                }
            }

            LocalVariableTypeTable localVariableTypeTable = (LocalVariableTypeTable) Stream.of(code.getAttributes()).filter(LocalVariableTypeTable.class::isInstance).findAny().orElse(null);

            if (localVariableTypeTable != null) {
                UpdateTypeVisitor updateTypeVisitor = new UpdateTypeVisitor(localVariableSet);

                for (LocalVariable lv : localVariableTypeTable.getLocalVariableTypeTable()) {
                    updateTypeVisitor.setLocalVariableType(lv);
                    typeMaker.makeFromSignature(lv.getSignature()).accept(updateTypeVisitor);
                }
            }
        }
    }

    protected void initLocalVariablesFromParameterTypes(ClassFile classFile, BaseType parameterTypes, boolean varargs, int firstVariableIndex, int lastParameterIndex) {
        Map<Type, Boolean> typeMap = new HashMap<>();
        DefaultList<Type> t = parameterTypes.getList();

        for (int parameterIndex=0; parameterIndex<=lastParameterIndex; parameterIndex++) {
            Type type = t.get(parameterIndex);
            typeMap.put(type, Boolean.valueOf(typeMap.containsKey(type)));
        }

        String parameterNamePrefix = "param";

        if (classFile.getOuterClassFile() != null) {
            int innerTypeDepth = 1;
            ObjectType type = typeMaker.makeFromInternalTypeName(classFile.getOuterClassFile().getInternalTypeName());

            while (type != null && type.isInnerObjectType()) {
                innerTypeDepth++;
                type = type.getOuterType();
            }

            parameterNamePrefix += innerTypeDepth;
        }

        StringBuilder sb = new StringBuilder();
        GenerateParameterSuffixNameVisitor generateParameterSuffixNameVisitor = new GenerateParameterSuffixNameVisitor();

        Type type;
        AbstractLocalVariable lv;
        for (int parameterIndex=0, variableIndex=firstVariableIndex; parameterIndex<=lastParameterIndex; parameterIndex++, variableIndex++) {
            type = t.get(parameterIndex);
            lv = localVariableSet.root(variableIndex);

            if (lv == null) {
                sb.setLength(0);
                sb.append(parameterNamePrefix);

                if (parameterIndex == lastParameterIndex && varargs) {
                    sb.append("VarArgs");
//                } else if (type.getDimension() > 1) {
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

                if (Boolean.TRUE.equals(typeMap.get(type))) {
                    sb.append(counter);
                    counter++;
                }

                String name = sb.toString();

                while (names.contains(name)) {
                    sb.setLength(length);
                    sb.append(counter);
                    counter++;
                    name = sb.toString();
                }

                names.add(name);
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
            if (lv == null) {
                lv = new ObjectLocalVariable(typeMaker, index, offset, ObjectType.TYPE_OBJECT, "SYNTHETIC_LOCAL_VARIABLE_"+index, true);
            }
        } else if (lv.getFrame() != currentFrame) {
            Frame frame = searchCommonParentFrame(lv.getFrame(), currentFrame);
            if (frame != null) {
                frame.mergeLocalVariable(typeBounds, this, lv);

                if (lv.getFrame() != frame) {
                    lv.getFrame().removeLocalVariable(lv);
                    frame.addLocalVariable(lv);
                }
            }
        }

        lv.setFromToOffset(offset);

        return lv;
    }

    protected AbstractLocalVariable searchLocalVariable(int index, int offset) {
        AbstractLocalVariable lv = localVariableSet.get(index, offset);

        if (lv == null) {
            lv = currentFrame.getLocalVariable(index);
        } else {
            AbstractLocalVariable lv2 = currentFrame.getLocalVariable(index);

            if (lv2 != null && (lv.getName() == null ? lv2.getName() == null : lv.getName().equals(lv2.getName())) && lv.getType().equals(lv2.getType())) {
                lv = lv2;
            }

            localVariableSet.remove(index, offset);
        }

        return lv;
    }

    public boolean isCompatible(AbstractLocalVariable lv, Type valueType) {
        if (valueType == ObjectType.TYPE_UNDEFINED_OBJECT) {
            return true;
        }
        if (valueType.isObjectType() && lv.getType().getDimension() == valueType.getDimension()) {
            ObjectType valueObjectType = (ObjectType) valueType;

            if (lv.getType().isObjectType()) {
                ObjectType lvObjectType = (ObjectType) lv.getType();

                BaseTypeArgument lvTypeArguments = lvObjectType.getTypeArguments();
                BaseTypeArgument valueTypeArguments = valueObjectType.getTypeArguments();

                if (lvTypeArguments == null || valueTypeArguments == null || valueTypeArguments == WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT) {
                    return typeMaker.isRawTypeAssignable(lvObjectType, valueObjectType);
                }

                searchInTypeArgumentVisitor.init();
                lvTypeArguments.accept(searchInTypeArgumentVisitor);

                if (!searchInTypeArgumentVisitor.containsGeneric()) {
                    searchInTypeArgumentVisitor.init();
                    valueTypeArguments.accept(searchInTypeArgumentVisitor);

                    if (searchInTypeArgumentVisitor.containsGeneric()) {
                        return typeMaker.isRawTypeAssignable(lvObjectType, valueObjectType);
                    }
                }
            } else if (lv.getType().isGenericType() && valueObjectType.getInternalName().equals(ObjectType.TYPE_OBJECT.getInternalName())) {
                return true;
            }
        }

        return false;
    }

    public AbstractLocalVariable getLocalVariableInAssignment(Map<String, BaseType> typeBounds, int index, int offset, Type valueType) {
        AbstractLocalVariable lv = searchLocalVariable(index, offset);

        if (lv != null && (lv.isAssignableFrom(typeBounds, valueType) || isCompatible(lv, valueType))) {
            // Assignable, reduce type
            lv.typeOnRight(typeBounds, valueType);
        } else if (lv == null || !lv.getType().isGenericType() || ObjectType.TYPE_OBJECT != valueType) {
            // Create a new local variable
            lv = createNewLocalVariable(index, offset, valueType);
        }

        lv.setFromToOffset(offset);
        store(lv);

        return lv;
    }

    public AbstractLocalVariable getLocalVariableInNullAssignment(int index, int offset, Type valueType) {
        AbstractLocalVariable lv = searchLocalVariable(index, offset);

        if (lv == null || lv.getType().getDimension() == 0 && lv.getType().isPrimitiveType()) {
            lv = createNewLocalVariable(index, offset, valueType);
        }

        lv.setFromToOffset(offset);
        store(lv);

        return lv;
    }

    public AbstractLocalVariable getLocalVariableInAssignment(Map<String, BaseType> typeBounds, int index, int offset, AbstractLocalVariable valueLocalVariable) {
        AbstractLocalVariable lv = searchLocalVariable(index, offset);

        if (lv == null || !lv.isAssignableFrom(typeBounds, valueLocalVariable) && !isCompatible(lv, valueLocalVariable.getType()) && (!lv.getType().isGenericType() || ObjectType.TYPE_OBJECT != valueLocalVariable.getType())) {
            // Create a new local variable
            lv = createNewLocalVariable(index, offset, valueLocalVariable);
        }

        lv.variableOnRight(typeBounds, valueLocalVariable);
        lv.setFromToOffset(offset);
        store(lv);

        return lv;
    }

    protected AbstractLocalVariable createNewLocalVariable(int index, int offset, Type valueType) {
        createLocalVariableVisitor.init(index, offset);
        valueType.accept(createLocalVariableVisitor);
        return createLocalVariableVisitor.getLocalVariable();
    }

    protected AbstractLocalVariable createNewLocalVariable(int index, int offset,
            AbstractLocalVariable valueLocalVariable) {
        createLocalVariableVisitor.init(index, offset);
        valueLocalVariable.accept(createLocalVariableVisitor);
        return createLocalVariableVisitor.getLocalVariable();
    }

    public AbstractLocalVariable getExceptionLocalVariable(int index, int offset, ObjectType type) {
        AbstractLocalVariable lv;

        if (index == -1) {
            lv = new ObjectLocalVariable(typeMaker, index, offset, type, null, true);
            currentFrame.setExceptionLocalVariable(lv);
        } else {
            lv = localVariableSet.remove(index, offset);

            if (lv == null) {
                lv = new ObjectLocalVariable(typeMaker, index, offset, type, "e", true);
            } else {
                lv.setDeclared(true);
            }

            currentFrame.addLocalVariable(lv);
        }

        return lv;
    }

    public void removeLocalVariable(AbstractLocalVariable lv) {
        if (lv != null && lv.getIndex() < localVariableCache.length) {
            // Remove from cache
            localVariableCache[lv.getIndex()] = null;
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

    public void make(boolean containsLineNumber, TypeMaker typeMaker) {
        currentFrame.updateLocalVariableInForStatements(typeMaker);
        currentFrame.createNames(blackListNames);
        currentFrame.createDeclarations(containsLineNumber);
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
        currentFrame = currentFrame.getParent();
    }

    protected static Frame searchCommonParentFrame(Frame frame1, Frame frame2) {
        if (frame1 == frame2 || frame2.getParent() == frame1) {
            return frame1;
        }

        if (frame1.getParent() == frame2) {
            return frame2;
        }

        Set<Frame> set = new HashSet<>();

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

    public void changeFrame(AbstractLocalVariable localVariable) {
        Frame frame = searchCommonParentFrame(localVariable.getFrame(), currentFrame);

        if (frame != null && localVariable.getFrame() != frame) {
            localVariable.getFrame().removeLocalVariable(localVariable);
            frame.addLocalVariable(localVariable);
        }
    }
}
