/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.printer;

import java.util.ArrayList;
import java.util.List;

public class InstructionPrinter implements Printer
{
    private final Printer printer;
    private int previousLineNumber;
    private boolean newInstruction;
    private boolean multiLineInstruction;
    private boolean active;
    private final List<Boolean> states;

    /*
     * L'etat se reduit a  "multiLineInstruction"
     * --> Optimisation: utilisation de Boolean a la place de State.
    private static class State
    {
        private boolean newInstruction;
        private boolean multiLineInstruction;

        public State(boolean newInstruction, boolean multiLineInstruction)
        {
            this.newInstruction = newInstruction;
            this.multiLineInstruction = multiLineInstruction;
        }
    } */

    // -------------------------------------------------------------------------
    public InstructionPrinter(Printer printer)
    {
        this.printer = printer;
        this.active = false;
        this.states = new ArrayList<>(0);
    }

    public void init(int previousLineNumber)
    {
        this.previousLineNumber = previousLineNumber;
        this.newInstruction = false;
        this.multiLineInstruction = false;
        this.active = false;
    }

    public void startOfInstruction()
    {
        this.active = true;
    }

    public void addNewLinesAndPrefix(int lineNumber)
    {
        if (!this.active)
        {
            // Instruction non commencée, en cours d'affichage. Restoration de
            // l'état précédent.
            this.multiLineInstruction = this.states.remove(this.states.size()-1);
            /* State state = this.states.remove(this.states.size()-1);
            this.newInstruction = state.newInstruction;
            this.multiLineInstruction = state.multiLineInstruction; */
            this.active = true;
        }

        if (lineNumber == UNKNOWN_LINE_NUMBER)
        {
            if (this.newInstruction)
            {
                if (this.previousLineNumber == UNKNOWN_LINE_NUMBER)
                {
                    this.printer.endOfLine();
                    this.printer.startOfLine(lineNumber);
                }
                else
                {
                    this.printer.print(' ');
                }
            }
        } else if (this.previousLineNumber == UNKNOWN_LINE_NUMBER)
        {
            this.previousLineNumber = lineNumber;
        } else if (this.previousLineNumber < lineNumber)
        {
            int lineCount = lineNumber - this.previousLineNumber;

            this.printer.endOfLine();

            if (lineCount > 1)
            {
                this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                this.printer.endOfLine();

                if (lineCount > 2)
                {
                    this.printer.extraLine(lineCount-2);
                }
            }

            if (!this.newInstruction &&
                !this.multiLineInstruction)
            {
                this.printer.indent();
                this.multiLineInstruction = true;
            }

            this.printer.startOfLine(lineNumber);

            this.previousLineNumber = lineNumber;
        }

        this.newInstruction = false;
    }

    public void endOfInstruction()
    {
        if (this.multiLineInstruction)
        {
            this.printer.desindent();
        }

        this.newInstruction = true;
        this.multiLineInstruction = false;
        this.active = false;
    }

    public void release()
    {
        if (this.active)
        {
            // Instruction non terminée. Sauvegarde de l'état courant.
            this.states.add(this.multiLineInstruction);
            /* this.states.add(
                new State(this.newInstruction, this.multiLineInstruction)); */
        }
    }

