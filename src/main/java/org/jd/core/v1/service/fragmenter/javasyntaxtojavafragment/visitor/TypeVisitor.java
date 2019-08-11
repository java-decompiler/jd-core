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
import org.jd.core.v1.model.javasyntax.type.*;
import org.jd.core.v1.model.token.*;
import org.jd.core.v1.util.DefaultList;

import java.util.HashMap;
import java.util.List;

import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_CLASS;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.*;

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

    protected Loader loader;
    protected String internalPackageName;
    protected int majorVersion;
    protected ImportsFragment importsFragment;
    protected Tokens tokens;
    protected int maxLineNumber = 0;
    protected String currentInternalTypeName;
    protected HashMap<String, TextToken> textTokenCache = new HashMap<>();

    public TypeVisitor(Loader loader, String mainInternalTypeName, int majorVersion, ImportsFragment importsFragment) {
        this.loader = loader;
        this.majorVersion = majorVersion;
        this.importsFragment = importsFragment;

        int index = mainInternalTypeName.lastIndexOf('/');
        this.internalPackageName = (index == -1) ? "" : mainInternalTypeName.substring(0, index+1);
    }

    @Override
    public void visit(ArrayTypeArguments list) {
        buildTokensForList(list, TextToken.COMMA_SPACE);
    }

    @Override
    public void visit(DiamondTypeArgument type) {}

    @Override
    public void visit(WildcardExtendsTypeArgument type) {
        tokens.add(TextToken.QUESTIONMARK_SPACE);
        tokens.add(EXTENDS);
        tokens.add(TextToken.SPACE);
        type.getType().accept(this);
    }

    @Override
    public void visit(PrimitiveType type) {
        switch (type.getJavaPrimitiveFlags()) {
            case FLAG_BOOLEAN: tokens.add(BOOLEAN); break;
            case FLAG_CHAR:    tokens.add(CHAR);    break;
            case FLAG_FLOAT:   tokens.add(FLOAT);   break;
            case FLAG_DOUBLE:  tokens.add(DOUBLE);  break;
            case FLAG_BYTE:    tokens.add(BYTE);    break;
            case FLAG_SHORT:   tokens.add(SHORT);   break;
            case FLAG_INT:     tokens.add(INT);     break;
            case FLAG_LONG:    tokens.add(LONG);    break;
            case FLAG_VOID:    tokens.add(VOID);    break;
        }

        // Build token for dimension
        visitDimension(type.getDimension());
    }

    @Override
    public void visit(ObjectType type) {
        // Build token for type reference
        tokens.add(newTypeReferenceToken(type, currentInternalTypeName));

        // Build token for type arguments
        BaseTypeArgument typeArguments = type.getTypeArguments();

        if (majorVersion >= 49) { // (majorVersion >= Java 5)
            if (typeArguments != null) {
                visitTypeArgumentList(typeArguments);
            } else if (type.equals(TYPE_CLASS)) {
                tokens.add(TextToken.LEFTANGLEBRACKET);
                tokens.add(TextToken.QUESTIONMARK);
                tokens.add(TextToken.RIGHTANGLEBRACKET);
            }
        }

        // Build token for dimension
        visitDimension(type.getDimension());
    }

    @Override
    public void visit(InnerObjectType type) {
        if ((currentInternalTypeName == null) || (!currentInternalTypeName.equals(type.getInternalName()) && !currentInternalTypeName.equals(type.getOuterType().getInternalName()))) {
            type.getOuterType().accept(this);
            tokens.add(TextToken.DOT);
        }

        // Build token for type reference
        tokens.add(new ReferenceToken(ReferenceToken.TYPE, type.getInternalName(), type.getName(), null, currentInternalTypeName));

        // Build token for type arguments
        visitTypeArgumentList(type.getTypeArguments());

        // Build token for dimension
        visitDimension(type.getDimension());
    }

    protected void visitTypeArgumentList(BaseTypeArgument typeArguments) {
        if (typeArguments != null) {
            tokens.add(TextToken.LEFTANGLEBRACKET);
            typeArguments.accept(this);
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
                tokens.add(newTextToken(new String(new char[dimension]).replaceAll("\0", "[]")));
        }
    }

    @Override
    public void visit(WildcardSuperTypeArgument type) {
        tokens.add(TextToken.QUESTIONMARK_SPACE);
        tokens.add(SUPER);
        tokens.add(TextToken.SPACE);
        type.getType().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(Types list) {
        buildTokensForList(list, TextToken.COMMA_SPACE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(TypeBounds list) {
        buildTokensForList(list, TextToken.SPACE_AND_SPACE);
    }

    @Override
    public void visit(TypeParameter type) {
        tokens.add(newTextToken(type.getIdentifier()));
    }

    @Override
    public void visit(TypeParameterWithTypeBounds type) {
        tokens.add(newTextToken(type.getIdentifier()));
        tokens.add(TextToken.SPACE);
        tokens.add(EXTENDS);
        tokens.add(TextToken.SPACE);
        type.getTypeBounds().accept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visit(TypeParameters list) {
        buildTokensForList(list, TextToken.COMMA_SPACE);
    }

    @Override
    public void visit(GenericType type) {
        tokens.add(newTextToken(type.getName()));
        visitDimension(type.getDimension());
    }

    @Override
    public void visit(UnknownTypeArgument type) {
        tokens.add(TextToken.QUESTIONMARK);
    }

    protected <T extends TypeVisitable> void buildTokensForList(List<T> list, TextToken separator) {
        int size = list.size();

        if (size > 0) {
            list.get(0).accept(this);

            for (int i=1; i<size; i++) {
                tokens.add(separator);
                list.get(i).accept(this);
            }
        }
    }

    protected ReferenceToken newTypeReferenceToken(ObjectType ot, String ownerInternalName) {
        String internalName = ot.getInternalName();
        String qualifiedName = ot.getQualifiedName();
        String name = ot.getName();

        if (packageContainsType(internalPackageName, internalName)) {
            // In the current package
            return new ReferenceToken(ReferenceToken.TYPE, internalName, name, null, ownerInternalName);
        } else {
            if (packageContainsType("java/lang/", internalName)) {
                // A 'java.lang' class
                String internalLocalTypeName = internalPackageName + name;

                if (loader.canLoad(internalLocalTypeName)) {
                    return new ReferenceToken(ReferenceToken.TYPE, internalName, qualifiedName, null, ownerInternalName);
                } else {
                    return new ReferenceToken(ReferenceToken.TYPE, internalName, name, null, ownerInternalName);
                }
            } else {
                return new TypeReferenceToken(importsFragment, internalName, qualifiedName, name, ownerInternalName);
            }
        }
    }

    protected static boolean packageContainsType(String internalPackageName, String internalClassName) {
        if (internalClassName.startsWith(internalPackageName)) {
            return internalClassName.indexOf('/', internalPackageName.length()) == -1;
        } else {
            return false;
        }
    }

    private static class TypeReferenceToken extends ReferenceToken {
        protected ImportsFragment importsFragment;
        protected String qualifiedName;

        public TypeReferenceToken(ImportsFragment importsFragment, String internalTypeName, String qualifiedName, String name, String ownerInternalName) {
            super(TYPE, internalTypeName, name, null, ownerInternalName);
            this.importsFragment = importsFragment;
            this.qualifiedName = qualifiedName;
        }

        @Override
        public String getName() {
            if (importsFragment.contains(internalTypeName)) {
                return name;
            } else {
                return qualifiedName;
            }
        }
    }

    protected TextToken newTextToken(String text) {
        TextToken textToken = textTokenCache.get(text);

        if (textToken == null) {
            textTokenCache.put(text, textToken=new TextToken(text));
        }

        return textToken;
    }

    public class Tokens extends DefaultList<Token> {
        protected int currentLineNumber = UNKNOWN_LINE_NUMBER;

        public int getCurrentLineNumber() {
            return currentLineNumber;
        }

        @Override
        public boolean add(Token token) {
            assert !(token instanceof LineNumberToken);
            return super.add(token);
        }

        public void addLineNumberToken(Expression expression) {
            addLineNumberToken(expression.getLineNumber());
        }

        public void addLineNumberToken(int lineNumber) {
            if (lineNumber != UNKNOWN_LINE_NUMBER) {
                if (lineNumber >= maxLineNumber) {
                    super.add(new LineNumberToken(lineNumber));
                    maxLineNumber = currentLineNumber = lineNumber;
                }
            }
        }
    }
}
