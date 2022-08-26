/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.visitor;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.javafragment.ImportsFragment;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.DiamondTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.TypeArgumentVisitable;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.javasyntax.type.TypeParameters;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.model.token.KeywordToken;
import org.jd.core.v1.model.token.LineNumberToken;
import org.jd.core.v1.model.token.ReferenceToken;
import org.jd.core.v1.model.token.TextToken;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.bcel.Const.MAJOR_1_5;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_CHAR;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_DOUBLE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_FLOAT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_LONG;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_SHORT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_VOID;

public class TypeVisitor extends AbstractJavaSyntaxVisitor {
    public static final KeywordToken BOOLEAN = new KeywordToken("boolean");
    public static final KeywordToken BYTE = new KeywordToken("byte");
    public static final KeywordToken CHAR = new KeywordToken("char");
    public static final KeywordToken DOUBLE = new KeywordToken("double");
    public static final KeywordToken EXPORTS = new KeywordToken("exports");
    public static final KeywordToken EXTENDS = new KeywordToken("extends");
    public static final KeywordToken FLOAT = new KeywordToken("float");
    public static final KeywordToken INT = new KeywordToken("int");
    public static final KeywordToken LONG = new KeywordToken("long");
    public static final KeywordToken MODULE = new KeywordToken("module");
    public static final KeywordToken OPEN = new KeywordToken("open");
    public static final KeywordToken OPENS = new KeywordToken("opens");
    public static final KeywordToken PROVIDES = new KeywordToken("provides");
    public static final KeywordToken REQUIRES = new KeywordToken("requires");
    public static final KeywordToken SHORT = new KeywordToken("short");
    public static final KeywordToken SUPER = new KeywordToken("super");
    public static final KeywordToken TO = new KeywordToken("to");
    public static final KeywordToken TRANSITIVE = new KeywordToken("transitive");
    public static final KeywordToken USES = new KeywordToken("uses");
    public static final KeywordToken VOID = new KeywordToken("void");
    public static final KeywordToken WITH = new KeywordToken("with");

    public static final int UNKNOWN_LINE_NUMBER = Printer.UNKNOWN_LINE_NUMBER;

    private final Loader loader;
    protected final TypeMaker typeMaker;
    private final String internalPackageName;
    private final boolean genericTypesSupported;
    protected final ImportsFragment importsFragment;
    protected Tokens tokens;
    private int maxLineNumber;
    protected ObjectType currentType;
    private final Map<String, TextToken> textTokenCache = new HashMap<>();

    public TypeVisitor(Loader loader, String mainInternalTypeName, int majorVersion, ImportsFragment importsFragment) {
        this.loader = loader;
        this.typeMaker = new TypeMaker(loader);
        this.genericTypesSupported = majorVersion >= MAJOR_1_5;
        this.importsFragment = importsFragment;
        int index = mainInternalTypeName.lastIndexOf('/');
        this.internalPackageName = index == -1 ? "" : mainInternalTypeName.substring(0, index+1);
    }

    @Override
    public void visit(TypeArguments arguments) {
        buildTokensForList(arguments, TextToken.COMMA_SPACE);
    }

    @Override
    public void visit(DiamondTypeArgument argument) {}

    @Override
    public void visit(WildcardExtendsTypeArgument argument) {
        tokens.add(TextToken.QUESTIONMARK_SPACE);
        tokens.add(EXTENDS);
        tokens.add(TextToken.SPACE);

        BaseType type = argument.type();

        type.accept(this);
    }

    @Override
    public void visit(PrimitiveType type) {
        switch (type.getJavaPrimitiveFlags()) {
            case FLAG_BOOLEAN:
                tokens.add(BOOLEAN);
                break;
            case FLAG_CHAR:
                tokens.add(CHAR);
                break;
            case FLAG_FLOAT:
                tokens.add(FLOAT);
                break;
            case FLAG_DOUBLE:
                tokens.add(DOUBLE);
                break;
            case FLAG_BYTE:
                tokens.add(BYTE);
                break;
            case FLAG_SHORT:
                tokens.add(SHORT);
                break;
            case FLAG_INT:
                tokens.add(INT);
                break;
            case FLAG_LONG:
                tokens.add(LONG);
                break;
            case FLAG_VOID:
                tokens.add(VOID);
                break;
        }

        // Build token for dimension
        visitDimension(type.getDimension());
    }

