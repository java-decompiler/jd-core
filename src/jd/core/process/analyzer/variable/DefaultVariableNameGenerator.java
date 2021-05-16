package jd.core.process.analyzer.variable;

import java.util.HashSet;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Field;
import jd.core.util.StringConstants;


public class DefaultVariableNameGenerator implements VariableNameGenerator
{
	private HashSet<String> fieldNames;
	private HashSet<String> localNames;
	
	
	public DefaultVariableNameGenerator(ClassFile classFile)
	{
		this.fieldNames = new HashSet<String>();
		this.localNames = new HashSet<String>();
		
		// Add field names
		Field[] fields = classFile.getFields();
		
		if (fields != null)
		{
			for (int i=0; i<fields.length; i++)
				this.fieldNames.add(
					classFile.getConstantPool().
						getConstantUtf8(fields[i].name_index));
		}
	}
	
	public void clearLocalNames()
	{
		this.localNames.clear();
	}
	
	public String generateParameterNameFromSignature(
			String signature, boolean appearsOnceFlag, 
			boolean varargsFlag, int anonymousClassDepth)
	{
		String prefix;
		
		switch (anonymousClassDepth)
		{
		case 0:
			prefix = "param";
			break;
		case 1:
			prefix = "paramAnonymous";
			break;
		default:
			prefix = "paramAnonymous" + anonymousClassDepth;
			break;
		}
		
		if (varargsFlag)
		{
			return prefix + "VarArgs";
		}
		else 
		{
			int index = CountDimensionOfArray(signature);		

			if (index > 0)
				prefix += "ArrayOf";
			
			return generateValidName(
				prefix + GetSuffixFromSignature(signature.substring(index)), 
				appearsOnceFlag);
		}
	}
	
	public String generateLocalVariableNameFromSignature(
			String signature, boolean appearsOnce)
	{
		int index = CountDimensionOfArray(signature);

		if (index > 0)
		{
			return generateValidName(
					"arrayOf" + GetSuffixFromSignature(signature.substring(index)), 
					appearsOnce);			
		}
		else
		{
			switch (signature.charAt(0))
			{
			case 'L' : 
				String s = FormatSignature(signature);
				
				if (s.equals("String"))
					return generateValidName("str", appearsOnce);
					
				return generateValidName("local" + s, appearsOnce);
			case 'B' : return generateValidName("b", appearsOnce);
			case 'C' : return generateValidName("c", appearsOnce);
			case 'D' : return generateValidName("d", appearsOnce);
			case 'F' : return generateValidName("f", appearsOnce);
			case 'I' : return generateValidIntName(appearsOnce);
			case 'J' : return generateValidName("l", appearsOnce);
			case 'S' : return generateValidName("s", appearsOnce);
			case 'Z' : return generateValidName("bool", appearsOnce);
			default:   
				// DEBUG
				new Throwable(
						"NameGenerator.generateParameterNameFromSignature: " +
						"invalid signature '" + signature + "'")
					.printStackTrace();
				// DEBUG
				return "?";
			}
		}
	}
	
	private static int CountDimensionOfArray(String signature)
	{
		int index = 0;
		int length = signature.length();
		
		// Comptage du nombre de dimensions : '[[?' ou '[L[?;'
		if (signature.charAt(index) == '[')
		{
			while (++index < length)
			{
				if ((signature.charAt(index) == 'L') && 
					(index+1 < length) && 
					(signature.charAt(index+1) == '['))
				{
					index++;
					length--;
				}
				else if (signature.charAt(index) != '[')
				{
					break;
				}
			}
		}
		
		return index;
	}

	private static String GetSuffixFromSignature(String signature)
	{
		switch (signature.charAt(0))
		{
		case 'L' : return FormatSignature(signature);
		case 'B' : return "Byte";
		case 'C' : return "Char";
		case 'D' : return "Double";
		case 'F' : return "Float";
		case 'I' : return "Int";
		case 'J' : return "Long";
		case 'S' : return "Short";
		case 'Z' : return "Boolean";
		case '[' : return "Array";
		case 'T' : return FormatTemplate(signature);
		default:   
			// DEBUG
			new Throwable("NameGenerator.generateParameterNameFromSignature: invalid signature '" + signature + "'").printStackTrace();
			// DEBUG
			return "?";
		}
	}

	private static String FormatSignature(String signature)
	{
		// cut 'L' and ';'
		signature = signature.substring(1, signature.length()-1);
		
		int index = signature.indexOf(StringConstants.INTERNAL_BEGIN_TEMPLATE);
		if (index != -1)
			signature = signature.substring(0, index);

		index = signature.lastIndexOf(StringConstants.INTERNAL_INNER_SEPARATOR);
		if (index != -1)
			signature = signature.substring(index+1);
		
		index = signature.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
		if (index != -1)
			signature = signature.substring(index+1);
		
		/* if (Character.isUpperCase(signature.charAt(0))) */
			return signature;
		
		/* return Character.toUpperCase(signature.charAt(0)) + signature.substring(1); */
	}

	private static String FormatTemplate(String signature)
	{
		return signature.substring(1, signature.length()-1);
	}
	
	private String generateValidName(String name, boolean appearsOnceFlag)
	{
		if (Character.isUpperCase(name.charAt(0)))
			name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		
		if (appearsOnceFlag)
			if (!this.fieldNames.contains(name) && 
				!this.localNames.contains(name))
			{
				this.localNames.add(name);
				return name;
			}

		for (int index=1; true; index++)
		{
			String newName = name + index;
			
			if (!this.fieldNames.contains(newName) && 
				!this.localNames.contains(newName))
			{
				this.localNames.add(newName);
				return newName;
			}
		}
	}

	private String generateValidIntName(boolean appearsOnce)
	{
		if (!this.fieldNames.contains("i") && !this.localNames.contains("i"))
		{
			this.localNames.add("i");
			return "i";
		}
		
		if (!this.fieldNames.contains("j") && !this.localNames.contains("j"))
		{
			this.localNames.add("j");
			return "j";
		}
		
		if (!this.fieldNames.contains("k") && !this.localNames.contains("k"))
		{
			this.localNames.add("k");
			return "k";
		}
		
		if (!this.fieldNames.contains("m") && !this.localNames.contains("m"))
		{
			this.localNames.add("m");
			return "m";
		}
		
		if (!this.fieldNames.contains("n") && !this.localNames.contains("n"))
		{
			this.localNames.add("n");
			return "n";
		}
		
		return generateValidName("i", false);
	}	
}
