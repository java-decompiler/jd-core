/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.processor;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Constants;
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
import org.jd.core.v1.model.classfile.constant.Constant;
import org.jd.core.v1.model.classfile.constant.ConstantDouble;
import org.jd.core.v1.model.classfile.constant.ConstantFloat;
import org.jd.core.v1.model.classfile.constant.ConstantInteger;
import org.jd.core.v1.model.classfile.constant.ConstantLong;
import org.jd.core.v1.model.classfile.constant.ConstantUtf8;
import org.jd.core.v1.model.classfile.constant.ConstantValue;
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
import org.jd.core.v1.model.javasyntax.reference.ElementValue;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jd.core.v1.model.classfile.Constants.ACC_STATIC;

/**
 * Convert ClassFile model to Java syntax model.<br><br>
 *
 * Input:  {@link org.jd.core.v1.model.classfile.ClassFile}<br>
 * Output: {@link org.jd.core.v1.model.javasyntax.CompilationUnit}<br>
 */
public class ConvertClassFileProcessor {
    protected PopulateBindingsWithTypeParameterVisitor populateBindingsWithTypeParameterVisitor = new PopulateBindingsWithTypeParameterVisitor() {
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

    public CompilationUnit process(ClassFile classFile, TypeMaker typeMaker, DecompileContext decompileContext) throws Exception {
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
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.typeParameters, outerClassFileBodyDeclaration);

        return new ClassFileInterfaceDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.thisType.getInternalName(), typeTypes.thisType.getName(),
                typeTypes.typeParameters, typeTypes.interfaces, bodyDeclaration);
    }

    protected ClassFileEnumDeclaration convertEnumDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.typeParameters, outerClassFileBodyDeclaration);

        return new ClassFileEnumDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.thisType.getInternalName(), typeTypes.thisType.getName(),
                typeTypes.interfaces, bodyDeclaration);
    }

    protected ClassFileAnnotationDeclaration convertAnnotationDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.typeParameters, outerClassFileBodyDeclaration);

        return new ClassFileAnnotationDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.thisType.getInternalName(), typeTypes.thisType.getName(),
                bodyDeclaration);
    }

    protected ClassFileClassDeclaration convertClassDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, classFile);
        TypeMaker.TypeTypes typeTypes = parser.parseClassFileSignature(classFile);
        ClassFileBodyDeclaration bodyDeclaration = convertBodyDeclaration(parser, converter, classFile, typeTypes.typeParameters, outerClassFileBodyDeclaration);

        return new ClassFileClassDeclaration(
                annotationReferences, classFile.getAccessFlags(),
                typeTypes.thisType.getInternalName(), typeTypes.thisType.getName(),
                typeTypes.typeParameters, typeTypes.superType,
                typeTypes.interfaces, bodyDeclaration);
    }

    protected ClassFileBodyDeclaration convertBodyDeclaration(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, BaseTypeParameter typeParameters, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        Map<String, TypeArgument> bindings;
        Map<String, BaseType> typeBounds;

        if (!classFile.isStatic() && (outerClassFileBodyDeclaration != null)) {
            bindings = outerClassFileBodyDeclaration.getBindings();
            typeBounds = outerClassFileBodyDeclaration.getTypeBounds();
        } else {
            bindings = Collections.emptyMap();
            typeBounds = Collections.emptyMap();
        }

        if (typeParameters != null) {
            populateBindingsWithTypeParameterVisitor.init(bindings=new HashMap<>(bindings), typeBounds=new HashMap<>(typeBounds));
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
        } else {
            DefaultList<ClassFileFieldDeclaration> list = new DefaultList<>(fields.length);

            for (Field field : fields) {
                BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, field);
                Type typeField = parser.parseFieldSignature(classFile, field);
                ExpressionVariableInitializer variableInitializer = convertFieldInitializer(field, typeField);
                FieldDeclarator fieldDeclarator = new FieldDeclarator(field.getName(), variableInitializer);

                list.add(new ClassFileFieldDeclaration(annotationReferences, field.getAccessFlags(), typeField, fieldDeclarator));
            }

            return list;
        }
    }

    protected List<ClassFileConstructorOrMethodDeclaration> convertMethods(TypeMaker parser, AnnotationConverter converter, ClassFileBodyDeclaration bodyDeclaration, ClassFile classFile) {
        Method[] methods = classFile.getMethods();

        if (methods == null) {
            return null;
        } else {
            DefaultList<ClassFileConstructorOrMethodDeclaration> list = new DefaultList<>(methods.length);

            for (Method method : methods) {
                String name = method.getName();
                BaseAnnotationReference annotationReferences = convertAnnotationReferences(converter, method);
                AttributeAnnotationDefault annotationDefault = method.getAttribute("AnnotationDefault");
                ElementValue defaultAnnotationValue = null;

                if (annotationDefault != null) {
                    defaultAnnotationValue = converter.convert(annotationDefault.getDefaultValue());
                }

                TypeMaker.MethodTypes methodTypes = parser.parseMethodSignature(classFile, method);
                Map<String, TypeArgument> bindings;
                Map<String, BaseType> typeBounds;

                if ((method.getAccessFlags() & ACC_STATIC) == 0) {
                    bindings = bodyDeclaration.getBindings();
                    typeBounds = bodyDeclaration.getTypeBounds();
                } else {
                    bindings = Collections.emptyMap();
                    typeBounds = Collections.emptyMap();
                }

                if (methodTypes.typeParameters != null) {
                    populateBindingsWithTypeParameterVisitor.init(bindings=new HashMap<>(bindings), typeBounds=new HashMap<>(typeBounds));
                    methodTypes.typeParameters.accept(populateBindingsWithTypeParameterVisitor);
               }

                AttributeCode code = method.getAttribute("Code");
                int firstLineNumber = 0;

                if (code != null) {
                    AttributeLineNumberTable lineNumberTable = code.getAttribute("LineNumberTable");
                    if (lineNumberTable != null) {
                        firstLineNumber = lineNumberTable.getLineNumberTable()[0].getLineNumber();
                    }
                }

                if ("<init>".equals(name)) {
                    list.add(new ClassFileConstructorDeclaration(
                            bodyDeclaration, classFile, method, annotationReferences, methodTypes.typeParameters,
                            methodTypes.parameterTypes, methodTypes.exceptionTypes, bindings, typeBounds, firstLineNumber));
                } else if ("<clinit>".equals(name)) {
                    list.add(new ClassFileStaticInitializerDeclaration(bodyDeclaration, classFile, method, bindings, typeBounds, firstLineNumber));
                } else {
                    ClassFileMethodDeclaration methodDeclaration = new ClassFileMethodDeclaration(
                            bodyDeclaration, classFile, method, annotationReferences, name, methodTypes.typeParameters,
                            methodTypes.returnedType, methodTypes.parameterTypes, methodTypes.exceptionTypes, defaultAnnotationValue,
                            bindings, typeBounds, firstLineNumber);
                    if (classFile.isInterface()) {
                        if (methodDeclaration.getFlags() == Constants.ACC_PUBLIC) {
                            // For interfaces, add 'default' access flag on public methods
                            methodDeclaration.setFlags(Declaration.FLAG_PUBLIC|Declaration.FLAG_DEFAULT);
                        }
                    }
                    list.add(methodDeclaration);
                }
            }

            return list;
        }
    }

    protected List<ClassFileTypeDeclaration> convertInnerTypes(TypeMaker parser, AnnotationConverter converter, ClassFile classFile, ClassFileBodyDeclaration outerClassFileBodyDeclaration) {
        List<ClassFile> innerClassFiles = classFile.getInnerClassFiles();

        if (innerClassFiles == null) {
            return null;
        } else {
            DefaultList<ClassFileTypeDeclaration> list = new DefaultList<>(innerClassFiles.size());

            for (ClassFile innerClassFile : innerClassFiles) {
                ClassFileTypeDeclaration innerTypeDeclaration;

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
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, ClassFile classFile) {
        Annotations visibles = classFile.getAttribute("RuntimeVisibleAnnotations");
        Annotations invisibles = classFile.getAttribute("RuntimeInvisibleAnnotations");

        return converter.convert(visibles, invisibles);
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, Field field) {
        Annotations visibles = field.getAttribute("RuntimeVisibleAnnotations");
        Annotations invisibles = field.getAttribute("RuntimeInvisibleAnnotations");

        return converter.convert(visibles, invisibles);
    }

    protected BaseAnnotationReference convertAnnotationReferences(AnnotationConverter converter, Method method) {
        Annotations visibles = method.getAttribute("RuntimeVisibleAnnotations");
        Annotations invisibles = method.getAttribute("RuntimeInvisibleAnnotations");

        return converter.convert(visibles, invisibles);
    }

    protected ExpressionVariableInitializer convertFieldInitializer(Field field, Type typeField) {
        AttributeConstantValue acv = field.getAttribute("ConstantValue");

        if (acv == null) {
            return null;
        } else {
            ConstantValue constantValue = acv.getConstantValue();
            Expression expression;

            switch (constantValue.getTag()) {
                case Constant.CONSTANT_Integer:
                    expression = new IntegerConstantExpression(typeField, ((ConstantInteger)constantValue).getValue());
                    break;
                case Constant.CONSTANT_Float:
                    expression = new FloatConstantExpression(((ConstantFloat)constantValue).getValue());
                    break;
                case Constant.CONSTANT_Long:
                    expression = new LongConstantExpression(((ConstantLong)constantValue).getValue());
                    break;
                case Constant.CONSTANT_Double:
                    expression = new DoubleConstantExpression(((ConstantDouble)constantValue).getValue());
                    break;
                case Constant.CONSTANT_Utf8:
                    expression = new StringConstantExpression(((ConstantUtf8)constantValue).getValue());
                    break;
                default:
                    throw new ConvertClassFileException("Invalid attributes");
            }

            return new ExpressionVariableInitializer(expression);
        }
    }

    protected ModuleDeclaration convertModuleDeclaration(ClassFile classFile) {
        AttributeModule attributeModule = classFile.getAttribute("Module");
        List<ModuleDeclaration.ModuleInfo> requires = convertModuleDeclarationModuleInfo(attributeModule.getRequires());
        List<ModuleDeclaration.PackageInfo> exports = convertModuleDeclarationPackageInfo(attributeModule.getExports());
        List<ModuleDeclaration.PackageInfo> opens = convertModuleDeclarationPackageInfo(attributeModule.getOpens());
        DefaultList<String> uses = new DefaultList<>(attributeModule.getUses());
        List<ModuleDeclaration.ServiceInfo> provides = convertModuleDeclarationServiceInfo(attributeModule.getProvides());

        return new ModuleDeclaration(
                attributeModule.getFlags(), classFile.getInternalTypeName(), attributeModule.getName(),
                attributeModule.getVersion(), requires, exports, opens, uses, provides);
    }

    protected List<ModuleDeclaration.ModuleInfo> convertModuleDeclarationModuleInfo(ModuleInfo[] moduleInfos) {
        if ((moduleInfos == null) || (moduleInfos.length == 0)) {
            return null;
        } else {
            DefaultList<ModuleDeclaration.ModuleInfo> list = new DefaultList<>(moduleInfos.length);

            for (ModuleInfo moduleInfo : moduleInfos) {
                list.add(new ModuleDeclaration.ModuleInfo(moduleInfo.getName(), moduleInfo.getFlags(), moduleInfo.getVersion()));
            }

            return list;
        }
    }

    protected List<ModuleDeclaration.PackageInfo> convertModuleDeclarationPackageInfo(PackageInfo[] packageInfos) {
        if ((packageInfos == null) || (packageInfos.length == 0)) {
            return null;
        } else {
            DefaultList<ModuleDeclaration.PackageInfo> list = new DefaultList<>(packageInfos.length);

            for (PackageInfo packageInfo : packageInfos) {
                DefaultList<String> moduleInfoNames = (packageInfo.getModuleInfoNames() == null) ?
                        null : new DefaultList<String>(packageInfo.getModuleInfoNames());
                list.add(new ModuleDeclaration.PackageInfo(packageInfo.getInternalName(), packageInfo.getFlags(), moduleInfoNames));
            }

            return list;
        }
    }

    protected List<ModuleDeclaration.ServiceInfo> convertModuleDeclarationServiceInfo(ServiceInfo[] serviceInfos) {
        if ((serviceInfos == null) || (serviceInfos.length == 0)) {
            return null;
        } else {
            DefaultList<ModuleDeclaration.ServiceInfo> list = new DefaultList<>(serviceInfos.length);

            for (ServiceInfo serviceInfo : serviceInfos) {
                DefaultList<String> implementationTypeNames = (serviceInfo.getImplementationTypeNames() == null) ?
                        null : new DefaultList<String>(serviceInfo.getImplementationTypeNames());
                list.add(new ModuleDeclaration.ServiceInfo(serviceInfo.getInterfaceTypeName(), implementationTypeNames));
            }

            return list;
        }
    }
}
