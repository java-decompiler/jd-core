/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.processor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationDefault;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Annotations;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Module;
import org.apache.bcel.classfile.ModuleExports;
import org.apache.bcel.classfile.ModuleOpens;
import org.apache.bcel.classfile.ModuleProvides;
import org.apache.bcel.classfile.ModuleRequires;
import org.apache.bcel.classfile.RuntimeInvisibleAnnotations;
import org.apache.bcel.classfile.RuntimeVisibleAnnotations;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.javasyntax.declaration.Declaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.ModuleDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.TypeDeclaration;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.BaseElementValue;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileAnnotationDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileClassDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileEnumDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileInterfaceDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileTypeDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.AnnotationConverter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.PopulateBindingsWithTypeParameterVisitor;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Convert ClassFile model to Java syntax model.<br><br>
 *
 * Input:  {@link org.jd.core.v1.model.classfile.ClassFile}<br>
 * Output: {@link org.jd.core.v1.model.javasyntax.CompilationUnit}<br>
 */
public class ConvertClassFileProcessor {
    private final PopulateBindingsWithTypeParameterVisitor populateBindingsWithTypeParameterVisitor = new PopulateBindingsWithTypeParameterVisitor() {
        @Override
        public void visit(TypeParameter parameter) {
            bindings.put(parameter.getIdentifier(), new GenericType(parameter.getIdentifier()));
        }
        @Override
        public void visit(TypeParameterWithTypeBounds parameter) {
            bindings.put(parameter.getIdentifier(), new GenericType(parameter.getIdentifier()));
            typeBounds.put(parameter.getIdentifier(), parameter.getTypeBounds());
        }
    };

    public CompilationUnit process(ClassFile classFile, TypeMaker typeMaker, DecompileContext decompileContext) {
        AnnotationConverter annotationConverter = new AnnotationConverter(typeMaker);

        TypeDeclaration typeDeclaration;

        if (classFile.isEnum()) {
            typeDeclaration = convertEnumDeclaration(typeMaker, annotationConverter, classFile, null);
        } else if (classFile.isAnnotation()) {
            typeDeclaration = convertAnnotationDeclaration(typeMaker, annotationConverter, classFile, null);
        } else if (classFile.isModule()) {
            typeDeclaration = convertModuleDeclaration(classFile);
        } else if (classFile.isInterface()) {
            typeDeclaration = convertInterfaceDeclaration(typeMaker, annotationConverter, classFile, null);
        } else {
            typeDeclaration = convertClassDeclaration(typeMaker, annotationConverter, classFile, null);
        }

        decompileContext.setMajorVersion(classFile.getMajorVersion());
        decompileContext.setMinorVersion(classFile.getMinorVersion());
        return new CompilationUnit(typeDeclaration);
    }