    @Override
    public void visit(ObjectType type) {
        // Build token for type reference
        tokens.add(newTypeReferenceToken(type, currentType));

        if (genericTypesSupported) {
            // Build token for type arguments
            BaseTypeArgument typeArguments = type.getTypeArguments();

            if (typeArguments != null) {
                visitTypeArgumentList(typeArguments);
            }
        }

        // Build token for dimension
        visitDimension(type.getDimension());
    }

    @Override
    public void visit(InnerObjectType type) {
        BaseType outerType = type.getOuterType();
        if (currentType == null || !currentType.getInternalName().equals(type.getInternalName()) && outerType != null && !currentType.getInternalName().equals(outerType.getInternalName())) {

            safeAccept(outerType);
            tokens.add(TextToken.DOT);
        }

        // Build token for type reference
        tokens.add(new ReferenceToken(Printer.TYPE, type.getInternalName(), type.getName(), null, currentType));

        if (genericTypesSupported) {
            // Build token for type arguments
            BaseTypeArgument typeArguments = type.getTypeArguments();

            if (typeArguments != null) {
                visitTypeArgumentList(typeArguments);
            }
        }

        // Build token for dimension
        visitDimension(type.getDimension());
    }

    protected void visitTypeArgumentList(BaseTypeArgument arguments) {
        if (arguments != null) {
            tokens.add(TextToken.LEFTANGLEBRACKET);
            arguments.accept(this);
            tokens.add(TextToken.RIGHTANGLEBRACKET);
        }
    }

    protected void visitDimension(int dimension) {
        switch (dimension) {
            case 0:
                break;
            case 1:
                tokens.add(TextToken.DIMENSION_1);
                break;
            case 2:
                tokens.add(TextToken.DIMENSION_2);
                break;
            default:
                tokens.add(newTextToken(new String(new char[dimension]).replace("\0", "[]")));
                break;
        }
    }

    @Override
    public void visit(WildcardSuperTypeArgument argument) {
        tokens.add(TextToken.QUESTIONMARK_SPACE);
        tokens.add(SUPER);
        tokens.add(TextToken.SPACE);

        BaseType type = argument.type();

        type.accept(this);
    }

    @Override
    public void visit(Types types) {
        buildTokensForList(types, TextToken.COMMA_SPACE);
    }

    @Override
    public void visit(TypeParameter parameter) {
        tokens.add(newTextToken(parameter.getIdentifier()));
    }

    @Override
    public void visit(TypeParameterWithTypeBounds parameter) {
        tokens.add(newTextToken(parameter.getIdentifier()));
        tokens.add(TextToken.SPACE);
        tokens.add(EXTENDS);
        tokens.add(TextToken.SPACE);

        BaseType types = parameter.getTypeBounds();

        if (types.isList()) {
            buildTokensForList(types.getList(), TextToken.SPACE_AND_SPACE);
        } else {
            BaseType type = types.getFirst();

            type.accept(this);
        }
    }

    @Override
    public void visit(TypeParameters parameters) {
        int size = parameters.size();

        if (size > 0) {
            parameters.get(0).accept(this);

            for (int i=1; i<size; i++) {
                tokens.add(TextToken.COMMA_SPACE);
                parameters.get(i).accept(this);
            }
        }
    }

    @Override
    public void visit(GenericType type) {
        tokens.add(newTextToken(type.getName()));
        visitDimension(type.getDimension());
    }

    @Override
    public void visit(WildcardTypeArgument type) {
        tokens.add(TextToken.QUESTIONMARK);
    }

    protected <T extends TypeArgumentVisitable> void buildTokensForList(List<T> list, TextToken separator) {
        int size = list.size();

        if (size > 0) {
            list.get(0).accept(this);

            for (int i=1; i<size; i++) {
                tokens.add(separator);
                list.get(i).accept(this);
            }
        }
    }

