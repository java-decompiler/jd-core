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
package jd.core.process.writer;

import java.util.HashSet;

import jd.core.loader.Loader;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.util.CharArrayUtil;
import jd.core.util.SignatureFormatException;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;

public class SignatureWriter 
{
	public static void WriteTypeDeclaration(
			Loader loader, Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, String signature) 
	{
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;

		// Affichage du nom de type
		printer.printTypeDeclaration(
			classFile.getThisClassName(), classFile.getClassName());

		// Affichage des generics
		WriteGenerics(
			loader, printer, referenceMap, classFile, caSignature, length, 0);
	}

	public static int WriteConstructor(
			Loader loader, Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, String signature, String descriptor) 
	{
		char[] caSignature = signature.toCharArray();
		return WriteSignature(
			loader, printer, referenceMap, classFile,
			caSignature, caSignature.length, 0, true, descriptor, false);
	}

	public static void WriteMethodDeclaration(
			HashSet<String> keywordSet, Loader loader, Printer printer, 
			ReferenceMap referenceMap, ClassFile classFile, Method method, 
			String signature, boolean descriptorFlag) 
	{
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;
		int index = 0;

		// Affichage des generics
		int newIndex = WriteGenerics(
				loader, printer, referenceMap, classFile, 
				caSignature, length, index);
		
		if (newIndex != index) {
			printer.print(' ');
			index = newIndex;
		}

		if (caSignature[index] != '(')
			throw new SignatureFormatException(signature);

		// pass '('
		index++;

		ConstantPool constants = classFile.getConstantPool();
		String internalClassName = classFile.getThisClassName();
		String descriptor = constants.getConstantUtf8(method.descriptor_index);
		boolean staticMethodFlag = ((method.access_flags & ClassFileConstants.ACC_STATIC) != 0);

		if (method.name_index == constants.instanceConstructorIndex) 
		{
			printer.printConstructorDeclaration(internalClassName, classFile.getClassName(), descriptor);
		} 
		else 
		{
			// search ')'
			newIndex = index;

			while (newIndex < length) {
				if (caSignature[newIndex++] == ')')
					break;
			}

			WriteSignature(
				loader, printer, referenceMap, classFile, 
				caSignature, length, newIndex, false, null, false);

			printer.print(' ');

			String methodName = constants.getConstantUtf8(method.name_index);
			if (keywordSet.contains(methodName))
				methodName = StringConstants.JD_METHOD_PREFIX + methodName;

			if (staticMethodFlag)
				printer.printStaticMethodDeclaration(internalClassName, methodName, descriptor);
			else
				printer.printMethodDeclaration(internalClassName, methodName, descriptor);
		}

		// Arguments
		// Constructeur des classes interne non static :
		// - var 1: outer this => ne pas generer de nom
		// Constructeur des Enum :
		// Descripteur:
		// - var 1: nom de la valeur => ne pas afficher
		// - var 2: index de la valeur => ne pas afficher
		// Signature:
		// - variableIndex = 1 + 1 + 1
		// Le premier parametre des m�thodes non statiques est 'this'
		printer.print('(');

		int variableIndex = staticMethodFlag ? 0 : 1;
		int firstVisibleParameterIndex = 0;

		if (method.name_index == constants.instanceConstructorIndex) {
			if ((classFile.access_flags & ClassFileConstants.ACC_ENUM) != 0) {
				if (descriptorFlag)
					firstVisibleParameterIndex = 2;
				else
					variableIndex = 3;
			} else if (classFile.isAInnerClass()) {
				if ((classFile.access_flags & ClassFileConstants.ACC_STATIC) == 0)
					firstVisibleParameterIndex = 1;
			}
		}

		// Parameter annotations
		ParameterAnnotations[] invisibleParameterAnnotations = method.getInvisibleParameterAnnotations();
		ParameterAnnotations[] visibleParameterAnnotations = method.getVisibleParameterAnnotations();
		int parameterIndex = 0;
		int varargsParameterIndex;
		
		if ((method.access_flags & ClassFileConstants.ACC_VARARGS) == 0)
		{
			varargsParameterIndex = Integer.MAX_VALUE;
		}
		else
		{
			varargsParameterIndex = SignatureUtil.GetParameterSignatureCount(signature) - 1;			
		}
		
		while (caSignature[index] != ')') {
			char firstChar = caSignature[index];

			if (parameterIndex >= firstVisibleParameterIndex) {
				if (parameterIndex > firstVisibleParameterIndex)
					printer.print(", ");

				// Affichage des annotations invisibles
				if (invisibleParameterAnnotations != null)
					AnnotationWriter.WriteParameterAnnotation(
						loader, printer, referenceMap, classFile,
						invisibleParameterAnnotations[parameterIndex]);

				// Affichage des annotations visibles
				if (visibleParameterAnnotations != null)
					AnnotationWriter.WriteParameterAnnotation(
						loader, printer, referenceMap, classFile,
						visibleParameterAnnotations[parameterIndex]);

				LocalVariable lv = null;

				// TODO Test � retirer. Ce test a ete ajouter lors du codage
				// de la gestion des enum pour eviter un NullPointerException
				if (method.getLocalVariables() != null) {
					lv = method.getLocalVariables().searchLocalVariableWithIndexAndOffset(variableIndex, 0);

					if ((lv != null) && lv.finalFlag) {
						printer.printKeyword("final");
						printer.print(' ');
					}
				}

				index = WriteSignature(
					loader, printer, referenceMap, classFile, caSignature, 
					length, index, false, null,
					(parameterIndex==varargsParameterIndex));

				if (lv != null) {
					printer.print(' ');
					if (lv.name_index == -1) {
						printer.startOfError();
						printer.print("???");
						printer.endOfError();
					} else {
						printer.print(constants.getConstantUtf8(lv.name_index));
					}
				} else {
					printer.print(" arg");
					printer.print(variableIndex);
				}
			} else {
				index = SignatureUtil.SkipSignature(caSignature, length, index);
			}

			variableIndex += ((firstChar == 'D') || (firstChar == 'J')) ? 2 : 1;
			parameterIndex++;
		}

		printer.print(')');
	}