    // -------------------------------------------------------------------------
    public void print(int lineNumber, char c)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.print(c);
    }

    public void print(int lineNumber, int i)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.print(i);
    }

    public void print(int lineNumber, String s)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.print(s);
    }

    public void printNumeric(int lineNumber, String s)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printNumeric(s);
    }

    public void printString(int lineNumber, String s, String scopeInternalName)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printString(s, scopeInternalName);
    }

    public void printKeyword(int lineNumber, String keyword)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printKeyword(keyword);
    }

    public void printType(
            int lineNumber, String internalName,
            String name, String scopeInternalName)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printType(internalName, name, scopeInternalName);
    }

    public void printField(
            int lineNumber, String internalName, String name,
            String descriptor, String scopeInternalName)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printField(
            internalName, name, descriptor, scopeInternalName);
    }

    public void printStaticField(
            int lineNumber, String internalName, String name,
            String descriptor, String scopeInternalName)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printStaticField(
            internalName, name, descriptor, scopeInternalName);
    }

    public void printMethod(
            int lineNumber, String internalName, String name,
            String descriptor, String scopeInternalName)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printMethod(
            internalName, name, descriptor, scopeInternalName);
    }

    public void printStaticMethod(
            int lineNumber, String internalName, String name,
            String descriptor, String scopeInternalName)
    {
        addNewLinesAndPrefix(lineNumber);
        this.printer.printStaticMethod(
            internalName, name, descriptor, scopeInternalName);
    }

    // -------------------------------------------------------------------------
    @Override
    public void print(byte b) { this.printer.print(b); }
    @Override
    public void print(char c) { this.printer.print(c); }
    @Override
    public void print(int i) { this.printer.print(i); }
    @Override
    public void print(String s) { this.printer.print(s); }

    @Override
    public void printNumeric(String s) { this.printer.printNumeric(s); }
    @Override
    public void printString(String s, String scopeInternalName) { this.printer.printString(s, scopeInternalName); }
    @Override
    public void printKeyword(String keyword) { this.printer.printKeyword(keyword); }
    @Override
    public void printJavaWord(String s) { this.printer.printJavaWord(s); }

    // TODO pourquoi de temps en temps passer un 'internalName' et d'autre fois un 'internalPath'? comprendre les besoin de jd-gui.
    @Override
    public void printType(String internalName, String name, String scopeInternalName)
        { this.printer.printType(internalName, name, scopeInternalName); }
    @Override
    public void printTypeDeclaration(String internalName, String name)
        { this.printer.printTypeDeclaration(internalName, name); }
    @Override
    public void printTypeImport(String internalName, String name)
        { this.printer.printTypeImport(internalName, name); }

    @Override
    public void printField(
            String internalName, String name,
            String descriptor, String scopeInternalName)
        { this.printer.printField(internalName, name, descriptor, scopeInternalName); }
    @Override
    public void printFieldDeclaration(
            String internalName, String name, String descriptor)
        { this.printer.printFieldDeclaration(internalName, name, descriptor); }

    @Override
    public void printStaticField(
            String internalName, String name,
            String descriptor, String scopeInternalName)
        { this.printer.printStaticField(internalName, name, descriptor, scopeInternalName); }
    @Override
    public void printStaticFieldDeclaration(
            String internalName, String name, String descriptor)
        { this.printer.printStaticFieldDeclaration(internalName, name, descriptor); }

    @Override
    public void printConstructor(
            String internalName, String name,
            String descriptor, String scopeInternalName)
        { this.printer.printConstructor(internalName, name, descriptor, scopeInternalName); }
    @Override
    public void printConstructorDeclaration(
            String internalName, String name, String descriptor)
        { this.printer.printConstructorDeclaration(internalName, name, descriptor); }
    @Override
    public void printStaticConstructorDeclaration(
            String internalName, String name)
        { this.printer.printStaticConstructorDeclaration(internalName, name); }

    @Override
    public void printMethod(
            String internalName, String name,
            String descriptor, String scopeInternalName)
        { this.printer.printMethod(internalName, name, descriptor, scopeInternalName); }
    @Override
    public void printMethodDeclaration(
            String internalName, String name, String descriptor)
        { this.printer.printMethodDeclaration(internalName, name, descriptor); }

    @Override
    public void printStaticMethod(
            String internalName, String name,
            String descriptor, String scopeInternalName)
        { this.printer.printStaticMethod(internalName, name, descriptor, scopeInternalName); }
    @Override
    public void printStaticMethodDeclaration(
            String internalName, String name, String descriptor)
        { this.printer.printStaticMethodDeclaration(internalName, name, descriptor); }

    @Override
    public void start(int maxLineNumber, int majorVersion, int minorVersion)
        { this.printer.start(maxLineNumber, majorVersion, minorVersion); }
    @Override
    public void end() { this.printer.end(); }

    @Override
    public void indent() { this.printer.indent(); }
    @Override
    public void desindent() { this.printer.desindent(); }

    @Override
    public void startOfLine(int lineNumber)
        { this.printer.startOfLine(lineNumber); }
    @Override
    public void endOfLine() { this.printer.endOfLine(); }
    @Override
    public void extraLine(int count) { this.printer.extraLine(count); }

    @Override
    public void startOfComment() { this.printer.startOfComment(); }
    @Override
    public void endOfComment() { this.printer.endOfComment(); }

    @Override
    public void startOfJavadoc() { this.printer.startOfJavadoc(); }
    @Override
    public void endOfJavadoc() { this.printer.endOfJavadoc(); }

    @Override
    public void startOfXdoclet() { this.printer.startOfXdoclet(); }
    @Override
    public void endOfXdoclet() { this.printer.endOfXdoclet(); }

    @Override
    public void startOfError() { this.printer.startOfError(); }
    @Override
    public void endOfError() { this.printer.endOfError(); }

    @Override
    public void startOfImportStatements() { this.printer.startOfImportStatements(); }
    @Override
    public void endOfImportStatements() { this.printer.endOfImportStatements(); }

    @Override
    public void startOfTypeDeclaration(String internalPath) { this.printer.startOfTypeDeclaration(internalPath); }
    @Override
    public void endOfTypeDeclaration() { this.printer.endOfTypeDeclaration(); }

    @Override
    public void startOfAnnotationName() { this.printer.startOfAnnotationName(); }
    @Override
    public void endOfAnnotationName() { this.printer.endOfAnnotationName(); }

    @Override
    public void startOfOptionalPrefix() { this.printer.startOfOptionalPrefix(); }
    @Override
    public void endOfOptionalPrefix() { this.printer.endOfOptionalPrefix(); }

    @Override
    public void debugStartOfLayoutBlock() { this.printer.debugStartOfLayoutBlock(); }
    @Override
    public void debugEndOfLayoutBlock() { this.printer.debugEndOfLayoutBlock(); }
    @Override
    public void debugStartOfSeparatorLayoutBlock()
        { this.printer.debugStartOfSeparatorLayoutBlock(); }
    @Override
    public void debugEndOfSeparatorLayoutBlock(int min, int value, int max)
        { this.printer.debugEndOfSeparatorLayoutBlock(min, value, max); }
    @Override
    public void debugStartOfStatementsBlockLayoutBlock()
        { this.printer.debugStartOfStatementsBlockLayoutBlock(); }
    @Override
    public void debugEndOfStatementsBlockLayoutBlock(int min, int value, int max)
        { this.printer.debugEndOfStatementsBlockLayoutBlock(min, value, max); }
    @Override
    public void debugStartOfInstructionBlockLayoutBlock()
        { this.printer.debugStartOfInstructionBlockLayoutBlock(); }
    @Override
    public void debugEndOfInstructionBlockLayoutBlock()
        { this.printer.debugEndOfInstructionBlockLayoutBlock(); }
    @Override
    public void debugStartOfCommentDeprecatedLayoutBlock()
        { this.printer.debugStartOfCommentDeprecatedLayoutBlock(); }
    @Override
    public void debugEndOfCommentDeprecatedLayoutBlock()
        { this.printer.debugEndOfCommentDeprecatedLayoutBlock(); }
    @Override
    public void debugMarker(String marker)
        { this.printer.debugMarker(marker); }
    @Override
    public void debugEndOfCaseBlockLayoutBlock()
        { this.printer.debugEndOfCaseBlockLayoutBlock(); }
    @Override
    public void debugStartOfCaseBlockLayoutBlock()
        { this.printer.debugStartOfCaseBlockLayoutBlock(); }
}
