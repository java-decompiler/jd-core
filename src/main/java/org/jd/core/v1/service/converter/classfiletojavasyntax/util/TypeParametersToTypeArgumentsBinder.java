/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.*;

import java.util.*;

import static org.jd.core.v1.model.javasyntax.declaration.Declaration.FLAG_STATIC;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;

public class TypeParametersToTypeArgumentsBinder {
    protected static final RemoveNonWildcardTypeArgumentsVisitor REMOVE_NON_WILDCARD_TYPE_ARGUMENTS_VISITOR = new RemoveNonWildcardTypeArgumentsVisitor();

    protected PopulateBindingsWithTypeParameterVisitor populateBindingsWithTypeParameterVisitor = new PopulateBindingsWithTypeParameterVisitor();
    protected BindTypesToTypesVisitor bindTypesToTypesVisitor = new BindTypesToTypesVisitor();
    protected SearchInTypeArgumentVisitor searchInTypeArgumentVisitor = new SearchInTypeArgumentVisitor();
    protected TypeArgumentToTypeVisitor typeArgumentToTypeVisitor = new TypeArgumentToTypeVisitor();
    protected BaseTypeToTypeArgumentVisitor baseTypeToTypeArgumentVisitor = new BaseTypeToTypeArgumentVisitor();
    protected BindTypeParametersToNonWildcardTypeArgumentsVisitor bindTypeParametersToNonWildcardTypeArgumentsVisitor = new BindTypeParametersToNonWildcardTypeArgumentsVisitor();
    protected BindVisitor bindVisitor = new BindVisitor();

    protected TypeMaker typeMaker;
    protected String internalTypeName;
    protected boolean staticMethod;
    protected PopulateBindingsWithTypeArgumentVisitor populateBindingsWithTypeArgumentVisitor;
    protected Map<String, TypeArgument> contextualBindings;
    protected Map<String, BaseType> contextualTypeBounds;

    public TypeParametersToTypeArgumentsBinder(TypeMaker typeMaker, String internalTypeName, ClassFileConstructorOrMethodDeclaration comd) {
        this.typeMaker = typeMaker;
        this.internalTypeName = internalTypeName;
        this.staticMethod = ((comd.getFlags() & FLAG_STATIC) != 0);
        this.populateBindingsWithTypeArgumentVisitor = new PopulateBindingsWithTypeArgumentVisitor(typeMaker);
        this.contextualBindings = comd.getBindings();
        this.contextualTypeBounds = comd.getTypeBounds();
    }

    public ClassFileConstructorInvocationExpression newConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {

        BaseType parameterTypes = clone(methodTypes.parameterTypes);
        Map<String, TypeArgument> bindings = createBindings(null, null, null, methodTypes.typeParameters, TYPE_OBJECT, null, parameterTypes, parameters);

        parameterTypes = bind(bindings, parameterTypes);
        bindParameters(parameterTypes, parameters);

        return new ClassFileConstructorInvocationExpression(lineNumber, objectType, descriptor, parameterTypes, parameters);
    }

    public ClassFileSuperConstructorInvocationExpression newSuperConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {

        BaseType parameterTypes = clone(methodTypes.parameterTypes);
        Map<String, TypeArgument> bindings = contextualBindings;
        TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(internalTypeName);

        if ((typeTypes != null) && (typeTypes.superType != null) && (typeTypes.superType.getTypeArguments() != null)) {
            TypeMaker.TypeTypes superTypeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());

            if (superTypeTypes != null) {
                BaseTypeParameter typeParameters = superTypeTypes.typeParameters;
                BaseTypeArgument typeArguments = typeTypes.superType.getTypeArguments();
                BaseTypeParameter methodTypeParameters = methodTypes.typeParameters;

                bindings = createBindings(null, typeParameters, typeArguments, methodTypeParameters, TYPE_OBJECT, null, parameterTypes, parameters);
            }
        }

        parameterTypes = bind(bindings, parameterTypes);
        bindParameters(parameterTypes, parameters);

