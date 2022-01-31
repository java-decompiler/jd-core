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
package jd.core.process.analyzer.instruction.fast;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Return;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastList;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTry;

/*
 * Le numéro de ligne des instructions 'return' genere par les compilateurs
 * sont faux et perturbe l'affichage des sources
 */
public final class ReturnLineNumberAnalyzer
{
    private ReturnLineNumberAnalyzer() {
        super();
    }

    public static void check(Method method)
    {
        List<Instruction> list = method.getFastNodes();
        int length = list.size();

        if (length > 1)
        {
            int afterListLineNumber = list.get(length-1).getLineNumber();

            if (afterListLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
            {
                recursiveCheck(list , afterListLineNumber);
            }
        }
    }

    private static void recursiveCheck(
        List<Instruction> list, int afterListLineNumber)
    {
        int index = list.size();

        // Appels recursifs
        while (index-- > 0)
        {
            Instruction instruction = list.get(index);

            switch (instruction.getOpcode())
            {
            case FastConstants.WHILE,
                 FastConstants.DO_WHILE,
                 FastConstants.INFINITE_LOOP,
                 FastConstants.FOR,
                 FastConstants.FOREACH,
                 FastConstants.IF_SIMPLE,
                 FastConstants.SYNCHRONIZED:
                {
                    List<Instruction> instructions =
                            ((FastList)instruction).getInstructions();
                    if (instructions != null) {
                        recursiveCheck(instructions, afterListLineNumber);
                    }
                }
                break;
            case FastConstants.IF_ELSE:
                {
                    FastTest2Lists ft2l = (FastTest2Lists)instruction;
                    recursiveCheck(ft2l.getInstructions(), afterListLineNumber);
                    recursiveCheck(ft2l.getInstructions2(), afterListLineNumber);
                }
                break;
            case FastConstants.SWITCH,
                 FastConstants.SWITCH_ENUM,
                 FastConstants.SWITCH_STRING:
                {
                    FastSwitch.Pair[] pairs = ((FastSwitch)instruction).getPairs();
                    if (pairs != null) {
                        for (int i=pairs.length-1; i>=0; --i)
                        {
                            List<Instruction> instructions = pairs[i].getInstructions();
                            if (instructions != null)
                            {
                                recursiveCheck(instructions, afterListLineNumber);
                                if (!instructions.isEmpty())
                                {
                                    afterListLineNumber =
                                        instructions.get(0).getLineNumber();
                                }
                            }
                        }
                    }
                }
                break;
            case FastConstants.TRY:
                {
                    FastTry ft = (FastTry)instruction;

                    if (ft.getFinallyInstructions() != null)
                    {
                        recursiveCheck(ft.getFinallyInstructions(), afterListLineNumber);
                        if (!ft.getFinallyInstructions().isEmpty())
                        {
                            afterListLineNumber =
                                ft.getFinallyInstructions().get(0).getLineNumber();
                        }
                    }

                    if (ft.getCatches() != null)
                    {
                        for (int i=ft.getCatches().size()-1; i>=0; --i)
                        {
                            List<Instruction> catchInstructions =
                                ft.getCatches().get(i).instructions();
                            recursiveCheck(
                                catchInstructions, afterListLineNumber);
                            if (!catchInstructions.isEmpty())
                            {
                                afterListLineNumber =
                                    catchInstructions.get(0).getLineNumber();
                            }
                        }
                    }

                    recursiveCheck(ft.getInstructions(), afterListLineNumber);
                }
                break;
            case Const.RETURN:
                {
                    Return r = (Return)instruction;
                    if (r.getLineNumber() > afterListLineNumber) {
                        r.setLineNumber(Instruction.UNKNOWN_LINE_NUMBER);
                    }
                }
                break;
            }

            afterListLineNumber = instruction.getLineNumber();
        }
    }
}
