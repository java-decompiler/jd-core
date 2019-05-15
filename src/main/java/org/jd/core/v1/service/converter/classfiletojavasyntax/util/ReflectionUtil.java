/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.*;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;

import java.lang.reflect.Method;

public class ReflectionUtil {
    public static final Method getExpression = getGetter(ExpressionStatement.class, "getExpression");
    public static final Method getLeftExpression = getGetter(BinaryOperatorExpression.class, "getLeftExpression");
    public static final Method getRightExpression = getGetter(BinaryOperatorExpression.class, "getRightExpression");
    public static final Method getLocalVariable = getGetter(ClassFileLocalVariableReferenceExpression.class, "getLocalVariable");
    public static final Method getName = getGetter(ClassFileLocalVariableReferenceExpression.class, "getName");
    public static final Method getValue = getGetter(IntegerConstantExpression.class, "getValue");
    public static final Method getMethodReferenceExpression = getGetter(MethodReferenceExpression.class, "getExpression");
    public static final Method getIndex = getGetter(ArrayExpression.class, "getIndex");
    public static final Method getStatements = getGetter(IfStatement.class, "getStatements");
    public static final Method getFirst = getGetter(Statements.class, "getFirst");
    public static final Method getElseStatements = getGetter(IfElseStatement.class, "getElseStatements");
    public static final Method getParameters = getGetter(MethodInvocationExpression.class, "getParameters");

    public static final Method[] getParameters_getString = { getParameters, getGetter(StringConstantExpression.class, "getString") };
    public static final Method[] getExpression_getLeftExpression_getLocalVariable = { getExpression, getLeftExpression, getLocalVariable };
    public static final Method[] getExpression_getRightExpression = { getExpression, getRightExpression};
    public static final Method[] getExpression_getLocalVariable = { getMethodReferenceExpression, getLocalVariable};
    public static final Method[] getRightExpression_getValue = {getRightExpression, getValue };
    public static final Method[] getLeftExpression_getIndex_getExpression_getName = {getLeftExpression, getIndex, getMethodReferenceExpression, getGetter(FieldReferenceExpression.class, "getName") };
    public static final Method[] getExpression_getRightExpression_getValue = { getExpression, getRightExpression, getValue };
    public static final Method[] getIndex_getExpression = { getIndex, getMethodReferenceExpression };
    public static final Method[] getStatements_getFirst = { getStatements, getFirst };
    public static final Method[] getFirst_getExpression = { getFirst, getExpression };
    public static final Method[] getElseStatements_getFirst_getExpression = { getElseStatements, getFirst, getExpression };

    protected static final Class[] parameterTypes = {};

    public static Object invokeGetter(Object object, Method getter) {
        if (object != null) {
            try {
                return getter.invoke(object);
            } catch (Exception e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        }
        return null;
    }

    public static Object invokeGetters(Object object, Method[] getters) {
        try {
            for (Method getter : getters) {
                object = getter.invoke(object);
            }
            return object;
        } catch (Exception e) {
            assert ExceptionUtil.printStackTrace(e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeGetter(Object object, Method getter, Class<T> resultClass) {
        if (object != null) {
            try {
                object = getter.invoke(object);
                if (resultClass.isInstance(object))
                    return (T)object;
            } catch (Exception e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeGetters(Object object, Method[] getters, Class<T> resultClass) {
        try {
            for (Method getter : getters) {
                object = getter.invoke(object);
            }
            if (resultClass.isInstance(object))
                return (T)object;
        } catch (Exception e) {
            assert ExceptionUtil.printStackTrace(e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeGetter(Object object, String getterName) {
        if (object != null) {
            try {
                return (T) getGetter(object.getClass(), getterName).invoke(object);
            } catch (Exception e) {
                assert ExceptionUtil.printStackTrace(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeGetters(Object object, String... getterNames) {
        try {
            for (String getterName : getterNames) {
                object = object.getClass().getMethod(getterName, parameterTypes).invoke(object);
            }
            return (T)object;
        } catch (Exception e) {
            assert ExceptionUtil.printStackTrace(e);
            return null;
        }
    }

    public static Method getGetter(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (Exception e) {
            assert ExceptionUtil.printStackTrace(e);
            return null;
        }
    }

    public static boolean equals(Object o1, Object o2) {
        return (o1 != null) && o1.equals(o2);
    }
}