        return new ClassFileSuperConstructorInvocationExpression(lineNumber, objectType, descriptor, parameterTypes, parameters);
    }

    public ClassFileMethodInvocationExpression newMethodInvocationExpression(
            int lineNumber, Expression expression, ObjectType objectType, String name, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        return new ClassFileMethodInvocationExpression(
            this, lineNumber, methodTypes.typeParameters, methodTypes.returnedType, expression,
            objectType.getInternalName(), name, descriptor, clone(methodTypes.parameterTypes), parameters);
    }

    public FieldReferenceExpression newFieldReferenceExpression(
            int lineNumber, Type type, Expression expression, ObjectType objectType, String name, String descriptor) {

        Type expressionType = expression.getType();

        if (expressionType.isObject()) {
            ObjectType expressionObjectType = (ObjectType) expressionType;

            if (staticMethod || !expressionObjectType.getInternalName().equals(internalTypeName)) {
                if (type.isObject()) {
                    ObjectType ot = (ObjectType) type;

                    if (ot.getTypeArguments() != null) {
                        Map<String, TypeArgument> bindings;
                        TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(expressionObjectType.getInternalName());

                        if (typeTypes == null) {
                            bindings = contextualBindings;
                        } else {
                            BaseTypeParameter typeParameters = typeTypes.typeParameters;
                            BaseTypeArgument typeArguments = expressionObjectType.getTypeArguments();

                            bindings = createBindings(expression, typeParameters, typeArguments, null, TYPE_OBJECT, null, null, null);
                        }

                        type = (Type) bind(bindings, type);
                    }
                }
            }
        }

        return new FieldReferenceExpression(lineNumber, type, expression, objectType.getInternalName(), name, descriptor);
    }

    public void updateNewExpression(ClassFileNewExpression ne, String descriptor, TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        ne.set(descriptor, clone(methodTypes.parameterTypes), parameters);
    }

    public void bindParameterTypesWithArgumentTypes(Type type, Expression expression) {
        bindVisitor.init(type);
        expression.accept(bindVisitor);
        expression.accept(REMOVE_NON_WILDCARD_TYPE_ARGUMENTS_VISITOR);
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

    protected void bind(Type type, ClassFileMethodInvocationExpression mie) {
        BaseType parameterTypes = mie.getParameterTypes();
        BaseExpression parameters = mie.getParameters();
        Expression expression = mie.getExpression();
        Type expressionType = expression.getType();

        if (staticMethod || (mie.getTypeParameters() != null) || expressionType.isGeneric() || (expression.getClass() != ThisExpression.class)) {
            TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(mie.getInternalTypeName());

            if (typeTypes != null) {
                BaseTypeParameter typeParameters = typeTypes.typeParameters;
                BaseTypeParameter methodTypeParameters = mie.getTypeParameters();
                BaseTypeArgument typeArguments;

                if (expression.getClass() == SuperExpression.class) {
                    typeTypes = typeMaker.makeTypeTypes(internalTypeName);
                    typeArguments = (typeTypes.superType == null) ? null : typeTypes.superType.getTypeArguments();
                } else if (expression.getClass() == ClassFileMethodInvocationExpression.class) {
                    Type t = getExpressionType((ClassFileMethodInvocationExpression) expression);
                    if ((t != null) && t.isObject()) {
                        typeArguments = ((ObjectType)t).getTypeArguments();
                    } else {
                        typeArguments = null;
                    }
                } else if (expressionType.isGeneric()) {
                    typeArguments = null;
                } else {
                    typeArguments = ((ObjectType)expressionType).getTypeArguments();
                }

                Type t = mie.getType();

                if (type.isObject() && t.isObject()) {
                    ObjectType objectType = (ObjectType) type;
                    ObjectType mieTypeObjectType = (ObjectType) t;
                    t = typeMaker.searchSuperParameterizedType(objectType, mieTypeObjectType);
                }

                Map<String, TypeArgument> bindings = createBindings(expression, typeParameters, typeArguments, methodTypeParameters, type, t, parameterTypes, parameters);
                boolean bindingsContainsNull = bindings.containsValue(null);

                mie.setParameterTypes(parameterTypes = bind(bindings, parameterTypes));
                mie.setType((Type) bind(bindings, mie.getType()));

                if ((methodTypeParameters != null) && !bindingsContainsNull) {
                    bindTypeParametersToNonWildcardTypeArgumentsVisitor.init(bindings);
                    methodTypeParameters.accept(bindTypeParametersToNonWildcardTypeArgumentsVisitor);
                    mie.setNonWildcardTypeArguments(bindTypeParametersToNonWildcardTypeArgumentsVisitor.getTypeArgument());
                }

                if (expressionType.isObject()) {
                    ObjectType expressionObjectType = (ObjectType) expressionType;

                    if (bindings.isEmpty() || bindingsContainsNull) {
                        expressionType = expressionObjectType.createType(null);
                    } else {
                        boolean statik = (expression.getClass() == ObjectTypeReferenceExpression.class);

                        if (statik || (typeParameters == null)) {
                            expressionType = expressionObjectType.createType(null);
                        } else if (typeParameters.isList()) {
                            TypeArguments tas = new TypeArguments(typeParameters.size());
                            for (TypeParameter typeParameter : typeParameters) {
                                tas.add(bindings.get(typeParameter.getIdentifier()));
                            }
                            expressionType = expressionObjectType.createType(tas);
                        } else {
                            expressionType = expressionObjectType.createType(bindings.get(typeParameters.getFirst().getIdentifier()));
                        }
                    }
                } else if (expressionType.isGeneric()) {
                    if (bindings.isEmpty() || bindingsContainsNull) {
                        expressionType = ObjectType.TYPE_OBJECT;
                    } else {
                        typeArgumentToTypeVisitor.init();
                        bindings.get(expressionType.getName()).accept(typeArgumentToTypeVisitor);
                        expressionType = typeArgumentToTypeVisitor.getType();
                    }
                }
            }
        }

        bindVisitor.init(expressionType);
        expression.accept(bindVisitor);

        bindParameters(parameterTypes, parameters);
    }

    protected void bind(Type type, ClassFileNewExpression ne) {
        BaseType parameterTypes = ne.getParameterTypes();
        BaseExpression parameters = ne.getParameters();
        ObjectType neObjectType = ne.getObjectType();

        if (staticMethod || !neObjectType.getInternalName().equals(internalTypeName)) {
            TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(neObjectType.getInternalName());

            if (typeTypes != null) {
                BaseTypeParameter typeParameters = typeTypes.typeParameters;
                BaseTypeArgument typeArguments = neObjectType.getTypeArguments();

                if ((typeParameters != null) && (typeArguments == null)) {
                    if (typeParameters.isList()) {
                        TypeArguments tas = new TypeArguments(typeParameters.size());
                        for (TypeParameter typeParameter : typeParameters) {
                            tas.add(new GenericType(typeParameter.getIdentifier()));
                        }
                        neObjectType = neObjectType.createType(tas);
                    } else {
                        neObjectType = neObjectType.createType(new GenericType(typeParameters.getFirst().getIdentifier()));
                    }
                }

                Type t = neObjectType;

                if (type.isObject()) {
                    ObjectType objectType = (ObjectType)type;
                    t = typeMaker.searchSuperParameterizedType(objectType, neObjectType);
                }

                Map<String, TypeArgument> bindings = createBindings(null, typeParameters, typeArguments, null, type, t, parameterTypes, parameters);

                ne.setParameterTypes(parameterTypes = bind(bindings, parameterTypes));

                // Replace wildcards
                for (Map.Entry<String, TypeArgument> entry : bindings.entrySet()) {
                    typeArgumentToTypeVisitor.init();
                    entry.getValue().accept(typeArgumentToTypeVisitor);
                    entry.setValue(typeArgumentToTypeVisitor.getType());
                }

                ne.setType((ObjectType) bind(bindings, neObjectType));
            }
        }

        bindParameters(parameterTypes, parameters);
    }

    protected void bindParameters(BaseType parameterTypes, BaseExpression parameters) {
        if (parameterTypes != null) {
            if (parameterTypes.isList() && parameters.isList()) {
                Iterator<Type> parameterTypesIterator = parameterTypes.iterator();
                Iterator<Expression> parametersIterator = parameters.iterator();

                while (parametersIterator.hasNext()) {
                    Expression parameter = parametersIterator.next();
                    bindVisitor.init(parameterTypesIterator.next());
                    parameter.accept(bindVisitor);
                    parameter.accept(REMOVE_NON_WILDCARD_TYPE_ARGUMENTS_VISITOR);
                }
            } else {
                Expression parameter = parameters.getFirst();
                bindVisitor.init(parameterTypes.getFirst());
                parameter.accept(bindVisitor);
                parameter.accept(REMOVE_NON_WILDCARD_TYPE_ARGUMENTS_VISITOR);
            }
        }
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

    protected Map<String, TypeArgument> createBindings(
            Expression expression,
            BaseTypeParameter typeParameters, BaseTypeArgument typeArguments, BaseTypeParameter methodTypeParameters,
            Type returnType, Type returnExpressionType, BaseType parameterTypes, BaseExpression parameters) {

        Map<String, TypeArgument> bindings = new HashMap<>();
        Map<String, BaseType> typeBounds = new HashMap<>();
        boolean statik = (expression != null) && (expression.getClass() == ObjectTypeReferenceExpression.class);

        if (!statik) {
            bindings.putAll(contextualBindings);

            if (typeParameters != null) {
                populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
                typeParameters.accept(populateBindingsWithTypeParameterVisitor);

                if (typeArguments != null) {
                    if (typeParameters.isList() && typeArguments.isTypeArgumentList()) {
                        Iterator<TypeParameter> iteratorTypeParameter = typeParameters.iterator();
                        Iterator<TypeArgument> iteratorTypeArgument = typeArguments.getTypeArgumentList().iterator();

                        while (iteratorTypeParameter.hasNext()) {
                            bindings.put(iteratorTypeParameter.next().getIdentifier(), iteratorTypeArgument.next());
                        }
                    } else {
                        bindings.put(typeParameters.getFirst().getIdentifier(), typeArguments.getTypeArgumentFirst());
                    }
                }
            }
        }

        if (methodTypeParameters != null) {
            populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
            methodTypeParameters.accept(populateBindingsWithTypeParameterVisitor);
        }

        if (!TYPE_OBJECT.equals(returnType) && (returnExpressionType != null)) {
            populateBindingsWithTypeArgumentVisitor.init(contextualTypeBounds, bindings, typeBounds, returnType);
            returnExpressionType.accept(populateBindingsWithTypeArgumentVisitor);
        }

        if (parameterTypes != null) {
            if (parameterTypes.isList() && parameters.isList()) {
                Iterator<Type> parameterTypesIterator = parameterTypes.iterator();
                Iterator<Expression> parametersIterator = parameters.iterator();

                while (parametersIterator.hasNext()) {
                    populateBindingsWithTypeArgument(bindings, typeBounds, parameterTypesIterator.next(), parametersIterator.next());
                }
            } else {
                populateBindingsWithTypeArgument(bindings, typeBounds, parameterTypes.getFirst(), parameters.getFirst());
            }
        }

        if (bindings.containsValue(null)) {
            if (eraseTypeArguments(expression, typeParameters, typeArguments)) {
                for (Map.Entry<String, TypeArgument> entry : bindings.entrySet()) {
                    entry.setValue(null);
                }
            } else {
                for (Map.Entry<String, TypeArgument> entry : bindings.entrySet()) {
                    if (entry.getValue() == null) {
                        BaseType baseType = typeBounds.get(entry.getKey());

                        if (baseType == null) {
                            entry.setValue(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                        } else {
                            bindTypesToTypesVisitor.setBindings(bindings);
                            bindTypesToTypesVisitor.init();
                            baseType.accept(bindTypesToTypesVisitor);
                            baseType = bindTypesToTypesVisitor.getType();

                            baseTypeToTypeArgumentVisitor.init();
                            baseType.accept(baseTypeToTypeArgumentVisitor);
                            entry.setValue(baseTypeToTypeArgumentVisitor.getTypeArgument());
                        }
                    }
                }
            }
        }

        return bindings;
    }

    protected boolean eraseTypeArguments(Expression expression, BaseTypeParameter typeParameters, BaseTypeArgument typeArguments) {
        if ((typeParameters != null) && (typeArguments == null) && (expression != null)) {
            Class expressionClass = expression.getClass();

            if (expressionClass == CastExpression.class) {
                expression = ((CastExpression)expression).getExpression();
                expressionClass = expression.getClass();
            }

            if ((expressionClass == FieldReferenceExpression.class) ||
                (expressionClass == ClassFileMethodInvocationExpression.class) ||
                (expressionClass == ClassFileLocalVariableReferenceExpression.class)) {
                return true;
            }
        }

        return false;
    }

    protected void populateBindingsWithTypeArgument(Map<String, TypeArgument> bindings, Map<String, BaseType> typeBounds, Type type, Expression expression) {
        Type t = getExpressionType(expression);

        if ((t != null) && (t != TYPE_UNDEFINED_OBJECT)) {
            populateBindingsWithTypeArgumentVisitor.init(contextualTypeBounds, bindings, typeBounds, t);
            type.accept(populateBindingsWithTypeArgumentVisitor);
        }
    }

    protected BaseType bind(Map<String, TypeArgument> bindings, BaseType parameterTypes) {
        if ((parameterTypes != null) && !bindings.isEmpty()) {
            bindTypesToTypesVisitor.setBindings(bindings);
            bindTypesToTypesVisitor.init();
            parameterTypes.accept(bindTypesToTypesVisitor);
            parameterTypes = bindTypesToTypesVisitor.getType();
        }

        return parameterTypes;
    }

    protected BaseType clone(BaseType parameterTypes) {
        if ((parameterTypes != null) && parameterTypes.isList()) {
            switch (parameterTypes.size()) {
                case 0:
                    parameterTypes = null;
                    break;
                case 1:
                    parameterTypes = parameterTypes.getFirst();
                    break;
                default:
                    parameterTypes = new Types(parameterTypes.getList());
                    break;
            }
        }

        return parameterTypes;
    }

    protected Type getExpressionType(Expression expression) {
        Class expressionClass = expression.getClass();

        if (expressionClass == ClassFileMethodInvocationExpression.class) {
            return getExpressionType((ClassFileMethodInvocationExpression)expression);
        } else if (expressionClass == ClassFileNewExpression.class) {
            return getExpressionType((ClassFileNewExpression)expression);
        }

        return expression.getType();
    }

    protected Type getExpressionType(ClassFileMethodInvocationExpression mie) {
        Type t = mie.getType();

        searchInTypeArgumentVisitor.init();
        t.accept(searchInTypeArgumentVisitor);

        if (!searchInTypeArgumentVisitor.containsGeneric()) {
            return t;
        }

        if (mie.getTypeParameters() != null) {
            return null;
        }

        if (staticMethod || (mie.getExpression().getClass() != ThisExpression.class)) {
            TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(mie.getInternalTypeName());

            if ((typeTypes != null) && (typeTypes.typeParameters != null)) {
                return null;
            }
        }

        return t;
    }

    protected Type getExpressionType(ClassFileNewExpression ne) {
        ObjectType ot = ne.getObjectType();

        if (staticMethod || !ot.getInternalName().equals(internalTypeName)) {
            TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(ot.getInternalName());

            if ((typeTypes != null) && (typeTypes.typeParameters != null)) {
                return null;
            }
        }

        return ot;
    }

    protected class BindVisitor extends AbstractNopExpressionVisitor {
        protected Type type;

        public void init(Type type) {
            this.type = type;
        }

        @Override
        public void visit(MethodInvocationExpression expression) {
            ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression)expression;
            bind(type, mie);
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (!type.isPrimitive()) {
                AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();
                localVariable.typeOnLeft(contextualTypeBounds, checkTypeArguments(type, localVariable));
            }
        }

        @Override
        public void visit(NewExpression expression) {
            ClassFileNewExpression ne = (ClassFileNewExpression)expression;
            bind(type, ne);
        }

        @Override
        public void visit(CastExpression expression) {
            assert TYPE_OBJECT.equals(type) || (type.getDimension() == expression.getType().getDimension()) : "TypeParametersToTypeArgumentsBinder.visit(CastExpression ce) : invalid array type";

            if (type.isObject()) {
                ObjectType objectType = (ObjectType)type;

                if ((objectType.getTypeArguments() != null) && !objectType.getTypeArguments().equals(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT)) {
                    assert expression.getType().isObject() : "TypeParametersToTypeArgumentsBinder.visit(CastExpression ce) : invalid object type";

                    ObjectType expressionObjectType = (ObjectType) expression.getType();

                    if (objectType.getInternalName().equals(expressionObjectType.getInternalName())) {
                        Type expressionExpressionType = expression.getExpression().getType();

                        if (expressionExpressionType.isObject()) {
                            ObjectType expressionExpressionObjectType = (ObjectType)expressionExpressionType;

                            if (expressionExpressionObjectType.getTypeArguments() == null) {
                                expression.setType(objectType);
                            } else if (objectType.getTypeArguments().isTypeArgumentAssignableFrom(contextualTypeBounds, expressionExpressionObjectType.getTypeArguments())) {
                                expression.setType(objectType);
                            }
                        } else if (expressionExpressionType.isGeneric()) {
                            expression.setType(objectType);
                        }
                    }
                }
            }

            type = expression.getType();
            expression.getExpression().accept(this);
        }

        @Override
        public void visit(TernaryOperatorExpression expression) {
            Type t = type;

            expression.setType(t);
            expression.getExpressionTrue().accept(this);
            type = t;
            expression.getExpressionFalse().accept(this);
        }

        @Override
        public void visit(BinaryOperatorExpression expression) {
            Type t = type;

            expression.getLeftExpression().accept(this);
            type = t;
            expression.getRightExpression().accept(this);
        }
    }

    protected static class RemoveNonWildcardTypeArgumentsVisitor extends AbstractNopExpressionVisitor {
        @Override
        public void visit(MethodInvocationExpression expression) {
            expression.setNonWildcardTypeArguments(null);
        }
    }
}
