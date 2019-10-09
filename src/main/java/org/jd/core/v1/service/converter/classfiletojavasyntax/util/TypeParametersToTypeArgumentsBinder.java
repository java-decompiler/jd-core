/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.BindTypeParametersToTypeArgumentsVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.PopulateBindingsWithTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.PopulateBindingsWithTypeParameterVisitor;
import org.jd.core.v1.util.DefaultList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.jd.core.v1.model.javasyntax.declaration.Declaration.FLAG_STATIC;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;

public class TypeParametersToTypeArgumentsBinder {
    protected PopulateBindingsWithTypeParameterVisitor populateBindingsWithTypeParameterVisitor = new PopulateBindingsWithTypeParameterVisitor();
    protected BindTypeParametersToTypeArgumentsVisitor bindTypeParametersToTypeArgumentsVisitor = new BindTypeParametersToTypeArgumentsVisitor();

    protected TypeMaker typeMaker;
    protected String internalTypeName;
    protected PopulateBindingsWithTypeArgumentVisitor populateBindingsWithTypeArgumentVisitor;
    protected Map<String, TypeArgument> contextualBindings;

    public TypeParametersToTypeArgumentsBinder(
            TypeMaker typeMaker, String internalTypeName, ClassFileBodyDeclaration bodyDeclaration, ClassFileConstructorOrMethodDeclaration comd) {
        this.typeMaker = typeMaker;
        this.internalTypeName = internalTypeName;
        this.populateBindingsWithTypeArgumentVisitor = new PopulateBindingsWithTypeArgumentVisitor(typeMaker);

        if ((comd.getFlags() & FLAG_STATIC) == 0) {
            this.contextualBindings = bodyDeclaration.getBindings();
        } else {
            this.contextualBindings = Collections.emptyMap();
        }

        if (comd.getTypeParameters() != null) {
            HashMap<String, TypeArgument> bindings = new HashMap<>();

            bindings.putAll(this.contextualBindings);

            populateBindingsWithTypeParameterVisitor.init(bindings);
            comd.getTypeParameters().accept(populateBindingsWithTypeParameterVisitor);

            for (HashMap.Entry<String, TypeArgument> entry : bindings.entrySet()) {
                if (entry.getValue() == null) {
                    entry.setValue(new GenericType(entry.getKey()));
                }
            }

            this.contextualBindings = bindings;
        }
    }

    public ClassFileConstructorInvocationExpression newConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {

        BaseType parameterTypes = methodTypes.parameterTypes;
        Map<String, TypeArgument> bindings = contextualBindings;

        if ((parameterTypes != null) && (methodTypes.typeParameters != null)) {
            bindings = new HashMap<>();
            bindings.putAll(contextualBindings);

            populateBindingsWithTypeParameterVisitor.init(bindings);
            methodTypes.typeParameters.accept(populateBindingsWithTypeParameterVisitor);

            if (parameterTypes.size() > 1) {
                Iterator<Expression> parametersIterator = parameters.iterator();
                Iterator<Type> parameterTypesIterator = parameterTypes.iterator();

                while (parametersIterator.hasNext()) {
                    Expression parameter = parametersIterator.next();
                    Type parameterType = parameterTypesIterator.next();

                    if ((parameter.getClass() != ClassFileMethodInvocationExpression.class) || (((ClassFileMethodInvocationExpression) parameter).getTypeParameters() == null)) {
                        populateBindingsWithTypeArgumentVisitor.init(bindings, parameter.getType());
                        parameterType.accept(populateBindingsWithTypeArgumentVisitor);
                    }
                }
            } else if (parameterTypes.size() > 0) {
                Expression parameter = parameters.getFirst();

                if ((parameter.getClass() != ClassFileMethodInvocationExpression.class) || (((ClassFileMethodInvocationExpression) parameter).getTypeParameters() == null)) {
                    populateBindingsWithTypeArgumentVisitor.init(bindings, parameter.getType());
                    parameterTypes.getFirst().accept(populateBindingsWithTypeArgumentVisitor);
                }
            }
        }

        if (!bindings.isEmpty() && !bindings.containsValue(null)) {
            bindTypeParametersToTypeArgumentsVisitor.setBindings(bindings);
            parameterTypes = bindParameterTypesWithArgumentTypes(TYPE_OBJECT, parameterTypes);
        }

        parameters = prepareParameters(parameters, parameterTypes);
        return new ClassFileConstructorInvocationExpression(lineNumber, objectType, descriptor, parameterTypes, parameters);
    }

