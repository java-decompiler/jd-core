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
package jd.core.process.layouter.visitor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.LambdaInstruction;
import jd.core.model.layout.block.FragmentLayoutBlock;
import jd.core.model.layout.block.InstructionsLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.model.layout.block.LayoutBlockConstants;
import jd.core.preferences.Preferences;
import jd.core.process.layouter.ClassFileLayouter;

public class InstructionsSplitterVisitor extends BaseInstructionSplitterVisitor
{
    private Preferences preferences;
    private List<LayoutBlock> layoutBlockList;
    private Method method;
    private List<Instruction> list;
    private int firstLineNumber;
    private int maxLineNumber;
    private int initialIndex1;
    private int index1;
    private int index2;
    private int offset1;

    public void start(
        Preferences preferences,
        List<LayoutBlock> layoutBlockList, ClassFile classFile,
        Method method, List<Instruction> list, int index1)
    {
        super.start(classFile);

        this.preferences = preferences;
        this.layoutBlockList = layoutBlockList;
        this.method = method;
        this.list = list;
        this.firstLineNumber = this.maxLineNumber =
            Instruction.UNKNOWN_LINE_NUMBER;
        this.initialIndex1 = this.index1 = index1;
        this.offset1 = 0;
    }

    public void end()
    {
        int lastOffset = this.list.get(this.index2).getOffset();

        // S'il reste un fragment d'instruction a traiter...
        if (this.index1 != this.index2 || this.offset1 != lastOffset || lastOffset == 0)
        {
            // Add last part of instruction
            int lastLineNumber = Instruction.UNKNOWN_LINE_NUMBER;

            for (int j=index2; j>=index1; j--)
            {
                Instruction instruction = list.get(j);
                if (instruction.getLineNumber() != Instruction.UNKNOWN_LINE_NUMBER)
                {
                    lastLineNumber = MaxLineNumberVisitor.visit(instruction);
                    break;
                }
            }
            if (lastOffset == 0) {
                addInstructionsLayoutBlock(lastLineNumber, 1);
                layoutBlockList.add(new FragmentLayoutBlock(
                        LayoutBlockConstants.FRAGMENT_SEMICOLON));
            } else {
                addInstructionsLayoutBlock(lastLineNumber, lastOffset);
            }
        }
    }

    public void setIndex2(int index2)
    {
        this.index2 = index2;
    }

    @Override
    public void visit(Instruction instruction)
    {
        if (this.firstLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
        {
            // Bloc exécuté soit lors de la visite de
            // - du 1er statement
            // - d'un statement qui suit un statement dont la derniere
            //   instruction est 'AnonymousNewInvoke'
            // Assez complexe a comprendre sans exemple sous les yeux
            // Methode d'exemple :
            //   java.io.ObjectInputStream, auditSubclass(...)
            int initialFirstLineNumber =
                this.list.get(this.initialIndex1).getLineNumber();

            if (initialFirstLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
            {
                // Si la méthode possède des numéros de lignes
                if (initialFirstLineNumber < instruction.getLineNumber())
                {
                    // Cas d'un statement qui suit un statement dont la derniere
                    //   instruction est 'AnonymousNewInvoke' ==> on fait
                    //   commencer le bloc a la ligne precedent.
                    this.firstLineNumber = instruction.getLineNumber() - 1;
                }
                else
                {
                    // Cas du 1er statement
                    this.firstLineNumber = instruction.getLineNumber();
                }
            }
        }

        super.visit(null, instruction);
    }

    @Override
    protected void visit(Instruction parent, Instruction instruction)
    {
        if (instruction.getLineNumber() == Instruction.UNKNOWN_LINE_NUMBER)
        {
            instruction.setLineNumber(this.maxLineNumber);
        }
        else if (this.maxLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
        {
            this.maxLineNumber = instruction.getLineNumber();
        }
        else if (instruction.getLineNumber() < this.maxLineNumber)
        {
            // Modification du numéro de ligne fournit dans le fichier CLASS !
            instruction.setLineNumber(this.maxLineNumber);
        }

        if (this.firstLineNumber == Instruction.UNKNOWN_LINE_NUMBER)
        {
            // Bloc exécuté si une instruction 'AnonymousNewInvoke' vient
            // d'être traitée.
            this.firstLineNumber = instruction.getLineNumber();
        }

        super.visit(parent, instruction);
    }

    @Override
    public void visitAnonymousNewInvoke(
        Instruction parent, InvokeNew in, ClassFile innerClassFile)
    {
        // Add a new part of instruction
        addInstructionsLayoutBlock(in.getLineNumber(), in.getOffset());

        // Add blocks for inner class body
        this.maxLineNumber =
            ClassFileLayouter.createBlocksForBodyOfAnonymousClass(
                this.preferences, innerClassFile, this.layoutBlockList);

        this.firstLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
        this.index1 = this.index2;
        this.offset1 = in.getOffset();
    }
    
    @Override
    public void visitAnonymousLambda(
            Instruction parent, LambdaInstruction in)
    {
        // Add a new part of instruction
        addInstructionsLayoutBlock(in.getLineNumber(), in.getOffset());
        
        // Add blocks for lambda method body
        this.maxLineNumber =
                ClassFileLayouter.createBlocksForBodyOfLambda(
                        this.preferences, in, this.layoutBlockList);
        
        this.firstLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
        this.index1 = this.index2;
        this.offset1 = in.getOffset();
    }

    protected void addInstructionsLayoutBlock(int lastLineNumber, int lastOffset)
    {
        int preferedLineCount;

        if (this.firstLineNumber != Instruction.UNKNOWN_LINE_NUMBER &&
            lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
        {
            if (lastLineNumber < this.firstLineNumber)
            {
                // Les instructions newAnonymousClass imbriquées n'ont pas de
                // numéros de ligne correctes. Exemple: com.googlecode.dex2jar.v3.Dex2jar
                lastLineNumber = this.firstLineNumber;
            }
            preferedLineCount = lastLineNumber - this.firstLineNumber;
        }
        else
        {
            preferedLineCount = LayoutBlockConstants.UNLIMITED_LINE_COUNT;
        }

        this.layoutBlockList.add(new InstructionsLayoutBlock(
            this.firstLineNumber, lastLineNumber,
            preferedLineCount, preferedLineCount, preferedLineCount,
            this.classFile, this.method, this.list,
            this.index1, this.index2,
            this.offset1, lastOffset));
    }
}
