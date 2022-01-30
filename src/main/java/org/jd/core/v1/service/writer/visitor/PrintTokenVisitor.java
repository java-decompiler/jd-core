/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.writer.visitor;

import org.jd.core.v1.api.printer.Printer;
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

import java.util.List;

import static org.jd.core.v1.api.printer.Printer.UNKNOWN_LINE_NUMBER;

public class PrintTokenVisitor implements TokenVisitor {

    private final SearchLineNumberVisitor searchLineNumberVisitor = new SearchLineNumberVisitor();

    private Printer printer;
    private List<Token> tokens;
    private int index;
    private int newLineCount;

    public void start(Printer printer, List<Token> tokens) {
        this.printer = printer;
        this.tokens = tokens;
        this.index = 0;
        this.newLineCount = 0;
        printer.startLine(searchLineNumber());
    }

    public void end() {
        printer.endLine();
    }

    @Override
    public void visit(BooleanConstantToken token) {
        prepareNewLine();
        printer.printKeyword(token.value() ? "true" : "false");
        index++;
    }

    @Override
    public void visit(CharacterConstantToken token) {
        prepareNewLine();
        printer.printStringConstant('\'' + token.getCharacter() + '\'', token.getOwnerInternalName());
        index++;
    }

    @Override
    public void visit(DeclarationToken token) {
        prepareNewLine();
        printer.printDeclaration(token.getType(), token.getInternalTypeName(), token.getName(), token.getDescriptor());
        index++;
    }

    @Override
    public void visit(StartBlockToken token) {
        prepareNewLine();
        printer.printText(token.text());
        printer.indent();
        if (token == StartBlockToken.START_RESOURCES_BLOCK) {
            printer.indent();
        }
        index++;
    }

    @Override
    public void visit(EndBlockToken token) {
        printer.unindent();
        if (token == EndBlockToken.END_RESOURCES_BLOCK) {
            printer.unindent();
        }
        prepareNewLine();
        printer.printText(token.text());
        index++;
    }

    @Override
    public void visit(StartMarkerToken token) {
        prepareNewLine();
        printer.startMarker(token.type());
        index++;
    }

    @Override
    public void visit(EndMarkerToken token) {
        prepareNewLine();
        printer.endMarker(token.type());
        index++;
    }

    @Override
    public void visit(NewLineToken token) {
        newLineCount += token.count();
        index++;
    }

    @Override
    public void visit(KeywordToken token) {
        prepareNewLine();
        printer.printKeyword(token.keyword());
        index++;
    }

    @Override
    public void visit(LineNumberToken token) {
        index++;
    }

    @Override
    public void visit(NumericConstantToken token) {
        prepareNewLine();
        printer.printNumericConstant(token.text());
        index++;
    }

    @Override
    public void visit(ReferenceToken token) {
        prepareNewLine();
        printer.printReference(token.getType(), token.getInternalTypeName(), token.getName(), token.getDescriptor(), token.getOwnerInternalName());
        index++;
    }

    @Override
    public void visit(StringConstantToken token) {
        prepareNewLine();
        printer.printStringConstant('"' + token.text() + '"', token.getOwnerInternalName());
        index++;
    }

    @Override
    public void visit(TextToken token) {
        prepareNewLine();
        printer.printText(token.text());
        index++;
    }

    protected void prepareNewLine() {
        if (newLineCount > 0) {
            printer.endLine();

            if (newLineCount > 2) {
                printer.extraLine(newLineCount-2);
                newLineCount = 2;
            }

            if (newLineCount > 1) {
                printer.startLine(UNKNOWN_LINE_NUMBER);
                printer.endLine();
            }

            printer.startLine(searchLineNumber());
            newLineCount = 0;
        }
    }

    protected int searchLineNumber() {
        // Backward search
        searchLineNumberVisitor.reset();

        for (int i=index; i>=0; i--) {
            tokens.get(i).accept(searchLineNumberVisitor);

            if (searchLineNumberVisitor.lineNumber != UNKNOWN_LINE_NUMBER) {
                return searchLineNumberVisitor.lineNumber;
            }
            if (searchLineNumberVisitor.newLineCounter > 0) {
                break;
            }
        }

        // Forward search
        searchLineNumberVisitor.reset();

        int size = tokens.size();

        for (int i=index; i<size; i++) {
            tokens.get(i).accept(searchLineNumberVisitor);

            if (searchLineNumberVisitor.lineNumber != UNKNOWN_LINE_NUMBER) {
                return searchLineNumberVisitor.lineNumber;
            }
            if (searchLineNumberVisitor.newLineCounter > 0) {
                break;
            }
        }

        return UNKNOWN_LINE_NUMBER;
    }

    protected static class SearchLineNumberVisitor extends AbstractNopTokenVisitor {
        private int lineNumber;
        private int newLineCounter;

        public void reset() {
            this.lineNumber = UNKNOWN_LINE_NUMBER;
            this.newLineCounter = 0;
        }

        @Override
        public void visit(LineNumberToken token) {
            lineNumber = token.lineNumber();
        }

        @Override
        public void visit(NewLineToken token) {
            newLineCounter++;
        }
    }
}
