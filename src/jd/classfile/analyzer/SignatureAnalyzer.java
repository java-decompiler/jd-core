package jd.classfile.analyzer;

import java.util.ArrayList;

import jd.exception.SignatureFormatException;
import jd.instruction.bytecode.ByteCodeConstants;
import jd.util.CharArrayUtil;
import jd.util.ReferenceMap;


public class SignatureAnalyzer 
{
	public static void AnalyzeClassSignature(
			ReferenceMap referenceMap, String signature)
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
	
	public static void AnalyzeMethodSignature(
			ReferenceMap referenceMap, String signature)
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

	public static ArrayList<String> GetParameterSignatures(
			String methodSignature)
	{
		char[] caSignature = methodSignature.toCharArray();
		int length = caSignature.length;
		ArrayList<String> parameterTypes = new ArrayList<String>(1);
		int index = CharArrayUtil.IndexOf(caSignature, '(', 0);
		
		if (index != -1)
		{
			// pass '('
			index++;
	
			// Arguments
			while (caSignature[index] != ')')
			{
				int newIndex = 
					AnalyzeSignature(null, caSignature, length, index);
				parameterTypes.add(methodSignature.substring(index, newIndex));
				index = newIndex;
			}
		}
		
		return parameterTypes;
	}

	public static int GetParameterSignatureCount(String methodSignature)
	{
		char[] caSignature = methodSignature.toCharArray();
		int length = caSignature.length;
		int index = CharArrayUtil.IndexOf(caSignature, '(', 0);
		int count = 0;
		
		if (index != -1)
		{
			// pass '('
			index++;
	
			// Arguments
			while (caSignature[index] != ')')
			{
				int newIndex = 
					AnalyzeSignature(null, caSignature, length, index);
				index = newIndex;
				count++;
			}
		}
		
		return count;
	}
	
	public static String GetMethodReturnedSignature(String signature)
	{
		int index = signature.indexOf(')');
		if (index == -1)
			return null;
		
		return signature.substring(index + 1);
	}

	public static void AnalyzeSimpleSignature(
			ReferenceMap referenceMap, String signature)
	{
		char[] caSignature = signature.toCharArray();
		AnalyzeSignature(referenceMap, caSignature, caSignature.length, 0);	
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
					
				index = 
					AnalyzeSignature(referenceMap, caSignature, length, index);
				
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
		char c;
		int beginIndex;
		
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
			case 'L' : 
			case '.' : 
				boolean classFlag = (caSignature[index] == 'L');
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
				
				if ((referenceMap != null) && classFlag)
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
				beginIndex = ++index;				
				index = AnalyzeSignature(
						referenceMap, caSignature, length, beginIndex);
				break;
			case 'T' :
				beginIndex = ++index;				
				index = CharArrayUtil.IndexOf(caSignature, ';', beginIndex) + 1;
				break;
			case 'B' : case 'C' : case 'D' : case 'F' : case 'I' : 
			case 'J' : case 'S' : case 'V' : case 'Z' : case '*' :
				index++;				
			}

			if ((index >= length) || (caSignature[index] != '.'))
				break;
		}
		
		return index;
	}
	
	public static String GetSignatureFromType(int type) 
	{
		switch (type)
		{
		case ByteCodeConstants.T_BOOLEAN: return "Z";
		case ByteCodeConstants.T_CHAR:    return "C";
		case ByteCodeConstants.T_FLOAT:   return "F";
		case ByteCodeConstants.T_DOUBLE:  return "D";
		case ByteCodeConstants.T_BYTE:    return "B";
		case ByteCodeConstants.T_SHORT:   return "S";
		case ByteCodeConstants.T_INT:     return "I";
		case ByteCodeConstants.T_LONG:    return "J";
		default:                          return null;
		}
	}
	
	public static int GetTypeFromSignature(String signature) 
	{
		if (signature.length() != 1)
			return 0;
		
		switch (signature.charAt(0))
		{
		case 'Z': return ByteCodeConstants.T_BOOLEAN;
		case 'C': return ByteCodeConstants.T_CHAR;
		case 'F': return ByteCodeConstants.T_FLOAT;
		case 'D': return ByteCodeConstants.T_DOUBLE;
		case 'B': return ByteCodeConstants.T_BYTE;
		case 'S': return ByteCodeConstants.T_SHORT;
		case 'I': return ByteCodeConstants.T_INT;
		case 'J': return ByteCodeConstants.T_LONG;
		default:  return 0;
		}
	}
	
	public static boolean IsPrimitiveSignature(String signature) 
	{
		if ((signature == null) || (signature.length() != 1))
			return false;
		
		switch (signature.charAt(0))
		{
		case 'Z': case 'C': case 'F': case 'D':
		case 'B': case 'S': case 'I': case 'J':
			return true;
		default:
			return false;
		}
	}
	
	public static boolean IsIntegerSignature(String signature) 
	{
		if ((signature == null) || (signature.length() != 1))
			return false;
		
		switch (signature.charAt(0))
		{
		case 'C': case 'B': case 'S': case 'I':
			return true;
		default:
			return false;
		}
	}
	
	public static boolean IsObjectSignature(String signature) 
	{
		if ((signature == null) || (signature.length() <= 2))
			return false;
		
		return signature.charAt(0) == 'L';
	}
		
	public static String CutArrayDimensionPrefix(String signature) 
	{
		int beginIndex = 0;
		
		while (signature.charAt(beginIndex) == '[')
			beginIndex++;

		return signature.substring(beginIndex);
	}
	
	public static int GetArrayDimensionCount(String signature) 
	{
		int beginIndex = 0;
		
		while (signature.charAt(beginIndex) == '[')
			beginIndex++;

		return beginIndex;
	}
	
	public static String GetInnerName(String signature) 
	{
		signature = CutArrayDimensionPrefix(signature);

		if (signature.charAt(0) == 'L')
			signature = signature.substring(1, signature.length()-1);
		
		return signature;
	}
}