    protected ClassFileInterfaceDeclaration convertInterfaceDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileInterfaceDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                typeTypes.getTypeParameters(), typeTypes.getInterfaces(), bodyDeclaration);
    }

    protected ClassFileEnumDeclaration convertEnumDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileEnumDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                typeTypes.getInterfaces(), bodyDeclaration);
    }

    protected ClassFileAnnotationDeclaration convertAnnotationDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileAnnotationDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                bodyDeclaration);
    }

    protected ClassFileClassDeclaration convertClassDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.getTypeParameters(), outerClassFileBodyDeclaration);

        return new ClassFileClassDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.getThisType().getInternalName(), typeTypes.getThisType().getName(),
                typeTypes.getTypeParameters(), typeTypes.getSuperType(),
                typeTypes.getInterfaces(), bodyDeclaration);
    }

    protected ClassFileBodyDeclaration convertBodyDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, BaseTypeParameter typeParameters, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        Map<String, TypeArgument> bindings;
        Map<String, BaseType> typeBounds;

        if (!classFile.isStatic() && outerClassFileBodyDeclaration != null) {
            bindings = outerClassFileBodyDeclaration.getBindings();
            typeBounds = outerClassFileBodyDeclaration.getTypeBounds();
        } else {
            bindings = Collections.emptyMap();
            typeBounds = Collections.emptyMap();
        }

        if (typeParameters != null) {
            bindings=new HashMap<>(bindings);
            typeBounds=new HashMap<>(typeBounds);
            populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
            typeParameters.accept(populateBindingsWithTypeParameterVisitor);
        }

        ClassFileBodyDeclaration bodyDeclaration = new ClassFileBodyDeclaration(classFile, bindings, typeBounds, outerClassFileBodyDeclaration);

        bodyDeclaration.setFieldDeclarations(convertFields(parser, converter, classFile));
        bodyDeclaration.setMethodDeclarations(convertMethods(parser, converter, bodyDeclaration, classFile));
        bodyDeclaration.setInnerTypeDeclarations(convertInnerTypes(parser, converter, classFile, bodyDeclaration));

        return bodyDeclaration;
    }

    protected List<ClassFileFieldDeclaration> convertFields(TypeMaker parser, AnnotationConverter converter, ClassFile classFile) {
        Field[] fields = classFile.getFields();

        DefaultList<ClassFileFieldDeclaration> list = new DefaultList<>(fields.length);
        BaseAnnotationReference annotationReferences;
        Type typeField;
        ExpressionVariableInitializer variableInitializer;
        FieldDeclarator fieldDeclarator;
        for (Field field : fields) {
            annotationReferences = convertAnnotationReferences(converter, field);
            typeField = parser.parseFieldSignature(classFile, field);
            variableInitializer = convertFieldInitializer(field, typeField);
            fieldDeclarator = new FieldDeclarator(field.getName(), variableInitializer);

            list.add(new ClassFileFieldDeclaration(annotationReferences, field.getAccessFlags(), typeField, fieldDeclarator));
        }
        return list;
    }

    protected List<ClassFileConstructorOrMethodDeclaration> convertMethods(TypeMaker parser, AnnotationConverter converter, ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile) {
        Method[] methods = classFile.getMethods();

        DefaultList<ClassFileConstructorOrMethodDeclaration> list = new DefaultList<>(methods.length);
        String name;
        BaseAnnotationReference annotationReferences;
        BaseElementValue defaultAnnotationValue;
        TypeMaker.MethodTypes methodTypes;
        Map<String, TypeArgument> bindings;
        Map<String, BaseType> typeBounds;
        Code code;
        int firstLineNumber;
        for (Method method : methods) {
            name = method.getName();
            annotationReferences = convertAnnotationReferences(converter, method);
            AnnotationDefault annotationDefault = (AnnotationDefault) Stream.of(method.getAttributes()).filter(AnnotationDefault.class::isInstance).findAny().orElse(null);
            defaultAnnotationValue = null;

            if (annotationDefault != null) {
                defaultAnnotationValue = converter.convert(annotationDefault.getDefaultValue());
            }

            methodTypes = parser.parseMethodSignature(classFile, method);
            if ((method.getAccessFlags() & Const.ACC_STATIC) == 0) {
                bindings = bodyDeclaration.getBindings();
                typeBounds = bodyDeclaration.getTypeBounds();
            } else {
                bindings = Collections.emptyMap();
                typeBounds = Collections.emptyMap();
            }

            if (methodTypes.getTypeParameters() != null) {
                bindings=new HashMap<>(bindings);
                typeBounds=new HashMap<>(typeBounds);
                populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
                methodTypes.getTypeParameters().accept(populateBindingsWithTypeParameterVisitor);
           }

            code = method.getCode();
            firstLineNumber = 0;

            if (code != null) {
                LineNumberTable lineNumberTable = code.getLineNumberTable();
                if (lineNumberTable != null) {
                    firstLineNumber = lineNumberTable.getLineNumberTable()[0].getLineNumber();
                }
            }

            if (StringConstants.INSTANCE_CONSTRUCTOR.equals(name)) {
                list.add(new ClassFileConstructorDeclaration(
                        bodyDeclaration, classFile, method, annotationReferences, methodTypes.getTypeParameters(),
                        methodTypes.getParameterTypes(), methodTypes.getExceptionTypes(), bindings, typeBounds, firstLineNumber));
            } else if (StringConstants.CLASS_CONSTRUCTOR.equals(name)) {
                list.add(new ClassFileStaticInitializerDeclaration(bodyDeclaration, classFile, method, bindings, typeBounds, firstLineNumber));
            } else {
                ClassFileMethodDeclaration methodDeclaration = new ClassFileMethodDeclaration(
                        bodyDeclaration, classFile, method, annotationReferences, name, methodTypes.getTypeParameters(),
                        methodTypes.getReturnedType(), methodTypes.getParameterTypes(), methodTypes.getExceptionTypes(), defaultAnnotationValue,
                        bindings, typeBounds, firstLineNumber);
                if (classFile.isInterface()) {
                    // For interfaces, add 'default' access flag on public methods
                    if (methodDeclaration.getFlags() == Const.ACC_PUBLIC) {
                        methodDeclaration.setFlags(Const.ACC_PUBLIC|Declaration.FLAG_DEFAULT);
                    }
                    if (methodDeclaration.getFlags() == (Const.ACC_PUBLIC|Const.ACC_VARARGS)) {
                        methodDeclaration.setFlags(Const.ACC_PUBLIC|Const.ACC_VARARGS|Declaration.FLAG_DEFAULT);
                    }
                }
                list.add(methodDeclaration);
            }
        }
        return list;
    }

    protected List<ClassFileTypeDeclaration> convertInnerTypes(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        List<ClassFile> innerClassFiles = classFile.getInnerClassFiles();

        if (innerClassFiles == null) {
            return null;
        }
        DefaultList<ClassFileTypeDeclaration> list = new DefaultList<>(innerClassFiles.size());
        ClassFileTypeDeclaration innerTypeDeclaration;
        for (ClassFile innerClassFile : innerClassFiles) {
            if (innerClassFile.isEnum()) {
                innerTypeDeclaration = convertEnumDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            } else if (innerClassFile.isAnnotation()) {
                innerTypeDeclaration = convertAnnotationDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            } else if (innerClassFile.isInterface()) {
                innerTypeDeclaration = convertInterfaceDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            } else {
                innerTypeDeclaration = convertClassDeclaration(parser, converter, innerClassFile, outerClassFileBodyDeclaration);
            }

            list.add(innerTypeDeclaration);
        }
        return list;
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, ClassFile classFile) {
        Annotations visibles = classFile.getAttribute(Const.ATTR_RUNTIME_VISIBLE_ANNOTATIONS);
        Annotations invisibles = classFile.getAttribute(Const.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS);

        AnnotationEntry[] visibleEntries = visibles == null ? null : visibles.getAnnotationEntries();
        AnnotationEntry[] invisibleEntries = invisibles == null ? null : invisibles.getAnnotationEntries();
        
        return converter.convert(visibleEntries, invisibleEntries);
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, FieldOrMethod fieldOrMethod) {
        Annotations visibles = (Annotations) Stream.of(fieldOrMethod.getAttributes()).filter(RuntimeVisibleAnnotations.class::isInstance).findAny().orElse(null);
        Annotations invisibles = (Annotations) Stream.of(fieldOrMethod.getAttributes()).filter(RuntimeInvisibleAnnotations.class::isInstance).findAny().orElse(null);

        AnnotationEntry[] visibleEntries = visibles == null ? null : visibles.getAnnotationEntries();
        AnnotationEntry[] invisibleEntries = invisibles == null ? null : invisibles.getAnnotationEntries();
        
        return converter.convert(visibleEntries, invisibleEntries);
    }

    protected ExpressionVariableInitializer convertFieldInitializer(Field field, Type typeField) {
        ConstantValue acv = field.getConstantValue();
        if (acv == null) {
            return null;
        }
        Constant constantValue = acv.getConstantPool().getConstant(acv.getConstantValueIndex());
        Expression expression = switch (constantValue.getTag()) {
            case Const.CONSTANT_Integer -> new IntegerConstantExpression(typeField, ((ConstantInteger)constantValue).getBytes());
            case Const.CONSTANT_Float -> new FloatConstantExpression(((ConstantFloat)constantValue).getBytes());
            case Const.CONSTANT_Long -> new LongConstantExpression(((ConstantLong)constantValue).getBytes());
            case Const.CONSTANT_Double -> new DoubleConstantExpression(((ConstantDouble)constantValue).getBytes());
            case Const.CONSTANT_String -> new StringConstantExpression(((ConstantString)constantValue).getBytes(acv.getConstantPool()));
            default -> throw new ConvertClassFileException("Invalid attributes");
        };
        return new ExpressionVariableInitializer(expression);
    }

    protected ModuleDeclaration convertModuleDeclaration(ClassFile classFile) {
        Module attributeModule = classFile.getAttribute(Const.ATTR_MODULE);
        String fieldName = "usesIndex";
        int[] usesIndexes = getFieldValue(attributeModule, fieldName);
        final String[] usedClassNames = new String[usesIndexes.length];
        for (int i = 0; i < usesIndexes.length; i++) {
            usedClassNames[i] = classFile.getConstantPool().getConstantString(usesIndexes[i], Const.CONSTANT_Class);
        }
        List<ModuleDeclaration.ModuleInfo> requires = convertModuleRequiresToModuleInfo(attributeModule.getRequiresTable(), classFile.getConstantPool());
        List<ModuleDeclaration.PackageInfo> exports = convertModuleExportsToPackageInfo(attributeModule.getExportsTable(), classFile.getConstantPool());
        List<ModuleDeclaration.PackageInfo> opens = convertModuleOpensToPackageInfo(attributeModule.getOpensTable(), classFile.getConstantPool());
        DefaultList<String> uses = new DefaultList<>(usedClassNames);
        List<ModuleDeclaration.ServiceInfo> provides = convertModuleProvidesToServiceInfo(attributeModule.getProvidesTable(), classFile.getConstantPool());

        int moduleFlags = getFieldValue(attributeModule, "moduleFlags");
        int moduleNameIndex = getFieldValue(attributeModule, "moduleNameIndex");
        int moduleVersionIndex = getFieldValue(attributeModule, "moduleVersionIndex");
        String moduleName = classFile.getConstantPool().getConstantString(moduleNameIndex, Const.CONSTANT_Module);
        String moduleVersion = classFile.getConstantPool().getConstantString(moduleVersionIndex, Const.CONSTANT_Utf8);
        return new ModuleDeclaration(
                moduleFlags, classFile.getInternalTypeName(), moduleName,
                moduleVersion, requires, exports, opens, uses, provides);
    }

    @SuppressWarnings("all")
    private static <T> T getFieldValue(Object o, String fieldName) {
        try {
            java.lang.reflect.Field f = o.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<ModuleDeclaration.ModuleInfo> convertModuleRequiresToModuleInfo(ModuleRequires[] moduleRequires, ConstantPool constantPool) {
        if (moduleRequires == null || moduleRequires.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.ModuleInfo> list = new DefaultList<>(moduleRequires.length);
        for (ModuleRequires moduleRequire : moduleRequires) {
            int requiresFlags = getFieldValue(moduleRequire, "requiresFlags");
            int requiresIndex = getFieldValue(moduleRequire, "requiresIndex");
            int requiresVersionIndex = getFieldValue(moduleRequire, "requiresVersionIndex");
            String moduleName = constantPool.constantToString(requiresIndex, Const.CONSTANT_Module);
            String version = requiresVersionIndex == 0 ? "0" : constantPool.getConstantString(requiresVersionIndex, Const.CONSTANT_Utf8);
            list.add(new ModuleDeclaration.ModuleInfo(moduleName, requiresFlags, version));
        }
        return list;
    }

    protected List<ModuleDeclaration.PackageInfo> convertModuleOpensToPackageInfo(ModuleOpens[] moduleOpens, ConstantPool constantPool) {
        if (moduleOpens == null || moduleOpens.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.PackageInfo> list = new DefaultList<>(moduleOpens.length);
        DefaultList<String> moduleInfoNames;
        for (ModuleOpens moduleOpen : moduleOpens) {
            int opensToCount = getFieldValue(moduleOpen, "opensToCount");
            int[] opensToIndexes = getFieldValue(moduleOpen, "opensToIndex");
            String[] toModuleNames = new String[opensToCount];
            for (int i = 0; i < opensToCount; i++) {
                toModuleNames[i] = constantPool.getConstantString(opensToIndexes[i], Const.CONSTANT_Module);
            }
            moduleInfoNames = new DefaultList<>(toModuleNames);
            int exportsIndex = getFieldValue(moduleOpen, "opensIndex");
            int opensFlags = getFieldValue(moduleOpen, "opensFlags");
            String packageName = constantPool.constantToString(exportsIndex, Const.CONSTANT_Package);
            list.add(new ModuleDeclaration.PackageInfo(packageName, opensFlags, moduleInfoNames));
        }
        return list;
    }

    protected List<ModuleDeclaration.PackageInfo> convertModuleExportsToPackageInfo(ModuleExports[] moduleExports, ConstantPool constantPool) {
        if (moduleExports == null || moduleExports.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.PackageInfo> list = new DefaultList<>(moduleExports.length);
        DefaultList<String> moduleInfoNames;
        for (ModuleExports moduleExport : moduleExports) {
            int exportsToCount = getFieldValue(moduleExport, "exportsToCount");
            int[] exportsToIndexes = getFieldValue(moduleExport, "exportsToIndex");
            String[] toModuleNames = new String[exportsToCount];
            for (int i = 0; i < exportsToCount; i++) {
                toModuleNames[i] = constantPool.getConstantString(exportsToIndexes[i], Const.CONSTANT_Module);
            }
            moduleInfoNames = new DefaultList<>(toModuleNames);
            int exportsIndex = getFieldValue(moduleExport, "exportsIndex");
            int exportsFlags = getFieldValue(moduleExport, "exportsFlags");
            String packageName = constantPool.constantToString(exportsIndex, Const.CONSTANT_Package);
            list.add(new ModuleDeclaration.PackageInfo(packageName, exportsFlags, moduleInfoNames));
        }
        return list;
    }
    
    protected List<ModuleDeclaration.ServiceInfo> convertModuleProvidesToServiceInfo(ModuleProvides[] moduleProvides, ConstantPool constantPool) {
        if (moduleProvides == null || moduleProvides.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.ServiceInfo> list = new DefaultList<>(moduleProvides.length);
        DefaultList<String> implementationTypeNames;
        for (ModuleProvides serviceInfo : moduleProvides) {
            int providesIndex = getFieldValue(serviceInfo, "providesIndex");
            int providesWithCount = getFieldValue(serviceInfo, "providesWithCount");
            int[] providesWithIndexes = getFieldValue(serviceInfo, "providesWithIndex");
            String[] implementationClassNames = new String[providesWithCount];
            for (int i = 0; i < providesWithCount; i++) {
                implementationClassNames[i] = constantPool.getConstantString(providesWithIndexes[i], Const.CONSTANT_Class);
            }
            implementationTypeNames = new DefaultList<>(implementationClassNames);
            String interfaceName = constantPool.getConstantString(providesIndex, Const.CONSTANT_Class);
            list.add(new ModuleDeclaration.ServiceInfo(interfaceName, implementationTypeNames));
        }
        return list;
    }
}
