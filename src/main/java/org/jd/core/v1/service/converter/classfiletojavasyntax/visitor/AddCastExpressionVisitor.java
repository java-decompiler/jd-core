/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.ArrayVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.BaseMemberDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.VariableInitializer;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.EnumConstantReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.TypeTypes;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.bcel.Const.ACC_BRIDGE;
import static org.apache.bcel.Const.ACC_SYNTHETIC;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_SHORT;

public class AddCastExpressionVisitor extends AbstractJavaSyntaxVisitor {
    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();

    private final TypeMaker typeMaker;
    private Map<String, BaseType> typeBounds;
    private Type returnedType;
    private BaseType exceptionTypes;
    private Deque<BaseTypeParameter> typeParameters = new ArrayDeque<>();
    private Map<String, BaseTypeArgument> parameterTypeArguments = new HashMap<>();
    private Type type;
    private boolean visitingAnonymousClass;
    private boolean visitingLambda;
    private Set<String> fieldNamesInLambda = new HashSet<>();

    public AddCastExpressionVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }
    
    @Override
    public void visit(BodyDeclaration declaration) {
        if (!declaration.isAnonymous()) {
            visitBodyDeclaration(declaration);
        }
    }

    private void visitBodyDeclaration(BodyDeclaration declaration) {
        BaseMemberDeclaration memberDeclarations = declaration.getMemberDeclarations();

        if (memberDeclarations != null) {
            Map<String, BaseType> tb = typeBounds;

            typeBounds = ((ClassFileBodyDeclaration)declaration).getTypeBounds();
            memberDeclarations.accept(this);
            typeBounds = tb;
        }
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        if ((declaration.getFlags() & ACC_SYNTHETIC) == 0) {
            Type t = type;

            type = declaration.getType();
            declaration.getFieldDeclarators().accept(this);
            type = t;
        }
    }

    @Override
    public void visit(FieldDeclarator declarator) {
        
        if (declarator.getName() != null && visitingLambda) {
            fieldNamesInLambda.add(declarator.getName());
        }
        
        VariableInitializer variableInitializer = declarator.getVariableInitializer();

        if (variableInitializer != null) {
            variableInitializer.accept(this);
        }
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        BaseStatement statements = declaration.getStatements();

        if (statements != null) {
            Map<String, BaseType> tb = typeBounds;

            typeBounds = ((ClassFileStaticInitializerDeclaration)declaration).getTypeBounds();
            statements.accept(this);
            typeBounds = tb;
        }
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        if ((declaration.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) == 0) {
            BaseStatement statements = declaration.getStatements();

            if (statements != null) {
                Map<String, BaseType> tb = typeBounds;
                BaseType et = exceptionTypes;

                typeBounds = ((ClassFileConstructorDeclaration) declaration).getTypeBounds();
                exceptionTypes = declaration.getExceptionTypes();
                statements.accept(this);
                typeBounds = tb;
                exceptionTypes = et;
            }
        }
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        if ((declaration.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) == 0) {
            BaseStatement statements = declaration.getStatements();

            if (statements != null) {
                Map<String, BaseType> tb = typeBounds;
                Type rt = returnedType;
                BaseType et = exceptionTypes;

                typeBounds = ((ClassFileMethodDeclaration) declaration).getTypeBounds();
                returnedType = declaration.getReturnedType();
                exceptionTypes = declaration.getExceptionTypes();
                pushContext(declaration);
                safeAccept(declaration.getFormalParameters());
                statements.accept(this);
                typeBounds = tb;
                returnedType = rt;
                exceptionTypes = et;
                popContext(declaration);
            }
        }
    }
    
    @Override
    public void visit(ClassDeclaration declaration) {
        pushContext(declaration);
        super.visit(declaration);
        popContext(declaration);
    }

    @Override
    public void visit(FormalParameter declaration) {
        if (!visitingAnonymousClass && declaration.getType() instanceof ObjectType) {
            ObjectType ot = (ObjectType) declaration.getType();
            parameterTypeArguments.put(declaration.getName(), ot.getTypeArguments());
        }
    }
    
    public void pushContext(MethodDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.push(declaration.getTypeParameters());
        }
    }

    public void popContext(MethodDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.pop();
        }
        if (!visitingAnonymousClass) {
            parameterTypeArguments.clear();
        }
    }

    public void pushContext(ClassDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.push(declaration.getTypeParameters());
        }
    }
    
    public void popContext(ClassDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.pop();
        }
    }
    
    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        visitingLambda = true;
        BaseStatement statements = expression.getStatements();

        if (statements != null) {
            Type rt = returnedType;
            returnedType = ObjectType.TYPE_OBJECT;
            statements.accept(this);
            returnedType = rt;
        }
        visitingLambda = false;
    }

    @Override
    public void visit(ReturnExpressionStatement statement) {
        statement.setExpression(updateStatementExpression(statement.getExpression()));
    }
    
    @Override
    public void visit(LambdaExpressionStatement statement) {
        statement.setExpression(updateStatementExpression(statement.getExpression()));
    }

    private Expression updateStatementExpression(Expression expression) {
        Map<String, TypeArgument> typeBindings = getLocalTypeBindings(expression);
        Map<String, BaseType> localTypeBounds = getLocalTypeBounds(expression);
        return updateExpression(typeBindings, localTypeBounds, returnedType, null, expression, false, true, false);
    }

    @Override
    public void visit(ThrowStatement statement) {
        if (exceptionTypes != null && exceptionTypes.size() == 1) {
            Type exceptionType = exceptionTypes.getFirst();

            if (exceptionType.isGenericType() && !statement.getExpression().getType().equals(exceptionType)) {
                statement.setExpression(addCastExpression(exceptionType, statement.getExpression()));
            }
        }
    }

    @Override
    public void visit(LocalVariableDeclaration declaration) {
        Type t = type;

        type = declaration.getType();
        declaration.getLocalVariableDeclarators().accept(this);
        type = t;
    }

    @Override
    public void visit(LocalVariableDeclarator declarator) {
        VariableInitializer variableInitializer = declarator.getVariableInitializer();

        if (variableInitializer != null) {
            int extraDimension = declarator.getDimension();

            if (extraDimension == 0) {
                variableInitializer.accept(this);
            } else {
                Type t = type;

                type = type.createType(type.getDimension() + extraDimension);
                variableInitializer.accept(this);
                type = t;
            }
        }
    }

    @Override
    public void visit(ArrayVariableInitializer declaration) {
        if (type.getDimension() == 0) {
            acceptListDeclaration(declaration);
        } else {
            Type t = type;

            type = type.createType(Math.max(0, type.getDimension() - 1));
            acceptListDeclaration(declaration);
            type = t;
        }
    }

    @Override
    public void visit(ExpressionVariableInitializer declaration) {
        Expression expression = declaration.getExpression();

        if (expression.isNewInitializedArray()) {
            NewInitializedArray nia = (NewInitializedArray)expression;
            Type t = type;

            type = nia.getType();
            nia.getArrayInitializer().accept(this);
            type = t;
        } else {
            declaration.setExpression(updateExpression(Collections.emptyMap(), typeBounds, type, null, expression, false, true, false));
        }
    }

    @Override
    public void visit(SuperConstructorInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (parameters != null && parameters.size() > 0) {
            boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(Collections.emptyMap(), typeBounds, expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters, true) > 1;
            boolean rawCast = false;
            BaseType parameterTypes = ((ClassFileSuperConstructorInvocationExpression)expression).getParameterTypes();
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, forceCast, unique, rawCast));
        }
    }

    @Override
    public void visit(ConstructorInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (parameters != null && parameters.size() > 0) {
            boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(Collections.emptyMap(), typeBounds, expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters, true) > 1;
            boolean rawCast = false;
            BaseType parameterTypes = ((ClassFileConstructorInvocationExpression)expression).getParameterTypes();
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, forceCast, unique, rawCast));
        }
    }

    private static Map<String, TypeArgument> getLocalTypeBindings(Expression exp) {
        if (exp instanceof MethodInvocationExpression) {
            MethodInvocationExpression mie = (MethodInvocationExpression) exp;
            return mie.getTypeBindings();
        }
        return Collections.emptyMap();
    }

    private Map<String, BaseType> getLocalTypeBounds(Expression exp) {
        Map<String, BaseType> localTypeBounds = new HashMap<>(typeBounds);
        if (exp instanceof MethodInvocationExpression) {
            MethodInvocationExpression mie = (MethodInvocationExpression) exp;
            if (mie.getTypeBounds() != null) {
                localTypeBounds.putAll(mie.getTypeBounds());
            }
        }
        return localTypeBounds;
    }
    
    @Override
    public void visit(MethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        Map<String, TypeArgument> typeBindings = getLocalTypeBindings(expression);
        Map<String, BaseType> localTypeBounds = getLocalTypeBounds(expression);
        
        if (parameters != null && parameters.size() > 0) {
            boolean unique = typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameters.size(), false) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(typeBindings, localTypeBounds, expression.getInternalTypeName(), expression.getName(), parameters, false) > 1;
            BaseType parameterTypes = ((ClassFileMethodInvocationExpression)expression).getParameterTypes();
            BaseType unboundParameterTypes = ((ClassFileMethodInvocationExpression)expression).getUnboundParameterTypes();
            boolean rawCast = false;
            expression.setParameters(updateParameters(typeBindings, localTypeBounds, parameterTypes, unboundParameterTypes, parameters, forceCast, unique, rawCast));
        }

        if (expression.getNonWildcardTypeArguments() != null) {
            if (hasKnownTypeParameters(expression.getNonWildcardTypeArguments())) {
                safeAccept(expression.getNonWildcardTypeArguments());
            } else {
                expression.setNonWildcardTypeArguments(null);
            }
        }

        if (expression.getExpression() instanceof CastExpression) {
            CastExpression ce = (CastExpression) expression.getExpression();
            if (ce.isByteCodeCheckCast() && ce.getExpression() instanceof ClassFileMethodInvocationExpression && ce.getType() instanceof ObjectType) {
                ObjectType ot = (ObjectType) ce.getType();
                if (ot != null) {
                    if (isCastToBeRemoved(typeBindings, localTypeBounds, ot, ce, true)) {
                        // Remove cast
                        expression.setExpression(ce.getExpression());
                    }
                    TypeTypes typeTypes = typeMaker.makeTypeTypes(ot.getInternalName());
                    if (typeTypes != null && typeTypes.getTypeParameters() != null && ot.getTypeArguments() == null) {
                        ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression) ce.getExpression();
                        if (mie.getUnboundType() instanceof ObjectType) {
                            TypeArguments typeArguments = new TypeArguments();
                            for (int i = 0; i < typeTypes.getTypeParameters().size(); i++) {
                                typeArguments.add(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                            }
                            ce.setType(ot.createType(typeArguments));
                        }
                    } else if (ot.getTypeArguments() != null && !hasKnownTypeParameters(ot.getTypeArguments())) {
                        // Remove cast
                        expression.setExpression(ce.getExpression());
                    }
                }
            }
        }
        
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(NewExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (parameters != null) {
            boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(Collections.emptyMap(), typeBounds, expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters, true) > 1;
            boolean rawCast = (returnedType instanceof ObjectType && expression.getType() instanceof ObjectType
                    && typeMaker.isRawTypeAssignable((ObjectType) returnedType, (ObjectType) expression.getType())
                    && !typeMaker.isAssignable(typeBounds, (ObjectType) returnedType, (ObjectType) expression.getType()));
            if (rawCast) {
                expression.setObjectType(expression.getObjectType().createType(((ObjectType) returnedType).getTypeArguments()));
            }
            BaseType parameterTypes = ((ClassFileNewExpression)expression).getParameterTypes();
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, forceCast, unique, rawCast));
        }
        
        if (expression.getBodyDeclaration() != null && expression.getBodyDeclaration().isAnonymous()) {
            visitingAnonymousClass = true;
            visitBodyDeclaration(expression.getBodyDeclaration());
            visitingAnonymousClass = false;
        }
        
        if (!hasKnownTypeParameters(expression.getObjectType())) {
            expression.setType(expression.getObjectType().createType(ObjectType.TYPE_UNDEFINED_OBJECT));
        }
    }

    @Override
    public void visit(NewInitializedArray expression) {
        ArrayVariableInitializer arrayInitializer = expression.getArrayInitializer();

        if (arrayInitializer != null) {
            Type t = type;

            type = expression.getType();
            arrayInitializer.accept(this);
            type = t;
        }
    }

    @Override
    public void visit(FieldReferenceExpression expression) {
        Expression exp = expression.getExpression();

        if (exp != null && !exp.isObjectTypeReferenceExpression()) {
            Type localType = typeMaker.makeFromInternalTypeName(expression.getInternalTypeName());

            if (localType.getName() != null) {
                expression.setExpression(updateExpression(Collections.emptyMap(), typeBounds, localType, null, exp, false, true, false));
            }
        }
    }

    @Override
    public void visit(BinaryOperatorExpression expression) {
        expression.getLeftExpression().accept(this);

        Expression rightExpression = expression.getRightExpression();

        if ("=".equals(expression.getOperator())) {
            if (rightExpression.isMethodInvocationExpression()) {
                ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression)rightExpression;

                if (mie.getTypeParameters() != null) {
                    // Do not add cast expression if method contains type parameters
                    rightExpression.accept(this);
                    return;
                }
            }

            expression.setRightExpression(updateExpression(Collections.emptyMap(), typeBounds, expression.getLeftExpression().getType(), null, rightExpression, false, true, false));
            return;
        }

        rightExpression.accept(this);
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        Type expressionType = expression.getType();

        expression.getCondition().accept(this);
        expression.setTrueExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getTrueExpression(), false, true, false));
        expression.setFalseExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getFalseExpression(), false, true, false));
    }

    @Override
    public void visit(TypeArguments type) {
        BaseTypeParameter baseTypeParameter = typeParameters.peek();
        TypeWithBoundsToGenericVisitor typeParameterVisitor = new TypeWithBoundsToGenericVisitor();
        if (baseTypeParameter != null) {
            baseTypeParameter.accept(typeParameterVisitor);
            type.accept(typeParameterVisitor);
        }
    }
    
    protected BaseExpression updateParameters(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, BaseType types, BaseType unboundTypes, BaseExpression expressions, boolean forceCast, boolean unique, boolean rawCast) {
        if (expressions != null) {
            if (expressions.isList()) {
                DefaultList<Type> typeList = types.getList();
                DefaultList<Type> unboundTypeList = unboundTypes == null ? null : unboundTypes.getList();
                DefaultList<Expression> expressionList = expressions.getList();

                for (int i = expressionList.size() - 1; i >= 0; i--) {
                    Type unboundType = unboundTypes == null ?  null : unboundTypeList.get(i);
                    expressionList.set(i, updateParameter(typeBindings, localTypeBounds, typeList.get(i), unboundType, expressionList.get(i), forceCast, unique, rawCast));
                }
            } else {
                Type unboundType = unboundTypes == null ?  null : unboundTypes.getFirst();
                expressions = updateParameter(typeBindings, localTypeBounds, types.getFirst(), unboundType, expressions.getFirst(), forceCast, unique, rawCast);
            }
        }

        return expressions;
    }

    private Expression updateParameter(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast) {

        if (visitingAnonymousClass && expression instanceof FieldReferenceExpression && expression.getType() instanceof ObjectType) {
            FieldReferenceExpression fieldRef = (FieldReferenceExpression) expression;
            ObjectType ot = (ObjectType) expression.getType();
            if (ot.getTypeArguments() == null) {
                BaseTypeArgument parameterTypeArgument = parameterTypeArguments.get(expression.getName());
                if (parameterTypeArgument != null) {
                    fieldRef.setType(ot.createType(parameterTypeArgument));
                }
            }
        }
        
        expression = updateExpression(typeBindings, localTypeBounds, type, unboundType, expression, forceCast, unique, rawCast);

        if (type == TYPE_BYTE) {
            if (expression.isIntegerConstantExpression()) {
                expression = new CastExpression(TYPE_BYTE, expression);
            } else if (expression.isTernaryOperatorExpression()) {
                Expression exp = expression.getTrueExpression();

                if (exp.isIntegerConstantExpression() || exp.isTernaryOperatorExpression()) {
                    expression = new CastExpression(TYPE_BYTE, expression);
                } else {
                    exp = expression.getFalseExpression();

                    if (exp.isIntegerConstantExpression() || exp.isTernaryOperatorExpression()) {
                        expression = new CastExpression(TYPE_BYTE, expression);
                    }
                }
            }
        }

        for (PrimitiveType primitiveType : Arrays.asList(TYPE_BYTE, TYPE_SHORT)) {
            if (primitiveType.createType(type.getDimension()).equals(type) && expression.isNewInitializedArray()) {
                NewInitializedArray newInitializedArray = (NewInitializedArray) expression;
                for (VariableInitializer variableInitializer : newInitializedArray.getArrayInitializer()) {
                    if (variableInitializer instanceof ExpressionVariableInitializer) {
                        ExpressionVariableInitializer evi = (ExpressionVariableInitializer) variableInitializer;
                        evi.setExpression(new CastExpression(primitiveType, evi.getExpression()));
                    }
                }
            }
        }
        
        return expression;
    }

    private Expression updateExpression(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast) {
        if (expression.isNullExpression()) {
            if (forceCast) {
                searchFirstLineNumberVisitor.init();
                expression.accept(searchFirstLineNumberVisitor);
                expression = new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type, expression);
            }
        } else {
            Type expressionType = expression.getType();

            if (!expressionType.equals(type)) {
                if (type.isObjectType()) {
                    if (expressionType.isObjectType()) {
                        ObjectType objectType = (ObjectType) type;
                        ObjectType expressionObjectType = (ObjectType) expressionType;
                        if (rawCast) {
                            expression = addCastExpression(objectType.createType(null), expression);
                        } else if (forceCast && (!objectType.rawEquals(expressionObjectType) || expression instanceof LambdaIdentifiersExpression)) {
                            // Force disambiguation of method invocation => Add cast
                            if (expression.isNewExpression()) {
                                ClassFileNewExpression ne = (ClassFileNewExpression)expression;
                                ne.setObjectType(ne.getObjectType().createType(null));
                            }
                            expression = addCastExpression(objectType, expression);
                        } else if (!ObjectType.TYPE_OBJECT.equals(type) && !typeMaker.isAssignable(typeBindings, localTypeBounds, objectType, unboundType, expressionObjectType)) {
                            BaseTypeArgument ta1 = objectType.getTypeArguments();
                            BaseTypeArgument ta2 = expressionObjectType.getTypeArguments();
                            Type t = type;

                            if (ta1 != null && ta2 != null && !ta1.isTypeArgumentAssignableFrom(typeMaker, typeBindings, localTypeBounds, ta2)) {
                                // Incompatible typeArgument arguments => Add cast
                                t = objectType.createType(ta1.isGenericTypeArgument() ? ta1 :  null);
                            }
                            if (!expression.isNew() && hasKnownTypeParameters(t) && !(ta1 instanceof WildcardSuperTypeArgument)) {
                                expression = addCastExpression(t, expression);
                            }
                        }
                    } else if (expressionType.getDimension() == 0 && type.getDimension() == 0 && expressionType.isGenericType() && !ObjectType.TYPE_OBJECT.equals(type)) {
                        boolean cast = true;
                        if (expressionType instanceof GenericType) {
                            GenericType gt = (GenericType) expressionType;
                            BaseType boundType = typeBounds.get(gt.getName());
                            if (boundType instanceof ObjectType) {
                                ObjectType boundObjectType = (ObjectType) boundType;
                                if (boundObjectType.equals(type)) {
                                    cast = false;
                                }
                            }
                        }
                        if (cast) {
                            expression = addCastExpression(type, expression);
                        }
                    }
                } else if (type.isGenericType() && hasKnownTypeParameters(type) && (expressionType.isObjectType() || expressionType.isGenericType())) {
                    if (expression instanceof CastExpression && isCastToBeRemoved(typeBindings, localTypeBounds, type, (CastExpression) expression, unique)) {
                        // Remove cast expression
                        expression = expression.getExpression();
                    } else {
                        expression = addCastExpression(type, expression);
                    }
                }
            }

            if (expression instanceof CastExpression && isCastToBeRemoved(typeBindings, localTypeBounds, type, (CastExpression) expression, unique)) {
                // Remove cast expression
                expression = expression.getExpression();
            }
            expression.accept(this);
        }

        return expression;
    }

    private boolean isCastToBeRemoved(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, CastExpression expression, boolean unique) {
        if (!hasKnownTypeParameters(expression.getType())) {
            return true;
        }
        Expression nestedExpression = expression.getExpression();
        if (nestedExpression.getExpression() instanceof FieldReferenceExpression) {
            FieldReferenceExpression fre = (FieldReferenceExpression) nestedExpression.getExpression();
            if (fieldNamesInLambda.contains(fre.getName())) {
                return false;
            }
        }
        Type nestedExpressionType = nestedExpression.getType();

        if (type.isObjectType() && nestedExpressionType.isObjectType()) {
            ObjectType left = (ObjectType) type;
            ObjectType right = (ObjectType) nestedExpressionType;
            if (left.equals(right) || unique && typeMaker.isAssignable(typeBindings, localTypeBounds, left, right)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasKnownTypeParameters(BaseTypeArgument type) {
        Set<String> genericIdentifiersInType = type.findTypeParametersInType();
        Set<String> genericIdentifiersInScope = findKnownTypeParameters();
        return genericIdentifiersInScope.containsAll(genericIdentifiersInType);
    }

    private Set<String> findKnownTypeParameters() {
        Set<String> genericIdentifiers = new HashSet<>();
        for (BaseTypeParameter baseTypeParameters : typeParameters) {
            for (TypeParameter typeParameter : baseTypeParameters) {
                genericIdentifiers.add(typeParameter.getIdentifier());
            }
        }
        return genericIdentifiers;
    }
    
    private Expression addCastExpression(Type type, Expression expression) {
        if (!expression.isCastExpression()) {
            searchFirstLineNumberVisitor.init();
            expression.accept(searchFirstLineNumberVisitor);
            return new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type, expression);
        }
        if (type.equals(expression.getExpression().getType())) {
            return expression.getExpression();
        }
        CastExpression ce = (CastExpression)expression;
        ce.setType(type);
        return ce;
    }

    @Override
    public void visit(FloatConstantExpression expression) {}
    @Override
    public void visit(IntegerConstantExpression expression) {}
    @Override
    public void visit(ConstructorReferenceExpression expression) {}
    @Override
    public void visit(DoubleConstantExpression expression) {}
    @Override
    public void visit(EnumConstantReferenceExpression expression) {}
    @Override
    public void visit(LocalVariableReferenceExpression expression) {}
    @Override
    public void visit(LongConstantExpression expression) {}
    @Override
    public void visit(BreakStatement statement) {}
    @Override
    public void visit(ContinueStatement statement) {}
    @Override
    public void visit(NullExpression expression) {}
    @Override
    public void visit(ObjectTypeReferenceExpression expression) {}
    @Override
    public void visit(SuperExpression expression) {}
    @Override
    public void visit(QualifiedSuperExpression expression) {}
    @Override
    public void visit(ThisExpression expression) {}
    @Override
    public void visit(TypeReferenceDotClassExpression expression) {}
    @Override
    public void visit(WildcardExtendsTypeArgument type) {}
    @Override
    public void visit(ObjectType type) {}
    @Override
    public void visit(InnerObjectType type) {}
    @Override
    public void visit(WildcardSuperTypeArgument type) {}
    @Override
    public void visit(Types list) {}
    @Override
    public void visit(TypeParameterWithTypeBounds type) {}
}
