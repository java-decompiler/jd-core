/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.tokenizer.javafragmenttotoken.visitor;

import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.model.javafragment.EndBlockFragment;
import org.jd.core.v1.model.javafragment.EndBlockInParameterFragment;
import org.jd.core.v1.model.javafragment.EndBodyFragment;
import org.jd.core.v1.model.javafragment.EndBodyInParameterFragment;
import org.jd.core.v1.model.javafragment.EndMovableJavaBlockFragment;
import org.jd.core.v1.model.javafragment.EndSingleStatementBlockFragment;
import org.jd.core.v1.model.javafragment.EndStatementsBlockFragment;
import org.jd.core.v1.model.javafragment.ImportsFragment;
import org.jd.core.v1.model.javafragment.ImportsFragment.Import;
import org.jd.core.v1.model.javafragment.JavaFragmentVisitor;
import org.jd.core.v1.model.javafragment.LineNumberTokensFragment;
import org.jd.core.v1.model.javafragment.SpaceSpacerFragment;
import org.jd.core.v1.model.javafragment.SpacerBetweenMembersFragment;
import org.jd.core.v1.model.javafragment.SpacerFragment;
import org.jd.core.v1.model.javafragment.StartBlockFragment;
import org.jd.core.v1.model.javafragment.StartBodyFragment;
import org.jd.core.v1.model.javafragment.StartMovableJavaBlockFragment;
import org.jd.core.v1.model.javafragment.StartSingleStatementBlockFragment;
import org.jd.core.v1.model.javafragment.StartStatementsBlockFragment;
import org.jd.core.v1.model.javafragment.StartStatementsDoWhileBlockFragment;
import org.jd.core.v1.model.javafragment.StartStatementsTryBlockFragment;
import org.jd.core.v1.model.javafragment.TokensFragment;
import org.jd.core.v1.model.token.AbstractNopTokenVisitor;
import org.jd.core.v1.model.token.BooleanConstantToken;
import org.jd.core.v1.model.token.CharacterConstantToken;
import org.jd.core.v1.model.token.DeclarationToken;
import org.jd.core.v1.model.token.EndBlockToken;
import org.jd.core.v1.model.token.EndMarkerToken;
import org.jd.core.v1.model.token.KeywordToken;
import org.jd.core.v1.model.token.LineNumberToken;
import org.jd.core.v1.model.token.NewLineToken;
import org.jd.core.v1.model.token.NumericConstantToken;
import org.jd.core.v1.model.token.ReferenceToken;
import org.jd.core.v1.model.token.StartBlockToken;
import org.jd.core.v1.model.token.StartMarkerToken;
import org.jd.core.v1.model.token.StringConstantToken;
import org.jd.core.v1.model.token.TextToken;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.model.token.TokenVisitor;
import org.jd.core.v1.util.DefaultList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class TokenizeJavaFragmentVisitor implements JavaFragmentVisitor {

    protected static final KeywordToken DO = new KeywordToken("do");
    protected static final KeywordToken IMPORT = new KeywordToken("import");
    protected static final KeywordToken TRY = new KeywordToken("try");

    protected static final List<Token> DO_TOKENS = Arrays.asList((Token)DO);
    protected static final List<Token> TRY_TOKENS = Arrays.asList((Token)TRY);

    private final KnownLineNumberTokenVisitor knownLineNumberTokenVisitor = new KnownLineNumberTokenVisitor();
    private final UnknownLineNumberTokenVisitor unknownLineNumberTokenVisitor = new UnknownLineNumberTokenVisitor();
    protected final DefaultList<Token> tokens;

    public TokenizeJavaFragmentVisitor(int initialCapacity) {
        this.tokens = new DefaultList<>(initialCapacity);
    }

    public DefaultList<Token> getTokens() {
        return tokens;
    }

    @Override
    public void visit(EndBlockFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                tokens.add(EndBlockToken.END_BLOCK);
                break;
            case 1:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_2);
                tokens.add(EndBlockToken.END_BLOCK);
                break;
            default:
                tokens.add(new NewLineToken(fragment.getLineCount()));
                tokens.add(EndBlockToken.END_BLOCK);
        }
    }

    @Override
    public void visit(EndBlockInParameterFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
                break;
            case 1:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_2);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
                break;
            default:
                tokens.add(new NewLineToken(fragment.getLineCount()));
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
        }
    }

    @Override
    public void visit(EndBodyFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                tokens.add(EndBlockToken.END_BLOCK);
                break;
            case 1:
                if (fragment.getStartBodyFragment().getLineCount() == 0) {
                    tokens.add(TextToken.SPACE);
                    tokens.add(EndBlockToken.END_BLOCK);
                    tokens.add(NewLineToken.NEWLINE_1);
                } else {
                    tokens.add(NewLineToken.NEWLINE_1);
                    tokens.add(EndBlockToken.END_BLOCK);
                }
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            default:
                tokens.add(new NewLineToken(fragment.getLineCount()-1));
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
        }
    }

    @Override
    public void visit(EndBodyInParameterFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
                break;
            case 1:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
                tokens.add(TextToken.SPACE);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            default:
                tokens.add(new NewLineToken(fragment.getLineCount()-1));
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.COMMA);
                tokens.add(NewLineToken.NEWLINE_1);
        }
    }

    @Override
    public void visit(EndMovableJavaBlockFragment fragment) {}

    @Override
    public void visit(EndSingleStatementBlockFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
            if (fragment.getStartSingleStatementBlockFragment().getLineCount() == 0 || fragment.getStartSingleStatementBlockFragment().getLineCount() == 1) {
                tokens.add(TextToken.SPACE);
                tokens.add(EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK);
            } else {
                tokens.add(TextToken.SPACE);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.SPACE);
            }
                break;
            case 1:
            if (fragment.getStartSingleStatementBlockFragment().getLineCount() == 0) {
                tokens.add(EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
            } else {
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
            }
                break;
            case 2:
            if (fragment.getStartSingleStatementBlockFragment().getLineCount() == 0) {
                tokens.add(EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK);
                tokens.add(NewLineToken.NEWLINE_2);
            } else {
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
            }
                break;
            default:
            if (fragment.getStartSingleStatementBlockFragment().getLineCount() == 0) {
                tokens.add(EndBlockToken.END_DECLARATION_OR_STATEMENT_BLOCK);
                tokens.add(new NewLineToken(fragment.getLineCount()));
            } else {
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(new NewLineToken(fragment.getLineCount()-1));
            }
                break;
        }
    }

    @Override
    public void visit(EndStatementsBlockFragment fragment) {
        int minimalLineCount = fragment.getGroup().getMinimalLineCount();

        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                tokens.add(EndBlockToken.END_BLOCK);
                tokens.add(TextToken.SPACE);
                break;
            case 1:
                if (minimalLineCount == 0) {
                    tokens.add(TextToken.SPACE);
                    tokens.add(EndBlockToken.END_BLOCK);
                    tokens.add(NewLineToken.NEWLINE_1);
                } else {
                    tokens.add(NewLineToken.NEWLINE_1);
                    tokens.add(EndBlockToken.END_BLOCK);
                    tokens.add(TextToken.SPACE);
                }
                break;
            case 2:
                switch (minimalLineCount) {
                    case 0:
                        tokens.add(TextToken.SPACE);
                        tokens.add(EndBlockToken.END_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_2);
                        break;
                    case 1:
                        tokens.add(NewLineToken.NEWLINE_1);
                        tokens.add(EndBlockToken.END_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_1);
                        break;
                    default:
                        tokens.add(new NewLineToken(fragment.getLineCount()-1));
                        tokens.add(EndBlockToken.END_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_1);
                }
                break;
            default:
                switch (minimalLineCount) {
                    case 0:
                        tokens.add(TextToken.SPACE);
                        tokens.add(EndBlockToken.END_BLOCK);
                        tokens.add(new NewLineToken(fragment.getLineCount()));
                        break;
                    case 1:
                        tokens.add(new NewLineToken(fragment.getLineCount()));
                        tokens.add(EndBlockToken.END_BLOCK);
                        break;
                    default:
                        tokens.add(new NewLineToken(fragment.getLineCount()-1));
                        tokens.add(EndBlockToken.END_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_1);
                }
                break;
        }
    }

    @Override
    public void visit(ImportsFragment fragment) {
        List<ImportsFragment.Import> imports = new DefaultList<>(fragment.getImports());

        imports.sort(Comparator.comparing(Import::getQualifiedName));

        tokens.add(StartMarkerToken.IMPORT_STATEMENTS);

        for (ImportsFragment.Import imp : imports) {
            tokens.add(IMPORT);
            tokens.add(TextToken.SPACE);
            tokens.add(new ReferenceToken(Printer.TYPE, imp.getInternalName(), imp.getQualifiedName()));
            tokens.add(TextToken.SEMICOLON);
            tokens.add(NewLineToken.NEWLINE_1);
        }

        tokens.add(EndMarkerToken.IMPORT_STATEMENTS);
    }

    @Override
    public void visit(LineNumberTokensFragment fragment) {
        knownLineNumberTokenVisitor.reset(fragment.getFirstLineNumber());

        for (Token token : fragment.getTokens()) {
            token.accept(knownLineNumberTokenVisitor);
        }
    }

    @Override
    public void visit(SpacerBetweenMembersFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                break;
            case 1:
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_2);
                break;
            default:
                tokens.add(new NewLineToken(fragment.getLineCount()));
        }
    }

    @Override
    public void visit(SpacerFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                break;
            case 1:
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_2);
                break;
            default:
                tokens.add(new NewLineToken(fragment.getLineCount()));
        }
    }

    @Override
    public void visit(SpaceSpacerFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                break;
            case 1:
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_2);
                break;
            default:
                tokens.add(new NewLineToken(fragment.getLineCount()));
        }
    }

    @Override
    public void visit(StartBlockFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(TextToken.SPACE);
                break;
            case 1:
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            case 2:
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(NewLineToken.NEWLINE_2);
                break;
            default:
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(new NewLineToken(fragment.getLineCount()));
                break;
        }
    }

    @Override
    public void visit(StartBodyFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(TextToken.SPACE);
                break;
            case 1:
                tokens.add(TextToken.SPACE);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            default:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(new NewLineToken(fragment.getLineCount()-1));
                break;
        }
    }

    @Override
    public void visit(StartMovableJavaBlockFragment fragment) {
    }

    @Override
    public void visit(StartSingleStatementBlockFragment fragment) {
        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(StartBlockToken.START_DECLARATION_OR_STATEMENT_BLOCK);
                tokens.add(TextToken.SPACE);
                break;
            case 1:
            if (fragment.getEndSingleStatementBlockFragment().getLineCount() == 0) {
                tokens.add(StartBlockToken.START_DECLARATION_OR_STATEMENT_BLOCK);
            } else {
                tokens.add(TextToken.SPACE);
                tokens.add(StartBlockToken.START_BLOCK);
            }
            tokens.add(NewLineToken.NEWLINE_1);
                break;
            case 2:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(NewLineToken.NEWLINE_1);
                break;
            default:
                tokens.add(NewLineToken.NEWLINE_1);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(new NewLineToken(fragment.getLineCount() - 1));
                break;
        }
    }

    @Override
    public void visit(StartStatementsBlockFragment fragment) {
        int minimalLineCount = fragment.getGroup().getMinimalLineCount();

        switch (fragment.getLineCount()) {
            case 0:
                tokens.add(TextToken.SPACE);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(TextToken.SPACE);
                break;
            case 1:
                if (minimalLineCount == 0) {
                    tokens.add(NewLineToken.NEWLINE_1);
                    tokens.add(StartBlockToken.START_BLOCK);
                    tokens.add(TextToken.SPACE);
                } else {
                    tokens.add(TextToken.SPACE);
                    tokens.add(StartBlockToken.START_BLOCK);
                    tokens.add(NewLineToken.NEWLINE_1);
                }
                break;
            case 2:
                switch (minimalLineCount) {
                    case 0:
                        tokens.add(NewLineToken.NEWLINE_2);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(TextToken.SPACE);
                        break;
                    case 1:
                        tokens.add(TextToken.SPACE);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_2);
                        break;
                    default:
                        tokens.add(NewLineToken.NEWLINE_1);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_1);
                }
                break;
            default:
                switch (minimalLineCount) {
                    case 0:
                        tokens.add(new NewLineToken(fragment.getLineCount()));
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(TextToken.SPACE);
                        break;
                    case 1:
                        tokens.add(TextToken.SPACE);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(new NewLineToken(fragment.getLineCount()));
                        break;
                    default:
                        tokens.add(NewLineToken.NEWLINE_1);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(new NewLineToken(fragment.getLineCount() - 1));
                }
                break;
        }
    }

    @Override
    public void visit(StartStatementsDoWhileBlockFragment fragment) {
        visit(fragment, DO_TOKENS);
    }

    @Override
    public void visit(StartStatementsTryBlockFragment fragment) {
        visit(fragment, TRY_TOKENS);
    }

    @Override
    public void visit(TokensFragment fragment) {
        for (Token token : fragment.getTokens()) {
            token.accept(unknownLineNumberTokenVisitor);
        }
    }

    protected class KnownLineNumberTokenVisitor extends AbstractNopTokenVisitor {
        private int currentLineNumber;

        public void reset(int firstLineNumber) {
            this.currentLineNumber = firstLineNumber;
        }

        @Override
        public void visit(EndBlockToken token) {
            if (token == EndBlockToken.END_BLOCK) {
                throw new IllegalArgumentException("Unexpected EndBlockToken.END_BLOCK at this step. Uses 'JavaFragmentFactory.addEnd***(fragments)' instead");
            }
            tokens.add(token);
        }

        @Override
        public void visit(LineNumberToken token) {
            int lineNumber = token.lineNumber();

            if (lineNumber != Printer.UNKNOWN_LINE_NUMBER) {
                if (currentLineNumber != Printer.UNKNOWN_LINE_NUMBER) {
                    switch (lineNumber - currentLineNumber) {
                        case 0:
                            break;
                        case 1:
                            tokens.add(NewLineToken.NEWLINE_1);
                            break;
                        case 2:
                            tokens.add(NewLineToken.NEWLINE_2);
                            break;
                        default:
                            tokens.add(new NewLineToken(lineNumber - currentLineNumber));
                            break;
                    }
                }

                currentLineNumber = token.lineNumber();
                tokens.add(token);
            }
        }

        @Override
        public void visit(StartBlockToken token) {
            if (token == StartBlockToken.START_BLOCK) {
                throw new IllegalArgumentException("Unexpected StartBlockToken.START_BLOCK at this step. Uses 'JavaFragmentFactory.addStart***(fragments)' instead");
            }
            tokens.add(token);
        }

        @Override
        public void visit(BooleanConstantToken token) { tokens.add(token); }
        @Override
        public void visit(CharacterConstantToken token) { tokens.add(token); }
        @Override
        public void visit(DeclarationToken token) { tokens.add(token); }
        @Override
        public void visit(EndMarkerToken token) { tokens.add(token); }
        @Override
        public void visit(KeywordToken token) { tokens.add(token); }
        @Override
        public void visit(NumericConstantToken token) { tokens.add(token); }
        @Override
        public void visit(ReferenceToken token) { tokens.add(token); }
        @Override
        public void visit(StartMarkerToken token) { tokens.add(token); }
        @Override
        public void visit(StringConstantToken token) { tokens.add(token); }
        @Override
        public void visit(TextToken token) { tokens.add(token); }
    }

    protected class UnknownLineNumberTokenVisitor implements TokenVisitor {
        @Override
        public void visit(EndBlockToken token) {
            if (token == EndBlockToken.END_BLOCK) {
                throw new IllegalArgumentException("Unexpected EndBlockToken.END_BLOCK at this step. Uses 'JavaFragmentFactory.addEnd***(fragments)' instead");
            }
            tokens.add(token);
        }

        @Override
        public void visit(LineNumberToken token) {
            if (token.lineNumber() != Printer.UNKNOWN_LINE_NUMBER) {
                throw new IllegalArgumentException("LineNumberToken cannot have a known line number. Uses 'LineNumberTokensFragment' instead");
            }
        }

        @Override
        public void visit(StartBlockToken token) {
            if (token == StartBlockToken.START_BLOCK) {
                throw new IllegalArgumentException("Unexpected StartBlockToken.START_BLOCK at this step. Uses 'JavaFragmentFactory.addStart***(fragments)' instead");
            }
            tokens.add(token);
        }

        @Override
        public void visit(BooleanConstantToken token) { tokens.add(token); }
        @Override
        public void visit(CharacterConstantToken token) { tokens.add(token); }
        @Override
        public void visit(DeclarationToken token) { tokens.add(token); }
        @Override
        public void visit(EndMarkerToken token) { tokens.add(token); }
        @Override
        public void visit(KeywordToken token) { tokens.add(token); }
        @Override
        public void visit(NewLineToken token) { tokens.add(token); }
        @Override
        public void visit(NumericConstantToken token) { tokens.add(token); }
        @Override
        public void visit(ReferenceToken token) { tokens.add(token); }
        @Override
        public void visit(StartMarkerToken token) { tokens.add(token); }
        @Override
        public void visit(StringConstantToken token) { tokens.add(token); }
        @Override
        public void visit(TextToken token) { tokens.add(token); }
    }

    protected void visit(StartStatementsBlockFragment fragment, Collection<Token> adds) {
        int minimalLineCount = fragment.getGroup().getMinimalLineCount();

        switch (fragment.getLineCount()) {
            case 0:
                tokens.addAll(adds);
                tokens.add(TextToken.SPACE);
                tokens.add(StartBlockToken.START_BLOCK);
                tokens.add(TextToken.SPACE);
                break;
            case 1:
                if (minimalLineCount == 0) {
                    tokens.add(NewLineToken.NEWLINE_1);
                    tokens.addAll(adds);
                    tokens.add(TextToken.SPACE);
                    tokens.add(StartBlockToken.START_BLOCK);
                    tokens.add(TextToken.SPACE);
                } else {
                    tokens.addAll(adds);
                    tokens.add(TextToken.SPACE);
                    tokens.add(StartBlockToken.START_BLOCK);
                    tokens.add(NewLineToken.NEWLINE_1);
                }
                break;
            case 2:
                switch (minimalLineCount) {
                    case 0:
                        tokens.add(NewLineToken.NEWLINE_2);
                        tokens.addAll(adds);
                        tokens.add(TextToken.SPACE);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(TextToken.SPACE);
                        break;
                    case 1:
                        tokens.add(NewLineToken.NEWLINE_1);
                        tokens.addAll(adds);
                        tokens.add(TextToken.SPACE);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_1);
                        break;
                    default:
                        tokens.addAll(adds);
                        tokens.add(NewLineToken.NEWLINE_1);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(NewLineToken.NEWLINE_1);
                }
                break;
            default:
                switch (minimalLineCount) {
                    case 0:
                        tokens.add(new NewLineToken(fragment.getLineCount()));
                        tokens.addAll(adds);
                        tokens.add(TextToken.SPACE);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(TextToken.SPACE);
                        break;
                    case 1:
                        tokens.addAll(adds);
                        tokens.add(TextToken.SPACE);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(new NewLineToken(fragment.getLineCount()));
                        break;
                    default:
                        tokens.addAll(adds);
                        tokens.add(NewLineToken.NEWLINE_1);
                        tokens.add(StartBlockToken.START_BLOCK);
                        tokens.add(new NewLineToken(fragment.getLineCount() - 1));
                }
                break;
        }
    }
}