    public ClassFileSuperConstructorInvocationExpression newSuperConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {

        BaseType parameterTypes = methodTypes.parameterTypes;
        Map<String, TypeArgument> bindings = contextualBindings;
        TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(internalTypeName);

        if ((typeTypes != null) && (typeTypes.superType != null) && (typeTypes.superType.getTypeArguments() != null)) {
            TypeMaker.TypeTypes superTypeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());

            if (superTypeTypes != null) {
                bindings = createBindings(superTypeTypes.typeParameters, typeTypes.superType.getTypeArguments(), methodTypes.typeParameters);
            }
        }

        if (!bindings.isEmpty()) {
            bindTypeParametersToTypeArgumentsVisitor.setBindings(bindings);
            parameterTypes = bindParameterTypesWithArgumentTypes(objectType, methodTypes.parameterTypes);
        }

        parameters = prepareParameters(parameters, parameterTypes);
        return new ClassFileSuperConstructorInvocationExpression(lineNumber, objectType, descriptor, parameterTypes, parameters);
    }

    public ClassFileMethodInvocationExpression newMethodInvocationExpression(
            int lineNumber, Expression expression, ObjectType objectType, String name, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {

        Type returnedType = methodTypes.returnedType;
        BaseType parameterTypes = methodTypes.parameterTypes;

        if (methodTypes.typeParameters == null) {
            Map<String, TypeArgument> bindings = contextualBindings;

            if (expression.getClass() != ThisExpression.class) {
                Type expressionType = expression.getType();

                if (expressionType.isObject()) {
                    ObjectType expressionObjectType = (ObjectType) expressionType;
                    TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());

                    if (typeTypes != null) {
                        BaseTypeParameter typeParameters = typeTypes.typeParameters;
                        BaseTypeArgument typeArguments;

                        if (expression.getClass() == SuperExpression.class) {
                            typeTypes = typeMaker.makeTypeTypes(internalTypeName);
                            typeArguments = typeTypes.superType.getTypeArguments();
                        } else {
                            typeArguments = expressionObjectType.getTypeArguments();
                        }

                        if ((typeParameters != null) && (typeArguments == null)) {
                            Class expressionClass = expression.getClass();

                            if ((expressionClass == ObjectTypeReferenceExpression.class) && name.startsWith("access$")) {
                                // Bridge method => Do not bind
                                bindings = Collections.emptyMap();
                            } else if ((expressionClass == ClassFileMethodInvocationExpression.class) && ((ClassFileMethodInvocationExpression)expression).getName().startsWith("access$")) {
                                // Bridge method => Do not bind
                                bindings = Collections.emptyMap();
                            } else if ((expressionClass == FieldReferenceExpression.class) && ((FieldReferenceExpression)expression).getName().startsWith("this$")) {
                                // Bridge method => Do not bind
                                bindings = Collections.emptyMap();
                            } else {
                                bindings = new HashMap<>();
                                bindings.putAll(contextualBindings);
                                for (TypeParameter typeParameter : typeParameters) {
                                    bindings.put(typeParameter.getIdentifier(), WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                                }
                            }
                        } else {
                            bindings = createBindings(typeParameters, typeArguments, methodTypes.typeParameters);
                        }
                    }
                }
            }

            if (!bindings.isEmpty()) {
                bindTypeParametersToTypeArgumentsVisitor.setBindings(bindings);
                returnedType = (Type) bindParameterTypesWithArgumentTypes(objectType, methodTypes.returnedType);
                parameterTypes = bindParameterTypesWithArgumentTypes(objectType, methodTypes.parameterTypes);
            }

            parameters = prepareParameters(parameters, parameterTypes);
        }

        return new ClassFileMethodInvocationExpression(
                this, lineNumber, methodTypes.typeParameters, returnedType, expression,
                objectType.getInternalName(), name, descriptor, parameterTypes, parameters);
    }

    public FieldReferenceExpression newFieldReferenceExpression(
            int lineNumber, Type type, Expression expression, ObjectType objectType, String name, String descriptor) {

        if (type.isObject()) {
            ObjectType ot = (ObjectType)type;

            if (ot.getTypeArguments() != null) {
                Map<String, TypeArgument> bindings = contextualBindings;

                if (expression.getClass() != ThisExpression.class) {
                    Type expressionType = expression.getType();

                    if (expressionType.isObject()) {
                        ObjectType expressionObjectType = (ObjectType) expressionType;
                        TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(expressionObjectType.getInternalName());

                        if (typeTypes != null) {
                            BaseTypeParameter typeParameters = typeTypes.typeParameters;
                            BaseTypeArgument typeArguments = expressionObjectType.getTypeArguments();

                            if ((typeParameters != null) && (typeArguments == null)) {
                                bindings = new HashMap<>();
                                bindings.putAll(contextualBindings);
                                for (TypeParameter typeParameter : typeParameters) {
                                    bindings.put(typeParameter.getIdentifier(), WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                                }
                            } else {
                                bindings = createBindings(typeParameters, typeArguments, null);
                            }
                        }
                    }
                }

                if (!bindings.isEmpty()) {
                    bindTypeParametersToTypeArgumentsVisitor.setBindings(bindings);
                    type = (Type) bindParameterTypesWithArgumentTypes(objectType, type);
                }
            }
        }

        return new FieldReferenceExpression(lineNumber, type, expression, objectType.getInternalName(), name, descriptor);
    }

    public void updateNewExpression(Expression expression, String descriptor, TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        ClassFileNewExpression ne = (ClassFileNewExpression)expression;
        BaseType parameterTypes = methodTypes.parameterTypes;
        Map<String, TypeArgument> bindings = contextualBindings;

        if (!ne.getObjectType().getInternalName().equals(internalTypeName)) {
            if (parameterTypes != null) {
                TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(ne.getObjectType().getInternalName());

                if ((typeTypes != null) && (typeTypes.typeParameters != null)) {
                    bindings = new HashMap<>();
                    bindings.putAll(contextualBindings);

                    populateBindingsWithTypeParameterVisitor.init(bindings);
                    typeTypes.typeParameters.accept(populateBindingsWithTypeParameterVisitor);

                    if (parameterTypes.size() > 1) {
                        Iterator<Expression> parametersIterator = parameters.iterator();
                        Iterator<Type> parameterTypesIterator = parameterTypes.iterator();

                        while (parametersIterator.hasNext()) {
                            Expression parameter = parametersIterator.next();
                            Type parameterType = parameterTypesIterator.next();

                            if ((parameter.getClass() != ClassFileMethodInvocationExpression.class) || (((ClassFileMethodInvocationExpression) parameter).getTypeParameters() == null)) {
                                populateBindingsWithTypeArgumentVisitor.init(bindings, parameter.getType());
                                parameterType.accept(populateBindingsWithTypeArgumentVisitor);
                            }
                        }
                    } else if (parameterTypes.size() > 0) {
                        Expression parameter = parameters.getFirst();

                        if ((parameter.getClass() != ClassFileMethodInvocationExpression.class) || (((ClassFileMethodInvocationExpression) parameter).getTypeParameters() == null)) {
                            populateBindingsWithTypeArgumentVisitor.init(bindings, parameter.getType());
                            parameterTypes.getFirst().accept(populateBindingsWithTypeArgumentVisitor);
                        }
                    }

                    if (!bindings.containsValue(null)) {
                        bindTypeParametersToTypeArgumentsVisitor.setBindings(bindings);

                        if (typeTypes.typeParameters.isList()) {
                            TypeArguments tas = new TypeArguments(typeTypes.typeParameters.size());
                            boolean object = true;

                            for (TypeParameter typeParameter : typeTypes.typeParameters) {
                                bindTypeParametersToTypeArgumentsVisitor.init();
                                new GenericType(typeParameter.getIdentifier()).accept(bindTypeParametersToTypeArgumentsVisitor);
                                BaseType baseType = bindTypeParametersToTypeArgumentsVisitor.getType();
                                object &= TYPE_OBJECT.equals(baseType);
                                tas.add((Type) baseType);
                            }

                            if (!object) {
                                ne.setType(ne.getObjectType().createType(tas));
                            }
                        } else {
                            bindTypeParametersToTypeArgumentsVisitor.init();
                            new GenericType(typeTypes.typeParameters.getFirst().getIdentifier()).accept(bindTypeParametersToTypeArgumentsVisitor);
                            BaseType baseType = bindTypeParametersToTypeArgumentsVisitor.getType();

                            if (!TYPE_OBJECT.equals(baseType)) {
                                ne.setType(ne.getObjectType().createType((Type)baseType));
                            }
                        }
                    }
                }
            }

            if (!bindings.isEmpty() && !bindings.containsValue(null)) {
                bindTypeParametersToTypeArgumentsVisitor.setBindings(bindings);
                parameterTypes = bindParameterTypesWithArgumentTypes(ne.getObjectType(), parameterTypes);
            }
        }

        parameters = prepareParameters(parameters, parameterTypes);
        ((ClassFileNewExpression)expression).set(descriptor, parameterTypes, parameters);
    }

    protected Map<String, TypeArgument> createBindings(BaseTypeParameter typeParameters, BaseTypeArgument typeArguments, BaseTypeParameter methodTypeParameters) {
        if ((typeParameters == null) && (methodTypeParameters == null)) {
            return contextualBindings;
        } else {
            HashMap<String, TypeArgument> bindings = new HashMap<>();

            bindings.putAll(contextualBindings);

            if ((typeParameters != null) && (typeArguments != null)) {
                if (typeParameters.isList()) {
                    Iterator<TypeParameter> iteratorTypeParameter = typeParameters.iterator();
                    Iterator<TypeArgument> iteratorTypeArgument = typeArguments.getTypeArgumentList().iterator();

                    while (iteratorTypeParameter.hasNext()) {
                        bindings.put(iteratorTypeParameter.next().getIdentifier(), iteratorTypeArgument.next());
                    }
                } else {
                    bindings.put(typeParameters.getFirst().getIdentifier(), typeArguments.getTypeArgumentFirst());
                }
            }

            if (methodTypeParameters != null) {
                if (methodTypeParameters.isList()) {
                    for (TypeParameter tp : methodTypeParameters) {
                        String identifier = tp.getIdentifier();
                        bindings.put(identifier, new GenericType(identifier));
                    }
                } else {
                    String identifier = methodTypeParameters.getFirst().getIdentifier();
                    bindings.put(identifier, new GenericType(identifier));
                }
            }

            return bindings;
        }
    }

    protected BaseType bindParameterTypesWithArgumentTypes(ObjectType objectType, BaseType baseType) {
        if (baseType == null) {
            return null;
        }

        if (objectType.getInternalName().equals(internalTypeName)) {
            return baseType;
        }

        bindTypeParametersToTypeArgumentsVisitor.init();
        baseType.accept(bindTypeParametersToTypeArgumentsVisitor);

        return bindTypeParametersToTypeArgumentsVisitor.getType();
    }

    @SuppressWarnings("unchecked")
    protected BaseExpression prepareParameters(BaseExpression parameterExpressions, BaseType parameterTypes) {
        if (parameterTypes != null) {
            int size = parameterTypes.size();

            if (size == 1) {
                Expression parameter = parameterExpressions.getFirst();
                Type type = parameterTypes.getFirst();

                if (parameter.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                    AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) parameter).getLocalVariable();
                    localVariable.typeOnLeft(checkTypeArguments(type, localVariable));
                }

                bindParameterTypesWithArgumentTypes(type, parameter);
            } else if (size > 1) {
                DefaultList<Expression> parameters = parameterExpressions.getList();
                DefaultList<Type> types = parameterTypes.getList();

                for (int i=0; i<size; i++) {
                    Expression parameter = parameters.get(i);
                    Type type = types.get(i);

                    if (parameter.getClass() == ClassFileLocalVariableReferenceExpression.class) {
                        AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) parameter).getLocalVariable();
                        localVariable.typeOnLeft(checkTypeArguments(type, localVariable));
                    }

                    bindParameterTypesWithArgumentTypes(type, parameter);
                }
            }
        }

        return parameterExpressions;
    }

    protected Type checkTypeArguments(Type type, AbstractLocalVariable localVariable) {
        if (type.isObject()) {
            ObjectType objectType = (ObjectType)type;

            if (objectType.getTypeArguments() != null) {
                Type localVariableType = localVariable.getType();

                if (localVariableType.isObject()) {
                    ObjectType localVariableObjectType = (ObjectType)localVariableType;
                    TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(localVariableObjectType.getInternalName());

                    if ((typeTypes != null) && (typeTypes.typeParameters == null)) {
                        type = ((ObjectType)type).createType(null);
                    }
                }
            }
        }

        return type;
    }

    public void bindParameterTypesWithArgumentTypes(Type type, Expression expression) {
        Class expressionClass = expression.getClass();

        if (expressionClass == ClassFileMethodInvocationExpression.class) {
            ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression)expression;

            if (mie.getTypeParameters() != null) {
                bindParameterTypesWithArgumentTypes(type, mie);
            }
        } else if (expressionClass == ClassFileNewExpression.class) {
            if (contextualBindings != null) {
                ClassFileNewExpression ne = (ClassFileNewExpression) expression;

                if (ne.getParameterTypes() != null) {
                    bindTypeParametersToTypeArgumentsVisitor.setBindings(contextualBindings);

                    bindTypeParametersToTypeArgumentsVisitor.init();
                    ne.getParameterTypes().accept(bindTypeParametersToTypeArgumentsVisitor);
                    ne.setParameterTypes(bindTypeParametersToTypeArgumentsVisitor.getType());
                }
            }
        }
    }

    protected void bindParameterTypesWithArgumentTypes(Type type, ClassFileMethodInvocationExpression mie) {
        HashMap<String, TypeArgument> bindings = new HashMap<>();

        if (mie.getExpression().getClass() != ObjectTypeReferenceExpression.class) {
            // Non-static method invocation
            bindings.putAll(contextualBindings);
        }

        if (mie.getTypeParameters() != null) {
            populateBindingsWithTypeParameterVisitor.init(bindings);
            mie.getTypeParameters().accept(populateBindingsWithTypeParameterVisitor);
        }

        if (type != PrimitiveType.TYPE_VOID) {
            populateBindingsWithTypeArgumentVisitor.init(bindings, type);
            mie.getType().accept(populateBindingsWithTypeArgumentVisitor);
        }

        if (mie.getParameterTypes() != null) {
            BaseType parameterTypes = mie.getParameterTypes();

            if (parameterTypes.size() > 1) {
                Iterator<Expression> parametersIterator = mie.getParameters().iterator();
                Iterator<Type> parameterTypesIterator = mie.getParameterTypes().iterator();

                while (parametersIterator.hasNext()) {
                    Expression parameter = parametersIterator.next();
                    Type parameterType = parameterTypesIterator.next();

                    if ((parameter.getClass() != ClassFileMethodInvocationExpression.class) || (((ClassFileMethodInvocationExpression)parameter).getTypeParameters() == null)) {
                        populateBindingsWithTypeArgumentVisitor.init(bindings, parameter.getType());
                        parameterType.accept(populateBindingsWithTypeArgumentVisitor);
                    }
                }
            } else if (parameterTypes.size() > 0) {
                Expression parameter = mie.getParameters().getFirst();

                if ((parameter.getClass() != ClassFileMethodInvocationExpression.class) || (((ClassFileMethodInvocationExpression)parameter).getTypeParameters() == null)) {
                    populateBindingsWithTypeArgumentVisitor.init(bindings, parameter.getType());
                    parameterTypes.getFirst().accept(populateBindingsWithTypeArgumentVisitor);
                }
            }
        }

        assert !bindings.isEmpty() : "TypeParametersToTypeArgumentsBinder.bindParameterTypesWithArgumentTypes(...): Bindings is empty";

        bindTypeParametersToTypeArgumentsVisitor.setBindings(bindings);

        bindTypeParametersToTypeArgumentsVisitor.init();
        mie.getType().accept(bindTypeParametersToTypeArgumentsVisitor);
        mie.setType((Type) bindTypeParametersToTypeArgumentsVisitor.getType());

        if (mie.getParameterTypes() != null) {
            bindTypeParametersToTypeArgumentsVisitor.init();
            mie.getParameterTypes().accept(bindTypeParametersToTypeArgumentsVisitor);
            mie.setParameterTypes(bindTypeParametersToTypeArgumentsVisitor.getType());

            BaseType parameterTypes = mie.getParameterTypes();

            if (parameterTypes.size() > 1) {
                Iterator<Expression> parametersIterator = mie.getParameters().iterator();
                Iterator<Type> parameterTypesIterator = mie.getParameterTypes().iterator();

                while (parametersIterator.hasNext()) {
                    bindParameterTypesWithArgumentTypes(parameterTypesIterator.next(), parametersIterator.next());
                }
            } else if (parameterTypes.size() > 0) {
                bindParameterTypesWithArgumentTypes(parameterTypes.getFirst(), mie.getParameters().getFirst());
            }
        }

        bindParameterTypesWithArgumentTypes(mie.getType(), mie.getExpression());
    }

    public static void staticBindParameterTypesWithArgumentTypes(Type type, Expression expression) {
        if (expression.getClass() == ClassFileMethodInvocationExpression.class) {
            ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression)expression;
            TypeParametersToTypeArgumentsBinder binder = mie.getBinder();

            if (binder != null) {
                binder.bindParameterTypesWithArgumentTypes(type, mie);
            }
        }
    }
}