	private static int WriteGenerics(
			Loader loader, Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, char[] caSignature, int length, int index) 
	{
		if (caSignature[index] == '<') 
		{
			printer.print('<');
			index++;

			while (index < length) 
			{
				int endIndex = CharArrayUtil.IndexOf(caSignature, ':', index);
				String templateName = CharArrayUtil.Substring(caSignature, index, endIndex);
				printer.print(templateName);
				index = endIndex + 1;

				// Mystere ...
				if (caSignature[index] == ':')
					index++;

				int newIndex = SignatureUtil.SkipSignature(caSignature, length, index);

				if (!IsObjectClass(caSignature, index, newIndex)) 
				{
					printer.print(' ');
					printer.printKeyword("extends");
					printer.print(' ');
					WriteSignature(
						loader, printer, referenceMap, classFile, 
						caSignature, length, index, false, null, false);
				}

				index = newIndex;

				if (caSignature[index] == '>')
					break;

				printer.print(", ");
			}
			printer.print('>');
			index++;
		}

		return index;
	}

	public static int WriteSignature(
			Loader loader, Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, char[] caSignature, int length, int index) 
	{
		return WriteSignature(
			loader, printer, referenceMap, classFile, 
			caSignature, length, index, false, null, false);		
	}

	public static int WriteSignature(
			Loader loader, Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, String signature) 
	{
		char[] caSignature = signature.toCharArray();
		return WriteSignature(
			loader, printer, referenceMap, classFile,
			caSignature, caSignature.length, 0, false, null, false);
	}

