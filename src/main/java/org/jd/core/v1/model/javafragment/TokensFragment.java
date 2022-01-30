/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javafragment;

import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.fragment.FlexibleFragment;
import org.jd.core.v1.model.token.AbstractNopTokenVisitor;
import org.jd.core.v1.model.token.EndBlockToken;
import org.jd.core.v1.model.token.LineNumberToken;
import org.jd.core.v1.model.token.StartBlockToken;
import org.jd.core.v1.model.token.TextToken;
import org.jd.core.v1.model.token.Token;

import java.util.Arrays;
import java.util.List;

import static org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.visitor.StatementVisitor.RETURN;

public class TokensFragment extends FlexibleFragment implements JavaFragment {
    public static final TokensFragment COMMA = new TokensFragment(TextToken.COMMA);
    public static final TokensFragment SEMICOLON = new TokensFragment(TextToken.SEMICOLON);
    public static final TokensFragment START_DECLARATION_OR_STATEMENT_BLOCK = new TokensFragment(StartBlockToken.START_DECLARATION_OR_STATEMENT_BLOCK);
    public static final TokensFragment END_DECLARATION_OR_STATEMENT_BLOCK = new TokensFragment(EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK);
    public static final TokensFragment END_DECLARATION_OR_STATEMENT_BLOCK_SEMICOLON = new TokensFragment(EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK, TextToken.SEMICOLON);
    public static final TokensFragment RETURN_SEMICOLON = new TokensFragment(RETURN, TextToken.SEMICOLON);

    private final List<Token> tokens;

    public TokensFragment(Token... tokens) {
        this(Arrays.asList(tokens));
    }

    public TokensFragment(List<Token> tokens) {
        this(getLineCount(tokens), tokens);
    }

    protected TokensFragment(int lineCount, List<Token> tokens) {
        super(lineCount, lineCount, lineCount, 0, "Tokens");
        this.tokens = tokens;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    protected static int getLineCount(List<Token> tokens) {
        LineCountVisitor  visitor = new LineCountVisitor();

        for (Token token : tokens) {
            token.accept(visitor);
        }

        return visitor.lineCount;
    }

    protected static class LineCountVisitor extends AbstractNopTokenVisitor {
        private int lineCount;

        @Override
        public void visit(LineNumberToken token) {
            lineCount++;
            if (token.lineNumber() != Printer.UNKNOWN_LINE_NUMBER) {
                throw new IllegalArgumentException("LineNumberToken cannot have a known line number. Uses 'LineNumberTokensFragment' instead");
            }
        }
    }

    @Override
    public void accept(JavaFragmentVisitor visitor) {
        visitor.visit(this);
    }
}