    protected ReferenceToken newTypeReferenceToken(ObjectType ot, ObjectType ownerType) {
        String internalName = ot.getInternalName();
        String qualifiedName = ot.getQualifiedName();
        
        int printerType = isInInvokeNew() ? Printer.CONSTRUCTOR : Printer.TYPE;
        String name = ot.getName();
        if (packageContainsType(internalPackageName, internalName)) {
            // In the current package
            if (ownerType != null && ownerType.getInnerTypeNames() != null) {
                String innerTypeName = ownerType.getInternalName() + '$' + name;
                if (ownerType.getInnerTypeNames().contains(innerTypeName)) {
                    return new QualifiedReferenceToken(printerType, internalName, qualifiedName, null, ownerType);
                }
                return new ReferenceToken(printerType, internalName, name, null, ownerType);
            }
            return new ReferenceToken(printerType, internalName, name, null, ownerType);
        }
        if (packageContainsType("java/lang/", internalName)) {
            // A 'java.lang' class
            String internalLocalTypeName = internalPackageName + name;

            if (loader.canLoad(internalLocalTypeName)) {
                return new ReferenceToken(printerType, internalName, qualifiedName, null, ownerType);
            }
            return new ReferenceToken(printerType, internalName, name, null, ownerType);
        }
        return new TypeReferenceToken(importsFragment, printerType, internalName, qualifiedName, name, ownerType);
    }

    protected boolean isInInvokeNew() {
        return false;
    }

    protected static boolean packageContainsType(String internalPackageName, String internalClassName) {
        return internalClassName.startsWith(internalPackageName) && internalClassName.indexOf('/', internalPackageName.length()) == -1;
    }

    private static final class TypeReferenceToken extends ReferenceToken {
        private ImportsFragment importsFragment;
        private String qualifiedName;

        public TypeReferenceToken(ImportsFragment importsFragment, int printerType, String internalTypeName, String qualifiedName, String name, ObjectType ownerType) {
            super(printerType, internalTypeName, name, null, ownerType);
            this.importsFragment = importsFragment;
            this.qualifiedName = qualifiedName;
        }

        @Override
        public String getName() {
            if (importsFragment.contains(internalTypeName)) {
                return name;
            }
            return qualifiedName;
        }
    }

    private static final class QualifiedReferenceToken extends ReferenceToken {
        private String qualifiedName;
        
        public QualifiedReferenceToken(int printerType, String internalTypeName, String qualifiedName, String name, ObjectType ownerType) {
            super(printerType, internalTypeName, name, null, ownerType);
            this.qualifiedName = qualifiedName;
        }
        
        @Override
        public String getName() {
            return qualifiedName;
        }
    }
    
    protected TextToken newTextToken(String text) {
        return textTokenCache.computeIfAbsent(text, TextToken::new);
    }

    public class Tokens extends DefaultList<Token> {
        private static final long serialVersionUID = 1L;
        private int currentLineNumber = UNKNOWN_LINE_NUMBER;

        public int getCurrentLineNumber() {
            return currentLineNumber;
        }

        @Override
        public boolean add(Token token) {
            if (token instanceof LineNumberToken) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                throw new IllegalArgumentException("token instanceof LineNumberToken");
            }
            return super.add(token);
        }

        public void addLineNumberToken(Expression expression) {
            addLineNumberToken(expression.getLineNumber());
        }

        public void addLineNumberToken(int lineNumber) {
            if (lineNumber != UNKNOWN_LINE_NUMBER && lineNumber >= maxLineNumber) {
                super.add(new LineNumberToken(lineNumber));
                maxLineNumber = currentLineNumber = lineNumber;
            }
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hash(getEnclosingInstance(), currentLineNumber);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj) || getClass() != obj.getClass()) {
                return false;
            }
            Tokens other = (Tokens) obj;
            return getEnclosingInstance().equals(other.getEnclosingInstance()) && currentLineNumber == other.currentLineNumber;
        }

        private TypeVisitor getEnclosingInstance() {
            return TypeVisitor.this;
        }
    }
}
