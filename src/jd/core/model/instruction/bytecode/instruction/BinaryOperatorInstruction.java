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
package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;

/* Priority  Operator   Operation                            Order of Evaluation
 * 1         [ ]        Array index                          Left to Right
 *           ()         Method call
 *           .          Member access
 * 2         ++         Prefix or postfix increment          Right to Left
 *           --         Prefix or postfix decrement
 *           + -        Unary plus, minus
 *           ~          Bitwise NOT
 *           !          Boolean (logical) NOT
 *           (type)     Type cast
 *           new        Object creation
 * 3         * / %      Multiplication, division, remainder  Left to Right
 * 4         + -        Addition, subtraction                Left to Right
 *           +          String concatenation
 * 5         <<         Signed bit shift left to right       Left to Right
 *           >>         Signed bit shift right to left
 *           >>>        Unsigned bit shift right to left
 * 6         < <=       Less than, less than or equal to     Left to Right
 *           > >=       Greater than, greater than or equal to
 *           instanceof Reference test
 * 7         ==         Equal to                             Left to Right
 *           !=         Not equal to
 * 8         &          Bitwise AND                          Left to Right
 *           &          Boolean (logical) AND
 * 9         ^          Bitwise XOR                          Left to Right
 *           ^          Boolean (logical) XOR
 * 10        |          Bitwise OR                           Left to Right
 *           |          Boolean (logical) OR
 * 11        &&         Boolean (logical) AND                Left to Right
 * 12        ||         Boolean (logical) OR                 Left to Right
 * 13        ? :        Conditional                          Right to Left
 * 14        =          Assignment                           Right to Left
 *           *= /= +=   Combinated assignment
 *           -= %=      (operation and assignment)
 *           <<= >>= 
 *           >>>=
 *           &= ^= |=  
 *           
 * http://www.java-tips.org/java-se-tips/java.lang/what-is-java-operator-precedence.html
 */
public class BinaryOperatorInstruction extends Instruction 
{
	private int priority;
	public String signature;
	public String operator;
	public Instruction value1;
	public Instruction value2;

	public BinaryOperatorInstruction(
			int opcode, int offset, int lineNumber, int priority, 
			String signature, String operator, 
			Instruction value1, Instruction value2)
	{
		super(opcode, offset, lineNumber);
		this.priority = priority;
		this.signature = signature;
		this.operator = operator;
		this.value1 = value1;
		this.value2 = value2;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{		
		return this.signature;
	}

	public int getPriority()
	{
		return this.priority;
	}
}
