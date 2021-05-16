package jd.classfile.writer;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;

import jd.Constants;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.LocalVariable;
import jd.classfile.Method;
import jd.classfile.attribute.ParameterAnnotations;
import jd.exception.SignatureFormatException;
import jd.printer.Printer;
import jd.printer.SimpleFlushPrinter;
import jd.util.CharArrayUtil;
import jd.util.ReferenceMap;



public class SignatureWriter 
{
	public static void WriteClassSignature(
			Printer spw, ReferenceMap referenceMap, 
			ClassFile classFile, String signature)
	{
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;
		StringBuffer sb = new StringBuffer();
		int index = 0;
		int newIndex;
		
		spw.print(Printer.UNKNOWN_LINE_NUMBER, classFile.getClassName());
		
		// Affichage des generics
		index = WriteGenerics(
			spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, classFile, 
			caSignature, length, index);
		
		// Affichage de la classe mere
		newIndex = WriteSignature(
			sb, referenceMap, classFile, caSignature, length, index);

		if (((classFile.access_flags & 
				(Constants.ACC_INTERFACE|Constants.ACC_ENUM)) == 0) &&
			(sb.length() > 0) &&
			!IsObjectClass(caSignature, index, newIndex))
		{
			spw.startClassDeclarationExtends();
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "extends ");
			spw.print(Printer.UNKNOWN_LINE_NUMBER, sb.toString());
			spw.endClassDeclarationExtends();
		}
		
		index = newIndex;

