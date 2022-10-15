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
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;

public abstract class AbstractTypeParametersToTypeArgumentsBinder {
    
    protected BaseType exceptionTypes;
    
    public abstract ClassFileConstructorInvocationExpression newConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters);

    public abstract ClassFileSuperConstructorInvocationExpression newSuperConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters);

    public abstract ClassFileMethodInvocationExpression newMethodInvocationExpression(
            int lineNumber, Expression expression, ObjectType objectType, String name, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters);

    public abstract FieldReferenceExpression newFieldReferenceExpression(
            int lineNumber, Type type, Expression expression, ObjectType objectType, String name, String descriptor);

    public void bindParameterTypesWithArgumentTypes(Type type, Expression expression) {
        bindParameterTypesWithArgumentTypes(type, expression, false);
    }

    public abstract void bindParameterTypesWithArgumentTypes(Type type, Expression expression, boolean parametersFirst);

    public void setExceptionTypes(BaseType exceptionTypes) {
        this.exceptionTypes = exceptionTypes;
    }

    public void updateNewExpression(ClassFileNewExpression ne, String descriptor, TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        ne.set(descriptor, clone(methodTypes.getParameterTypes()), parameters);
    }

    public static BaseType clone(BaseType parameterTypes) {
        if (parameterTypes != null && parameterTypes.isList()) {
            parameterTypes = switch (parameterTypes.size()) {
                case 0 -> null;
                case 1 -> parameterTypes.getFirst();
                default -> new Types(parameterTypes.getList());
            };
        }

        return parameterTypes;
    }


}
