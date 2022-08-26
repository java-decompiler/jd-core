/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.processor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantUtf8;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Field;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.classfile.attribute.Annotations;
import org.jd.core.v1.model.classfile.attribute.AttributeAnnotationDefault;
import org.jd.core.v1.model.classfile.attribute.AttributeCode;
import org.jd.core.v1.model.classfile.attribute.AttributeConstantValue;
import org.jd.core.v1.model.classfile.attribute.AttributeLineNumberTable;
import org.jd.core.v1.model.classfile.attribute.AttributeModule;
import org.jd.core.v1.model.classfile.attribute.ModuleInfo;
import org.jd.core.v1.model.classfile.attribute.PackageInfo;
import org.jd.core.v1.model.classfile.attribute.ServiceInfo;
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

        if (fields == null) {
            return null;
        }
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

        if (methods == null) {
            return null;
        }
        DefaultList<ClassFileConstructorOrMethodDeclaration> list = new DefaultList<>(methods.length);
        String name;
        BaseAnnotationReference annotationReferences;
        AttributeAnnotationDefault annotationDefault;
        BaseElementValue defaultAnnotationValue;
        TypeMaker.MethodTypes methodTypes;
        Map<String, TypeArgument> bindings;
        Map<String, BaseType> typeBounds;
        AttributeCode code;
        int firstLineNumber;
        for (Method method : methods) {
            name = method.getName();
            annotationReferences = convertAnnotationReferences(converter, method);
            annotationDefault = method.getAttribute("AnnotationDefault");
            defaultAnnotationValue = null;

            if (annotationDefault != null) {
                defaultAnnotationValue = converter.convert(annotationDefault.defaultValue());
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

            code = method.getAttribute("Code");
            firstLineNumber = 0;

            if (code != null) {
                AttributeLineNumberTable lineNumberTable = code.getAttribute("LineNumberTable");
                if (lineNumberTable != null) {
                    firstLineNumber = lineNumberTable.getLineNumberTable(0).getLineNumber();
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
        Annotations visibles = classFile.getAttribute(StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
        Annotations invisibles = classFile.getAttribute(StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME);

        return converter.convert(visibles, invisibles);
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, Field field) {
        Annotations visibles = field.getAttribute(StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
        Annotations invisibles = field.getAttribute(StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME);

        return converter.convert(visibles, invisibles);
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, Method method) {
        Annotations visibles = method.getAttribute(StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
        Annotations invisibles = method.getAttribute(StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME);

        return converter.convert(visibles, invisibles);
    }

    protected ExpressionVariableInitializer convertFieldInitializer(Field field, Type typeField) {
        AttributeConstantValue acv = field.getAttribute("ConstantValue");
        if (acv == null) {
            return null;
        }
        Constant constantValue = acv.constantValue();
        Expression expression = switch (constantValue.getTag()) {
            case Const.CONSTANT_Integer -> new IntegerConstantExpression(typeField, ((ConstantInteger)constantValue).getBytes());
            case Const.CONSTANT_Float -> new FloatConstantExpression(((ConstantFloat)constantValue).getBytes());
            case Const.CONSTANT_Long -> new LongConstantExpression(((ConstantLong)constantValue).getBytes());
            case Const.CONSTANT_Double -> new DoubleConstantExpression(((ConstantDouble)constantValue).getBytes());
            case Const.CONSTANT_Utf8 -> new StringConstantExpression(((ConstantUtf8)constantValue).getBytes());
            default -> throw new ConvertClassFileException("Invalid attributes");
        };
        return new ExpressionVariableInitializer(expression);
    }

    protected ModuleDeclaration convertModuleDeclaration(ClassFile classFile) {
        AttributeModule attributeModule = classFile.getAttribute("Module");
        List<ModuleDeclaration.ModuleInfo> requires = convertModuleDeclarationModuleInfo(attributeModule.requires());
        List<ModuleDeclaration.PackageInfo> exports = convertModuleDeclarationPackageInfo(attributeModule.exports());
        List<ModuleDeclaration.PackageInfo> opens = convertModuleDeclarationPackageInfo(attributeModule.opens());
        DefaultList<String> uses = new DefaultList<>(attributeModule.uses());
        List<ModuleDeclaration.ServiceInfo> provides = convertModuleDeclarationServiceInfo(attributeModule.provides());

        return new ModuleDeclaration(
                attributeModule.flags(), classFile.getInternalTypeName(), attributeModule.name(),
                attributeModule.version(), requires, exports, opens, uses, provides);
    }

    protected List<ModuleDeclaration.ModuleInfo> convertModuleDeclarationModuleInfo(ModuleInfo[] moduleInfos) {
        if (moduleInfos == null || moduleInfos.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.ModuleInfo> list = new DefaultList<>(moduleInfos.length);
        for (ModuleInfo moduleInfo : moduleInfos) {
            list.add(new ModuleDeclaration.ModuleInfo(moduleInfo.name(), moduleInfo.flags(), moduleInfo.version()));
        }
        return list;
    }

    protected List<ModuleDeclaration.PackageInfo> convertModuleDeclarationPackageInfo(PackageInfo[] packageInfos) {
        if (packageInfos == null || packageInfos.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.PackageInfo> list = new DefaultList<>(packageInfos.length);
        DefaultList<String> moduleInfoNames;
        for (PackageInfo packageInfo : packageInfos) {
            moduleInfoNames = packageInfo.moduleInfoNames() == null ?
                    null : new DefaultList<>(packageInfo.moduleInfoNames());
            list.add(new ModuleDeclaration.PackageInfo(packageInfo.internalName(), packageInfo.flags(), moduleInfoNames));
        }
        return list;
    }

    protected List<ModuleDeclaration.ServiceInfo> convertModuleDeclarationServiceInfo(ServiceInfo[] serviceInfos) {
        if (serviceInfos == null || serviceInfos.length == 0) {
            return null;
        }
        DefaultList<ModuleDeclaration.ServiceInfo> list = new DefaultList<>(serviceInfos.length);
        DefaultList<String> implementationTypeNames;
        for (ServiceInfo serviceInfo : serviceInfos) {
            implementationTypeNames = serviceInfo.implementationTypeNames() == null ?
                    null : new DefaultList<>(serviceInfo.implementationTypeNames());
            list.add(new ModuleDeclaration.ServiceInfo(serviceInfo.interfaceTypeName(), implementationTypeNames));
        }
        return list;
    }
}
