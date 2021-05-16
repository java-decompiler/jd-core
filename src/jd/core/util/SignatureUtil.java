package jd.core.util;

import java.util.ArrayList;

import jd.core.model.instruction.bytecode.ByteCodeConstants;


public class SignatureUtil 
{
	/**
	 * @see SignatureAnalyzer.SignatureAnalyzer(...)
	 */
	public static int SkipSignature(char[] caSignature, int length, int index)
	{
		char c;

		while (true)
		{			
			// Affichage des dimensions du tableau : '[[?' ou '[L[?;'
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
			case 'B' : case 'C' : case 'D' : case 'F' : 
			case 'I' : case 'J' : case 'S' : case 'V' : 
			case 'Z' : case '*' : case 'X' : case 'Y' :
				index++;
				break;
			case 'L' : case '.' : 
				index++;
				c = '.';
				
				// Recherche de ; ou de <
				while (index < length)
				{
					c = caSignature[index];
					if ((c == ';') || (c == '<'))
						break;
					index++;
				}
				
				if (c == '<')
				{
					index = SkipSignature(caSignature, length, index+1);
					
					while (caSignature[index] != '>')
					{
						index = SkipSignature(caSignature, length, index);
					}
					
					// pass '>'
					index++;
				}
				
				// pass ';'
				if (caSignature[index] == ';')
					index++;
				break;
			case 'T' :
				index = CharArrayUtil.IndexOf(caSignature, ';', index+1) + 1;
				break;
			case '-' : case '+' :
				index = SkipSignature(caSignature, length, index+1);
				break;
			//default:
				// DEBUG
				//new Throwable(
				//	"SignatureWriter.WriteSignature: invalid signature '" + 
				//	String.valueOf(caSignature) + "'").printStackTrace();
				// DEBUG
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
		
	public static String GetInternalName(String signature) 
	{
		char[] caSignature = signature.toCharArray();
		int length = signature.length();
		int beginIndex = 0;

		while ((beginIndex < length) && (caSignature[beginIndex] == '['))
			beginIndex++;

		if ((beginIndex < length) && (caSignature[beginIndex] == 'L'))
		{
			beginIndex++;
			length--;
			return CharArrayUtil.Substring(caSignature, beginIndex, length);
		}
		else
		{
			return (beginIndex == 0) ? signature : 
				CharArrayUtil.Substring(caSignature, beginIndex, length);
		}
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

		switch (signature.charAt(0))
		{
		case 'L':
		case 'T':
			return signature.substring(1, signature.length()-1);
		default:
			return signature;
		}
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
				int newIndex = SkipSignature(caSignature, length, index);
				parameterTypes.add(methodSignature.substring(index, newIndex));
				index = newIndex;
			}
		}
		
		return parameterTypes;
	}

	public static String GetMethodReturnedSignature(String signature)
	{
		int index = signature.indexOf(')');
		if (index == -1)
			return null;
		
		return signature.substring(index + 1);
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
				int newIndex = SkipSignature(caSignature, length, index);
				index = newIndex;
				count++;
			}
		}
		
		return count;
	}

	public static int CreateTypesBitField(String signature)
	{
		/*
		 * Pour une constante de type 'signature', les types de variable 
		 * possible est retournée. 
		 */
		switch (signature.charAt(0))
		{
		case 'I': return ByteCodeConstants.TBF_INT_INT;
		case 'S': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT;
		case 'B': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_BYTE;
		case 'C': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_CHAR;
		case 'X': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_BYTE | ByteCodeConstants.TBF_INT_CHAR | ByteCodeConstants.TBF_INT_BOOLEAN;
		case 'Y': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_BYTE | ByteCodeConstants.TBF_INT_CHAR;
		case 'Z': return ByteCodeConstants.TBF_INT_BOOLEAN;
		default:  return 0;
		}
	}	
	
	public static int CreateArgOrReturnBitFields(String signature)
	{
		/*
		 * Pour un argument de type 'signature', les types de variable possible 
		 * est retournée. 
		 */
		switch (signature.charAt(0))
		{
		case 'I': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_BYTE | ByteCodeConstants.TBF_INT_CHAR;
		case 'S': return ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_BYTE;
		case 'B': return ByteCodeConstants.TBF_INT_BYTE;
		case 'C': return ByteCodeConstants.TBF_INT_CHAR;
		case 'X': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_BYTE | ByteCodeConstants.TBF_INT_CHAR | ByteCodeConstants.TBF_INT_BOOLEAN;
		case 'Y': return ByteCodeConstants.TBF_INT_INT | ByteCodeConstants.TBF_INT_SHORT | ByteCodeConstants.TBF_INT_BYTE | ByteCodeConstants.TBF_INT_CHAR;
		case 'Z': return ByteCodeConstants.TBF_INT_BOOLEAN;
		default:  return 0;
		}
	}

	public static String GetSignatureFromTypesBitField(int typesBitField)
	{
		/* 
		 * Lorsqu'un choix est possible, le plus 'gros' type est retourné. 
		 */
		if ((typesBitField & ByteCodeConstants.TBF_INT_INT) != 0)
			return "I";
		if ((typesBitField & ByteCodeConstants.TBF_INT_SHORT) != 0)
			return "S";
		if ((typesBitField & ByteCodeConstants.TBF_INT_CHAR) != 0)
			return "C";
		if ((typesBitField & ByteCodeConstants.TBF_INT_BYTE) != 0)
			return "B";
		if ((typesBitField & ByteCodeConstants.TBF_INT_BOOLEAN) != 0)
			return "Z";
		return "I";		
	}
	
	public static String CreateTypeName(String signature) 
	{
		if (signature.length() == 0)
			return signature;

		switch(signature.charAt(0))
		{
		case '[':
			return signature;
		case 'L':
		case 'T':
			if (signature.charAt(signature.length()-1) == ';')
				return signature;
		default:
			return "L" + signature + ';';
		}
	}	
}