		// Affichage des interfaces ou des super interfaces
		if (index < length)
		{
			boolean interfaceFlag =
				(classFile.access_flags & Constants.ACC_INTERFACE) != 0;
			
			if (interfaceFlag)
			{
				spw.startClassDeclarationExtends();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "extends ");
			}
			else
			{
				spw.startClassDeclarationImplements();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "implements ");
			}
			
			sb.setLength(0);
			index = WriteSignature(
				sb, referenceMap, classFile, caSignature, length, index);
			spw.print(Printer.UNKNOWN_LINE_NUMBER, sb.toString());
			
			while (index < length)
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ", ");
				sb.setLength(0);
				index = WriteSignature(
					sb, referenceMap, classFile, caSignature, length, index);
				spw.print(Printer.UNKNOWN_LINE_NUMBER, sb.toString());
			}

			if (interfaceFlag)
				spw.endClassDeclarationExtends();
			else
				spw.endClassDeclarationImplements();
		}
	}
			
	public static void WriteMethodSignature(
			HashSet<String> keywordSet, Printer spw, ReferenceMap referenceMap, 
			ClassFile classFile, Method method, 
			String signature, boolean descriptorFlag)
	{
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;
		int index = 0;
		
		// Affichage des generics
		int newIndex = WriteGenerics(
			spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, classFile, 
			caSignature, length, index);
		if (newIndex != index)
		{
			spw.print(Printer.UNKNOWN_LINE_NUMBER, ' ');
			index = newIndex;
		}
		
		if (caSignature[index] != '(')
			throw new SignatureFormatException(signature);

		// pass '('
		index++;

		ConstantPool constants = classFile.getConstantPool();
		
		// Arguments
		  // Constructeur des classes interne non static : 
		    // - var 1: outer this => ne pas generer de nom
		  // Constructeur des Enum : 
	        // Descripteur:
		        // - var 1: nom de la valeur => ne pas afficher
		        // - var 2: index de la valeur => ne pas afficher
			// Signature:
		        // - variableIndex = 1 + 1 + 1
		// Le premier parametre des méthodes non statiques est 'this'
		boolean staticMethodFlag = 
			((method.access_flags & Constants.ACC_STATIC) != 0);
		int variableIndex = staticMethodFlag ? 0 : 1;
		
		int firstVisibleParameterCounter = 0;
		
		if (method.name_index == constants.instanceConstructorIndex)
		{
			if ((classFile.access_flags & Constants.ACC_ENUM) != 0)
			{
				if (descriptorFlag)
					firstVisibleParameterCounter = 2;
				else
					variableIndex = 3;
			}
			else if (classFile.isAInnerClass())
			{
				if ((classFile.access_flags & Constants.ACC_STATIC) == 0)
					firstVisibleParameterCounter = 1;
			}
		}
		
		// Parameter annotations
		ParameterAnnotations[] invisibleParameterAnnotations = 
			method.getInvisibleParameterAnnotations();
		ParameterAnnotations[] visibleParameterAnnotations =
			method.getVisibleParameterAnnotations();
		int parameterCounter = 0;
		
		ByteArrayOutputStream argsBaos = new ByteArrayOutputStream();
		Printer argsPrinter = new SimpleFlushPrinter(new PrintWriter(argsBaos));
		StringBuffer arg = new StringBuffer();
		
		while (caSignature[index] != ')')
		{
			char firstChar = caSignature[index];
			
			arg.setLength(0);
			
			index = WriteSignature(
				arg, referenceMap, classFile, caSignature, length, index);
			String argType = arg.toString();
			
			if (parameterCounter >= firstVisibleParameterCounter)
			{
				if (parameterCounter > firstVisibleParameterCounter)
					argsPrinter.print(Printer.UNKNOWN_LINE_NUMBER, ", ");				
				
				// Affichage des annotations invisibles
				if (invisibleParameterAnnotations != null)
					AnnotationWriter.WriteParameterAnnotation(
						argsPrinter, referenceMap, classFile, 
						invisibleParameterAnnotations[parameterCounter]);
				
				// Affichage des annotations visibles
				if (visibleParameterAnnotations != null)
					AnnotationWriter.WriteParameterAnnotation(
						argsPrinter, referenceMap, classFile, 
						visibleParameterAnnotations[parameterCounter]);

				argsPrinter.print(Printer.UNKNOWN_LINE_NUMBER, argType);
				
				// TODO Test à retirer. Ce test a ete ajouter lors du codage 
				// de la gestion des enum pour eviter un NullPointerException
				if (method.getLocalVariables() != null)
				{
					LocalVariable l = method.getLocalVariables().
						searchLocalVariableWithIndexAndOffset(variableIndex, 0);
					if (l != null)
					{
						argsPrinter.print(Printer.UNKNOWN_LINE_NUMBER, " ");
						if (l.name_index == -1)
							argsPrinter.print(
									Printer.UNKNOWN_LINE_NUMBER, "???");
						else
							argsPrinter.print(
									Printer.UNKNOWN_LINE_NUMBER, 
									constants.getConstantUtf8(l.name_index));
					}
					else
					{
						argsPrinter.print(Printer.UNKNOWN_LINE_NUMBER, " arg");
						argsPrinter.print(Printer.UNKNOWN_LINE_NUMBER, variableIndex);
					}
				}
			}
			
			variableIndex += ((firstChar == 'D') || (firstChar == 'J')) ? 2 : 1;
			parameterCounter++;
		}
		
		if (method.name_index == constants.instanceConstructorIndex)
		{
			spw.print(Printer.UNKNOWN_LINE_NUMBER, classFile.getClassName());
		}
		else
		{
			// pass ')'
			index++;
			
			StringBuffer returnedType = new StringBuffer();
			WriteSignature(
				returnedType, referenceMap, classFile, 
				caSignature, length, index);	
			spw.print(Printer.UNKNOWN_LINE_NUMBER, returnedType.toString());

			spw.print(Printer.UNKNOWN_LINE_NUMBER, ' ');
			
			String methodName = constants.getConstantUtf8(method.name_index);			
			if (keywordSet.contains(methodName))
				spw.print(Printer.UNKNOWN_LINE_NUMBER, Constants.JD_METHOD_PREFIX);

			spw.print(Printer.UNKNOWN_LINE_NUMBER, methodName);
		}

		spw.print(Printer.UNKNOWN_LINE_NUMBER, '(');
		spw.print(Printer.UNKNOWN_LINE_NUMBER, argsBaos.toString());		
		spw.print(Printer.UNKNOWN_LINE_NUMBER, ')');
	}

	public static void WriteSimpleSignature(
			Printer spw, int lineNumber, ReferenceMap referenceMap, 
			ClassFile classFile, String signature)
	{
		spw.print(lineNumber, WriteSimpleSignature(
			referenceMap, classFile, signature));
	}
	
	public static String WriteSimpleSignature(
		ReferenceMap referenceMap, ClassFile classFile, String signature)
	{
		StringBuffer sb = new StringBuffer();
		char[] caSignature = signature.toCharArray();
		WriteSignature(
			sb, referenceMap, classFile, caSignature, caSignature.length, 0);
		return sb.toString();
	}
	
	private static int WriteGenerics(
			Printer spw, int lineNumber, ReferenceMap referenceMap, 
			ClassFile classFile, char[] caSignature, int length, int index)
	{
		StringBuffer sbSignature = new StringBuffer();
		int newIndex;
		
		if (caSignature[index] == '<')
		{
			spw.print(lineNumber, '<');
			index++;
			
			while (index < length)
			{
				int endIndex = CharArrayUtil.IndexOf(caSignature, ':', index);
				String templateName = 
					CharArrayUtil.Substring(caSignature, index, endIndex);
				spw.print(lineNumber, templateName);
				index = endIndex+1;
				
				// Mystere ...
				if (caSignature[index] == ':')
					index++;
					
				sbSignature.setLength(0);
				newIndex = WriteSignature(
					sbSignature, referenceMap, classFile, 
					caSignature, length, index);
				
				if (! IsObjectClass(caSignature, index, newIndex))
				{
					spw.print(lineNumber, " extends ");
					spw.print(lineNumber, sbSignature.toString());
				}
				
				index = newIndex;
				
				if (caSignature[index] == '>')
					break;
				
				spw.print(lineNumber, ", ");
			}			
			spw.print(lineNumber, '>');
			index++;
		}

		return index;
	}

	private static int WriteSignature(
		StringBuffer sbSignature, ReferenceMap referenceMap, ClassFile classFile, 
		char[] caSignature, int length, int index)
	{
		StringBuffer dimension = new StringBuffer();
		char c;
		int beginIndex;		
		
		while (true)
		{			
			// Affichage des dimensions du tableau : '[[?' ou '[L[?;'
			dimension.setLength(0);
			if (caSignature[index] == '[')
			{
				dimension.append("[]");
				
				while (++index < length)
				{
					if ((caSignature[index] == 'L') && 
						(index+1 < length) && 
						(caSignature[index+1] == '['))
					{
						index++;
						length--;
						dimension.append("[]");
					}
					else if (caSignature[index] == '[')
					{
						dimension.append("[]");
					}
					else
					{
						break;
					}
				}
			}			

			switch(caSignature[index]) 
			{
			case 'B' : 
				sbSignature.append("byte");
				index++;
				break;
			case 'C' :
				sbSignature.append("char");
				index++;
				break;
			case 'D' :
				sbSignature.append("double");
				index++;
				break;
			case 'F' : 
				sbSignature.append("float");
				index++;
				break;
			case 'I' : 
				sbSignature.append("int");
				index++;
				break;
			case 'J' : 
				sbSignature.append("long");
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
				sbSignature.append(
					InternalClassNameToClassName(
						referenceMap, classFile, internalClassName));

				if (c == '<')
				{
					sbSignature.append('<');
					index = WriteSignature(
						sbSignature, referenceMap, classFile, 
						caSignature, length, index+1);
					
					while (caSignature[index] != '>')
					{
						sbSignature.append(", ");
						index = WriteSignature(
							sbSignature, referenceMap, classFile, 
							caSignature, length, index);
					}
					sbSignature.append('>');		
					
					// pass '>'
					index++;
				}
				
				// pass ';'
				if (caSignature[index] == ';')
					index++;
				break;
			case 'S' : 
				sbSignature.append("short");
				index++;
				break;
			case 'T' :
				beginIndex = ++index;
				index = CharArrayUtil.IndexOf(caSignature, ';', beginIndex);
				sbSignature.append(caSignature, beginIndex, index-beginIndex);
				index++;
				break;
			case 'V' : 
				sbSignature.append("void");
				index++;
				break;			
			case 'Z' : 
				sbSignature.append("boolean");
				index++;
				break;
			case '-' :
				sbSignature.append("? super ");
				index = WriteSignature(
						sbSignature, referenceMap, classFile, 
						caSignature, length, index+1);
				break;
			case '+' :
				sbSignature.append("? extends ");
				index = WriteSignature(
						sbSignature, referenceMap, classFile, 
						caSignature, length, index+1);
				break;
			case '*' :
				sbSignature.append('?');
				index++;
				break;
			case 'X' :
			case 'Y' :
				sbSignature.append("int");
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
			
			sbSignature.append(dimension);
			
			if ((index >= length) || (caSignature[index] != '.'))
				break;
			
			sbSignature.append('.');
		}
		
		return index;
	}
	
	private static String InternalClassNameToClassName(
		ReferenceMap referenceMap, ClassFile classFile, String internalName)
	{
		if (classFile.getThisClassName().equals(internalName))
		{
			// La classe est la classe courante ou l'une de ses classes
			// internes
			
			// Reduction du nom de classe courante
			int index = internalName
							.lastIndexOf(Constants.INTERNAL_INNER_SEPARATOR);
			if (index >= 0)
			{
				// Reduction des noms de classe interne
				internalName = internalName.substring(index + 1);
			}
			else
			{
				index = internalName
							.lastIndexOf(Constants.INTERNAL_PACKAGE_SEPARATOR);				
				if (index >= 0)
					// Retrait du nom du package
					internalName = internalName.substring(index + 1);
			}
		}
		else
		{
			// La classe n'est pas la classe courante ou l'une de ses classes
			// internes
			int index = internalName
					.lastIndexOf(Constants.INTERNAL_PACKAGE_SEPARATOR);
	
			if (index != -1)
			{
				String internalPackageName = internalName.substring(0, index);
	
				if (classFile.getInternalPackageName().equals(internalPackageName))
				{
					// Classe appartenant au même package que la classe courante
					String innerClassPrefix = 
									classFile.getThisClassName() + 
									Constants.INTERNAL_INNER_SEPARATOR;
					if (internalName.startsWith(innerClassPrefix))
					{
						// Dans le cas d'une classe interne, on retire le nom de la 
						// classe externe
						internalName = internalName.substring(innerClassPrefix.length());
					}
					else
					{
						// Le nom est celui d'une classe appartenant au package
						// de la classe courante
						internalName = internalName.substring(index + 1);					
					}
				}
				else
				{
					if (referenceMap.contains(internalName))
					{
						// Si le nom interne fait parti de la liste des "import"
						internalName = internalName.substring(index+1);
					}
					else
					{
						// Sinon, on conserve le nom du package
						internalName = internalName.replace(
								Constants.INTERNAL_PACKAGE_SEPARATOR, 
								Constants.PACKAGE_SEPARATOR);
					}
				}			
			}
		}
		
		return internalName.replace(Constants.INTERNAL_INNER_SEPARATOR, 
									Constants.INNER_SEPARATOR);
	}
	
	private static boolean IsObjectClass(
			char[] caSignature, int beginIndex, int endIndex)
	{
		return CharArrayUtil.Substring(caSignature, beginIndex, endIndex)
					.equals(Constants.INTERNAL_OBJECT_SIGNATURE);
	}
}
