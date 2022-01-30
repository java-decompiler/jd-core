/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javafragment;

import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.fragment.FixedFragment;
import org.jd.core.v1.model.token.AbstractNopTokenVisitor;
import org.jd.core.v1.model.token.LineNumberToken;
import org.jd.core.v1.model.token.NewLineToken;
import org.jd.core.v1.model.token.Token;

import java.util.Arrays;
import java.util.List;

public class LineNumberTokensFragment extends FixedFragment implements JavaFragment {
    private final List<Token> tokens;

    public LineNumberTokensFragment(Token... tokens) {
        this(Arrays.asList(tokens));
    }

    public LineNumberTokensFragment(List<Token> tokens) {
        super(searchFirstLineNumber(tokens), searchLastLineNumber(tokens));
        if (firstLineNumber == Printer.UNKNOWN_LINE_NUMBER) {
            throw new IllegalArgumentException("Use 'TokensFragment' instead");
        }
        this.tokens = tokens;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    protected static int searchFirstLineNumber(List<Token> tokens) {
        SearchLineNumberVisitor visitor = new SearchLineNumberVisitor();

        for (Token token : tokens) {
            token.accept(visitor);

            if (visitor.lineNumber != Printer.UNKNOWN_LINE_NUMBER) {
                return visitor.lineNumber - visitor.newLineCounter;
            }
        }

        return Printer.UNKNOWN_LINE_NUMBER;
    }

    protected static int searchLastLineNumber(List<Token> tokens) {
        SearchLineNumberVisitor visitor = new SearchLineNumberVisitor();
        int index = tokens.size();

        while (index-- > 0) {
            tokens.get(index).accept(visitor);

            if (visitor.lineNumber != Printer.UNKNOWN_LINE_NUMBER) {
                return visitor.lineNumber + visitor.newLineCounter;
            }
        }

        return Printer.UNKNOWN_LINE_NUMBER;
    }

    protected static class SearchLineNumberVisitor extends AbstractNopTokenVisitor {
        private int lineNumber;
        private int newLineCounter;

        @Override
        public void visit(LineNumberToken token) {
            lineNumber = token.lineNumber();
        }

        @Override
        public void visit(NewLineToken token) {
            newLineCounter++;
        }
    }

    @Override
    public void accept(JavaFragmentVisitor visitor) {
        visitor.visit(this);
    }
}