	private static int WriteSignature(
			Loader loader, Printer printer, ReferenceMap referenceMap, 
			ClassFile classFile, char[] caSignature, int length, int index, 
			boolean constructorFlag, String constructorDescriptor,
			boolean varargsFlag) 
	{
		char c;
		int beginIndex;

		while (true) {
			// Preparation de l'affichage des dimensions du tableau : '[[?' ou
			// '[L[?;'
			int dimensionLength = 0;

			if (caSignature[index] == '[') {
				dimensionLength++;

				while (++index < length) {
					if ((caSignature[index] == 'L') && (index + 1 < length) && (caSignature[index + 1] == '[')) {
						index++;
						length--;
						dimensionLength++;
					} else if (caSignature[index] == '[') {
						dimensionLength++;
					} else {
						break;
					}
				}
			}

			switch (caSignature[index]) {
			case 'B':
				printer.printKeyword("byte");
				index++;
				break;
			case 'C':
				printer.printKeyword("char");
				index++;
				break;
			case 'D':
				printer.printKeyword("double");
				index++;
				break;
			case 'F':
				printer.printKeyword("float");
				index++;
				break;
			case 'I':
				printer.printKeyword("int");
				index++;
				break;
			case 'J':
				printer.printKeyword("long");
				index++;
				break;
			case 'L':
			case '.':
				boolean typeFlag = (caSignature[index] == 'L');
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

				if (typeFlag) 
				{
					String thisClassName = classFile.getThisClassName();
					
					if (constructorFlag)
					{
						printer.printConstructor(
							internalClassName,
							InternalClassNameToClassName(
								loader, referenceMap, classFile, internalClassName),
							constructorDescriptor,
							thisClassName);
					}
					else
					{
						printer.printType(
							internalClassName,
							InternalClassNameToClassName(
								loader, referenceMap, classFile, internalClassName),
								thisClassName);
					}
				} 
				else 
				{
					printer.print(InternalClassNameToClassName(
						loader, referenceMap, classFile, internalClassName));
				}

				if (c == '<') {
					printer.print('<');
					index = WriteSignature(
						loader, printer, referenceMap, classFile, 
						caSignature, length, index + 1, false, null, false);

					while (caSignature[index] != '>') {
						printer.print(", ");
						index = WriteSignature(
							loader, printer, referenceMap, classFile, 
							caSignature, length, index, false, null, false);
					}
					printer.print('>');

					// pass '>'
					index++;
				}

				// pass ';'
				if (caSignature[index] == ';')
					index++;
				break;
			case 'S':
				printer.printKeyword("short");
				index++;
				break;
			case 'T':
				beginIndex = ++index;
				index = CharArrayUtil.IndexOf(caSignature, ';', beginIndex);
				printer.print(new String(caSignature, beginIndex, index - beginIndex));
				index++;
				break;
			case 'V':
				printer.printKeyword("void");
				index++;
				break;
			case 'Z':
				printer.printKeyword("boolean");
				index++;
				break;
			case '-':
				printer.print("? ");
				printer.printKeyword("super");
				printer.print(' ');
				index = WriteSignature(
					loader, printer, referenceMap, classFile, 
					caSignature, length, index + 1, false, null, false);
				break;
			case '+':
				printer.print("? ");
				printer.printKeyword("extends");
				printer.print(' ');
				index = WriteSignature(
					loader, printer, referenceMap, classFile, 
					caSignature, length, index + 1, false, null, false);
				break;
			case '*':
				printer.print('?');
				index++;
				break;
			case 'X':
			case 'Y':
				printer.printKeyword("int");
				System.err.println("<UNDEFINED>");
				index++;
				break;
			default:
				// DEBUG
				new Throwable("SignatureWriter.WriteSignature: invalid signature '" + String.valueOf(caSignature) + "'")
						.printStackTrace();
				// DEBUG
			}
			
			if (varargsFlag)
			{
				if (dimensionLength > 0)
				{
					while (--dimensionLength > 0)
						printer.print("[]");					
					printer.print("...");
				}
			}
			else
			{
				while (dimensionLength-- > 0)
					printer.print("[]");
			}
			
			if ((index >= length) || (caSignature[index] != '.'))
				break;

			printer.print('.');
		}

		return index;
	}

