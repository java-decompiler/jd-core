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

import jd.core.util.CharArrayUtil;
import jd.core.util.StringConstants;

public class FieldNameGenerator 
{
	public static String GenerateName(
		String signature, String name)
	{
		StringBuffer sbName = new StringBuffer(StringConstants.JD_FIELD_PREFIX);	
		
		sbName.append(name);
		sbName.append("_of_type");
		
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;
		
		GenerateName(sbName, caSignature, length, 0);
		
		return sbName.toString();
	}	
	
	private static int GenerateName(
		StringBuffer sbName, char[] caSignature, int length, int index)
	{
		char c;
		int beginIndex;		
		
		sbName.append('_');
		
		while (true)
		{
			int nbrOfDimensions = 0;

			while (caSignature[index] == '[')
			{
				index++;
				nbrOfDimensions++;
			}
			
			if (nbrOfDimensions > 0)
			{
				sbName.append("Array");
				if (nbrOfDimensions > 1)
				{
					sbName.append(nbrOfDimensions);
					sbName.append('d');
				}
				sbName.append("Of");
			}			
			
			switch(caSignature[index]) 
			{
			case 'B' : 
				sbName.append("Byte");
				index++;
				break;
			case 'C' :
				sbName.append("Char");
				index++;
				break;
			case 'D' :
				sbName.append("Double");
				index++;
				break;
			case 'F' : 
				sbName.append("Float");
				index++;
				break;
			case 'I' : 
				sbName.append("Int");
				index++;
				break;
			case 'J' : 
				sbName.append("Long");
				index++;
				break;
			case 'L' : 
			case '.' : 
				beginIndex = ++index;
				c = '.';
				
				// Recherche de ; ou de <
				while (index < length)
				{
					c = caSignature[index];
					if ((c == ';') || (c == '<'))
						break;
					index++;
				}
				
				String internalClassName = 
					CharArrayUtil.Substring(caSignature, beginIndex, index);
				InternalClassNameToCapitalizedClassName(
							sbName, internalClassName);

				if (c == '<')
				{
					sbName.append("_of");
					index = GenerateName(
							sbName, caSignature, length, index+1);
					
					while (caSignature[index] != '>')
					{
						sbName.append("_and");
						index = GenerateName(
							sbName, caSignature, length, index);
					}		
					
					// pass '>'
					index++;
				}
				
				// pass ';'
				if (caSignature[index] == ';')
					index++;
				break;
			case 'S' : 
				sbName.append("Short");
				index++;
				break;
			case 'T' :
				beginIndex = ++index;
				index = CharArrayUtil.IndexOf(caSignature, ';', beginIndex);
				sbName.append(caSignature, beginIndex, index-beginIndex);
				index++;
				break;
			case 'V' : 
				sbName.append("Void");
				index++;
				break;			
			case 'Z' : 
				sbName.append("Boolean");
				index++;
				break;
			case '-' :
				sbName.append("_super_");
				index = GenerateName(sbName, caSignature, length, index+1);
				break;
			case '+' :
				sbName.append("_extends_");
				index = GenerateName(sbName, caSignature, length, index+1);
				break;
			case '*' :
				sbName.append('X');
				index++;
				break;
			case 'X' :
			case 'Y' :
				sbName.append('X');
				System.err.println("<UNDEFINED>");
				index++;
				break;
			default:
				// DEBUG
				new Throwable(
					"SignatureWriter.WriteSignature: invalid signature '" + 
					String.valueOf(caSignature) + "'").printStackTrace();
				// DEBUG
			}
						
			if ((index >= length) || (caSignature[index] != '.'))
				break;
			
			sbName.append("_");
		}
		
		return index;		
	}
	
	private static void InternalClassNameToCapitalizedClassName(
			StringBuffer sbName, String internalClassName)
	{
		int index1 = 0;
		int index2 = internalClassName.indexOf(
			StringConstants.INTERNAL_PACKAGE_SEPARATOR);
		
		while (index2 != -1)
		{
			sbName.append(Character.toUpperCase(
				internalClassName.charAt(index1)));
			sbName.append(internalClassName.substring(index1+1, index2));
			index1 = index2+1;
			index2 = internalClassName.indexOf(
					StringConstants.INTERNAL_PACKAGE_SEPARATOR, index1);
		}
		
		sbName.append(Character.toUpperCase(
				internalClassName.charAt(index1)));
		sbName.append(internalClassName.substring(index1+1));		
	}
}
