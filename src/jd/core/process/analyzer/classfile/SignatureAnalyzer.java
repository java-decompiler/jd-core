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
package jd.core.process.analyzer.classfile;

import jd.core.model.reference.ReferenceMap;
import jd.core.util.CharArrayUtil;
import jd.core.util.SignatureFormatException;


public class SignatureAnalyzer 
{
	public static void AnalyzeClassSignature(
			ReferenceMap referenceMap, String signature)
	{
		try // DEBUG //
		{
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;
		int index = 0;
		
		// Generics
		index = AnalyzeGenerics(referenceMap, caSignature, length, index);
		
		// Superclass
		index = AnalyzeSignature(referenceMap, caSignature, length, index);

		//Interfaces
		while (index < signature.length())
			index = AnalyzeSignature(referenceMap, caSignature, length, index);
		}
		catch (RuntimeException e) // DEBUG //
		{
			System.err.println("SignatureAnalyzer.AnalyzeClassSignature: Infinite loop, signature=" + signature);
			throw e;
		}
	}
	
	public static void AnalyzeMethodSignature(
			ReferenceMap referenceMap, String signature)
	{
		try // DEBUG //
		{
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;
		int index = 0;
		
		// Affichage des generics
		index = AnalyzeGenerics(referenceMap, caSignature, length, index);
		
		if (caSignature[index] != '(')
			throw new SignatureFormatException(signature);

		// pass '('
		index++;

		// Arguments
		while (caSignature[index] != ')')
			index = AnalyzeSignature(referenceMap, caSignature, length, index);
		
		// pass ')'
		index++;
		
		AnalyzeSignature(referenceMap, caSignature, length, index);
		}
		catch (RuntimeException e) // DEBUG //
		{
			System.err.println("SignatureAnalyzer.AnalyzeMethodSignature: Infinite loop, signature=" + signature);
			throw e;
		}
	}

	public static void AnalyzeSimpleSignature(
			ReferenceMap referenceMap, String signature)
	{
		try // DEBUG //
		{
		char[] caSignature = signature.toCharArray();
		AnalyzeSignature(referenceMap, caSignature, caSignature.length, 0);	
		}
		catch (RuntimeException e) // DEBUG //
		{
			System.err.println("SignatureAnalyzer.AnalyzeSimpleSignature: Infinite loop, signature=" + signature);
			throw e;
		}
	}
	
	private static int AnalyzeGenerics(
		ReferenceMap referenceMap, char[] caSignature, int length, int index)
	{
		if (caSignature[index] == '<')
		{
			index++;
			
			while (index < length)
			{
				index = CharArrayUtil.IndexOf(caSignature, ':', index) + 1;
				
				// Mystere ...
				if (caSignature[index] == ':')
					index++;
					
				index = AnalyzeSignature(referenceMap, caSignature, length, index);
				
				if (caSignature[index] == '>')
					break;
			}			

			index++;
		}

		return index;
	}
	
	private static int AnalyzeSignature(
		ReferenceMap referenceMap, char[] caSignature, int length, int index)
	{
		int debugCounter = 0; // DEBUG //
		char c;
		
		while (true)
		{
			// Retrait des prefixes de tableau : '[[?' ou '[L[?;'
			if (caSignature[index] == '[')
			{
				while (++index < length)
				{
					if ((caSignature[index] == 'L') && 
						(index+1 < length) && 
						(caSignature[index+1] == '['))
					{
						index++;
						length--;
					}
					else if (caSignature[index] != '[')
					{
						break;
					}
				}
			}
			
			switch(caSignature[index]) 
			{
			case 'L' : case '.' : 
				boolean classFlag = (caSignature[index] == 'L');
				int beginIndex = ++index;
				c = '.';
				
				// Recherche de ; ou de <
				while (index < length)
				{
					c = caSignature[index];
					if ((c == ';') || (c == '<'))
						break;
					index++;
				}
				
				if (classFlag)
					referenceMap.add(
						CharArrayUtil.Substring(caSignature, beginIndex, index));

				if (c == '<')
				{					
					// pass '<'
					index++;
					
					while (caSignature[index] != '>')
						index = AnalyzeSignature(
							referenceMap, caSignature, length, index);
					
					// pass '>'
					index++;
				}
				
				// pass ';'
				if (caSignature[index] == ';')
					index++;
				break;
			case '-' : case '+' :
				index = AnalyzeSignature(
							referenceMap, caSignature, length, index+1);
				break;
			case 'T' :			
				index = CharArrayUtil.IndexOf(caSignature, ';', index+1) + 1;
				break;
			case 'B' : case 'C' : case 'D' : case 'F' : case 'I' : 
			case 'J' : case 'S' : case 'V' : case 'Z' : case '*' :
				index++;				
			}

			if ((index >= length) || (caSignature[index] != '.'))
				break;
			
			debugCounter++;
			
			if (debugCounter > 3000) // DEBUG //
				throw new RuntimeException("Infinite loop");
		}
		
		return index;
	}
}