	public static String InternalClassNameToClassName(
			Loader loader, ReferenceMap referenceMap, 
			ClassFile classFile, String internalName) 
	{
		if (classFile.getThisClassName().equals(internalName)) 
		{
			// La classe est la classe courante ou l'une de ses classes
			// internes

			// Reduction du nom de classe courante
			int index = internalName.lastIndexOf(StringConstants.INTERNAL_INNER_SEPARATOR);
			if (index >= 0) {
				// Reduction des noms de classe interne
				internalName = internalName.substring(index + 1);
			} else {
				index = internalName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
				if (index >= 0)
					// Retrait du nom du package
					internalName = internalName.substring(index + 1);
			}
		} 
		else 
		{
			// La classe n'est pas la classe courante ou l'une de ses classes
			// internes
			int index = internalName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);

			if (index != -1) 
			{
				String internalPackageName = internalName.substring(0, index);

				if (classFile.getInternalPackageName().equals(internalPackageName)) 
				{
					// Classe appartenant au m�me package que la classe courante
					if (classFile.getInnerClassFile(internalName) != null) 
					{
						// Dans le cas d'une classe interne, on retire le nom
						// de la classe externe
						internalName = internalName.substring(classFile.getThisClassName().length() + 1);
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
						internalName = internalName.substring(index + 1);
					} 
					else if ("java/lang".equals(internalPackageName)) 
					{
						// Si c'est une classe du package "java.lang"
						String internalClassName = 
							internalName.substring(index + 1);
						
						String currentPackageNamePlusInternalClassName = 
							classFile.getInternalPackageName() + 
							StringConstants.INTERNAL_PACKAGE_SEPARATOR +
							internalClassName + 
							StringConstants.CLASS_FILE_SUFFIX;
						
						if (loader.canLoad(currentPackageNamePlusInternalClassName)) {
							// Une class du package local contient une classe qui
							// porte le meme nom que la classe du package "java.lang".
							// On conserve le nom du package.
							internalName = internalName.replace(StringConstants.INTERNAL_PACKAGE_SEPARATOR,
								StringConstants.PACKAGE_SEPARATOR);
						} else {
							internalName = internalClassName;
						}						
					} else {
						// Sinon, on conserve le nom du package
						internalName = internalName.replace(StringConstants.INTERNAL_PACKAGE_SEPARATOR,
								StringConstants.PACKAGE_SEPARATOR);
					}
				}
			}
		}

		return internalName.replace(StringConstants.INTERNAL_INNER_SEPARATOR, StringConstants.INNER_SEPARATOR);
	}

	public static String InternalClassNameToShortClassName(
			ReferenceMap referenceMap, ClassFile classFile, String internalClassName) 
	{
		int index = internalClassName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);

		if (index != -1) 
		{
			String aPackageName = internalClassName.substring(0, index);

			if (classFile.getInternalPackageName().equals(aPackageName)) 
			{
				internalClassName = internalClassName.substring(index + 1);
			} 
			else 
			{
				if (referenceMap.contains(internalClassName))
					internalClassName = internalClassName.substring(index + 1);
				else
					internalClassName = internalClassName.replace(
						StringConstants.INTERNAL_PACKAGE_SEPARATOR,
						StringConstants.PACKAGE_SEPARATOR);
			}
		}

		return internalClassName.replace(
			StringConstants.INTERNAL_INNER_SEPARATOR, 
			StringConstants.INNER_SEPARATOR);
	}

	private static boolean IsObjectClass(char[] caSignature, int beginIndex, int endIndex) 
	{
		int length = StringConstants.INTERNAL_OBJECT_SIGNATURE.length();

		if (endIndex-beginIndex == length)
		{
			return CharArrayUtil.Substring(caSignature, beginIndex, endIndex).equals(
					StringConstants.INTERNAL_OBJECT_SIGNATURE);
		}
		else
		{
			return false;
		}		
	}
}
