/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;

public class JavaTypeParametersToTypeArgumentsBinder extends AbstractTypeParametersToTypeArgumentsBinder {
    @Override
    public ClassFileConstructorInvocationExpression newConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor, TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        return new ClassFileConstructorInvocationExpression(lineNumber, objectType, descriptor, clone(methodTypes.getParameterTypes()), parameters, methodTypes.isVarArgs());
    }

    @Override
    public ClassFileSuperConstructorInvocationExpression newSuperConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor, TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        return new ClassFileSuperConstructorInvocationExpression(lineNumber, objectType, descriptor, clone(methodTypes.getParameterTypes()), parameters, methodTypes.isVarArgs());
    }

    @Override
    public ClassFileMethodInvocationExpression newMethodInvocationExpression(
            int lineNumber, Expression expression, ObjectType objectType, String name, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        return new ClassFileMethodInvocationExpression(
            lineNumber, methodTypes.getReturnedType(), expression,
            objectType.getInternalName(), name, descriptor, parameters, methodTypes);
    }

    @Override
    public FieldReferenceExpression newFieldReferenceExpression(
            int lineNumber, Type type, Expression expression, ObjectType objectType, String name, String descriptor) {
        return new FieldReferenceExpression(lineNumber, type, expression, objectType.getInternalName(), name, descriptor);
    }

    @Override
    public void bindParameterTypesWithArgumentTypes(Type type, Expression expression, boolean parametersFirst) {}
}
