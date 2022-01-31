/**
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
 */
package jd.core.process.writer;

import org.apache.bcel.Const;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.SignatureFormatException;
import org.jd.core.v1.util.StringConstants;

import java.util.Set;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.Printer;
import jd.core.util.CharArrayUtil;
import jd.core.util.SignatureUtil;

public final class SignatureWriter
{
    private SignatureWriter() {
    }
        public static void writeTypeDeclaration(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, String signature)
    {
        char[] caSignature = signature.toCharArray();
        int length = caSignature.length;

        // Affichage du nom de type
        printer.printTypeDeclaration(
            classFile.getThisClassName(), classFile.getClassName());

        // Affichage des generics
        writeGenerics(
            loader, printer, referenceMap, classFile, caSignature, length, 0);
    }

    public static int writeConstructor(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, String signature, String descriptor)
    {
        char[] caSignature = signature.toCharArray();
        return writeSignature(
            loader, printer, referenceMap, classFile,
            caSignature, caSignature.length, 0, true, descriptor, false);
    }

    public static void writeMethodDeclaration(
            Set<String> keywordSet, Loader loader, Printer printer,
            ReferenceMap referenceMap, ClassFile classFile, Method method,
            String signature, boolean descriptorFlag, boolean lambda)
    {
        char[] caSignature = signature.toCharArray();
        int length = caSignature.length;
        ConstantPool constants = classFile.getConstantPool();
        boolean staticMethodFlag = (method.getAccessFlags() & Const.ACC_STATIC) != 0;
        int index = 0;
        if (!lambda) {
            // Affichage des generics
            int newIndex = writeGenerics(
                    loader, printer, referenceMap, classFile,
                    caSignature, length, index);
    
            if (newIndex != index) {
                printer.print(' ');
                index = newIndex;
            }
    
            if (caSignature[index] != '(') {
                throw new SignatureFormatException(signature);
            }
    
            // pass '('
            index++;
    
            String internalClassName = classFile.getThisClassName();
            String descriptor = constants.getConstantUtf8(method.getDescriptorIndex());
    
            if (method.getNameIndex() == constants.getInstanceConstructorIndex())
            {
                printer.printConstructorDeclaration(internalClassName, classFile.getClassName(), descriptor);
            }
            else
            {
    
                newIndex = index;
    
                while (newIndex < length && caSignature[newIndex++] != ')') {
                    // search ')'
                }
    
                writeSignature(
                    loader, printer, referenceMap, classFile,
                    caSignature, length, newIndex, false, null, false);
    
                printer.print(' ');
    
                String methodName = constants.getConstantUtf8(method.getNameIndex());
                if (keywordSet.contains(methodName)) {
                    methodName = StringConstants.JD_METHOD_PREFIX + methodName;
                }
    
                if (staticMethodFlag) {
                    printer.printStaticMethodDeclaration(internalClassName, methodName, descriptor);
                } else {
                    printer.printMethodDeclaration(internalClassName, methodName, descriptor);
                }
            }
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
        // Le premier parametre des méthodes non statiques est 'this'
        printer.print('(');

        int variableIndex = staticMethodFlag ? 0 : 1;
        int firstVisibleParameterIndex = 0;

        if (method.getNameIndex() == constants.getInstanceConstructorIndex()) {
            if ((classFile.getAccessFlags() & Const.ACC_ENUM) != 0) {
                if (descriptorFlag) {
                    firstVisibleParameterIndex = 2;
                } else {
                    variableIndex = 3;
                }
            } else if (classFile.isAInnerClass() && (classFile.getAccessFlags() & Const.ACC_STATIC) == 0) {
                firstVisibleParameterIndex = 1;
            }
        }

        // Parameter annotations
        ParameterAnnotations[] invisibleParameterAnnotations = method.getInvisibleParameterAnnotations();
        ParameterAnnotations[] visibleParameterAnnotations = method.getVisibleParameterAnnotations();
        int parameterIndex = 0;
        int varargsParameterIndex;

        if ((method.getAccessFlags() & Const.ACC_VARARGS) == 0)
        {
            varargsParameterIndex = Integer.MAX_VALUE;
        }
        else
        {
            varargsParameterIndex = SignatureUtil.getParameterSignatureCount(signature) - 1;
        }

        char firstChar;
        while (caSignature[index] != ')') {
            firstChar = caSignature[index];

            if (parameterIndex >= firstVisibleParameterIndex) {
                if (parameterIndex > firstVisibleParameterIndex) {
                    printer.print(", ");
                }

                // Affichage des annotations invisibles
                if (invisibleParameterAnnotations != null) {
                    AnnotationWriter.writeParameterAnnotation(
                        loader, printer, referenceMap, classFile,
                        invisibleParameterAnnotations[parameterIndex]);
                }

                // Affichage des annotations visibles
                if (visibleParameterAnnotations != null) {
                    AnnotationWriter.writeParameterAnnotation(
                        loader, printer, referenceMap, classFile,
                        visibleParameterAnnotations[parameterIndex]);
                }

                LocalVariable lv = null;

                // TODO Test à  retirer. Ce test a été ajouté lors du codage
                // de la gestion des enum pour éviter un NullPointerException
                if (method.getLocalVariables() != null) {
                    lv = method.getLocalVariables().searchLocalVariableWithIndexAndOffset(variableIndex, 0);

                    if (lv != null && lv.hasFinalFlag()) {
                        printer.printKeyword("final");
                        printer.print(' ');
                    }
                }

                index = writeSignature(
                    loader, printer, referenceMap, classFile, caSignature,
                    length, index, false, null,
                    parameterIndex==varargsParameterIndex);

                if (lv != null) {
                    printer.print(' ');
                    if (lv.getNameIndex() == -1) {
                        printer.startOfError();
                        printer.print("???");
                        printer.endOfError();
                    } else {
                        printer.print(constants.getConstantUtf8(lv.getNameIndex()));
                    }
                } else {
                    printer.print(" arg");
                    printer.print(variableIndex);
                }
            } else {
                index = SignatureUtil.skipSignature(caSignature, length, index);
            }

            variableIndex += firstChar == 'D' || firstChar == 'J' ? 2 : 1;
            parameterIndex++;
        }

        printer.print(')');
    }

    private static int writeGenerics(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, char[] caSignature, int length, int index)
    {
        if (caSignature[index] == '<')
        {
            printer.print('<');
            index++;

            int endIndex;
            String templateName;
            int newIndex;
            while (index < length)
            {
                endIndex = CharArrayUtil.indexOf(caSignature, ':', index);
                templateName = CharArrayUtil.substring(caSignature, index, endIndex);
                printer.print(templateName);
                index = endIndex + 1;

                // Mystere ...
                if (caSignature[index] == ':') {
                    index++;
                }

                newIndex = SignatureUtil.skipSignature(caSignature, length, index);

                if (!isObjectClass(caSignature, index, newIndex))
                {
                    printer.print(' ');
                    printer.printKeyword("extends");
                    printer.print(' ');
                    writeSignature(
                        loader, printer, referenceMap, classFile,
                        caSignature, length, index, false, null, false);
                }

                index = newIndex;

                if (caSignature[index] == '>') {
                    break;
                }

                printer.print(", ");
            }
            printer.print('>');
            index++;
        }

        return index;
    }

    public static int writeSignature(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, char[] caSignature, int length, int index)
    {
        return writeSignature(
            loader, printer, referenceMap, classFile,
            caSignature, length, index, false, null, false);
    }

    public static int writeSignature(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, String signature)
    {
        char[] caSignature = signature.toCharArray();
        return writeSignature(
            loader, printer, referenceMap, classFile,
            caSignature, caSignature.length, 0, false, null, false);
    }

    private static int writeSignature(
            Loader loader, Printer printer, ReferenceMap referenceMap,
            ClassFile classFile, char[] caSignature, int length, int index,
            boolean constructorFlag, String constructorDescriptor,
            boolean varargsFlag)
    {
        char c;
        int beginIndex;

        int dimensionLength;
        do {
            // Preparation de l'affichage des dimensions du tableau : '[[?' ou
            // '[L[?;'
            dimensionLength = 0;

            if (caSignature[index] == '[') {
                dimensionLength++;

                while (++index < length) {
                    if (caSignature[index] == 'L' && index + 1 < length && caSignature[index + 1] == '[') {
                        index++;
                        length--;
                    } else if (caSignature[index] != '[') {
                        break;
                    }
                    dimensionLength++;
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
            case 'L', '.':
                boolean typeFlag = caSignature[index] == 'L';
                index++;
                beginIndex = index;
                c = '.';

                // Recherche de ; ou de <
                while (index < length)
                {
                    c = caSignature[index];
                    if (c == ';' || c == '<') {
                        break;
                    }
                    index++;
                }

                String internalClassName =
                    CharArrayUtil.substring(caSignature, beginIndex, index);

                if (typeFlag)
                {
                    String thisClassName = classFile.getThisClassName();

                    if (constructorFlag)
                    {
                        printer.printConstructor(
                            internalClassName,
                            internalClassNameToClassName(
                                loader, referenceMap, classFile, internalClassName),
                            constructorDescriptor,
                            thisClassName);
                    }
                    else
                    {
                        printer.printType(
                            internalClassName,
                            internalClassNameToClassName(
                                loader, referenceMap, classFile, internalClassName),
                                thisClassName);
                    }
                }
                else
                {
                    printer.print(internalClassNameToClassName(
                        loader, referenceMap, classFile, internalClassName));
                }

                if (c == '<') {
                    printer.print('<');
                    index = writeSignature(
                        loader, printer, referenceMap, classFile,
                        caSignature, length, index + 1, false, null, false);

                    while (caSignature[index] != '>') {
                        printer.print(", ");
                        index = writeSignature(
                            loader, printer, referenceMap, classFile,
                            caSignature, length, index, false, null, false);
                    }
                    printer.print('>');

                    // pass '>'
                    index++;
                }

                // pass ';'
                if (caSignature[index] == ';') {
                    index++;
                }
                break;
            case 'S':
                printer.printKeyword("short");
                index++;
                break;
            case 'T':
                index++;
                beginIndex = index;
                index = CharArrayUtil.indexOf(caSignature, ';', beginIndex);
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
                index = writeSignature(
                    loader, printer, referenceMap, classFile,
                    caSignature, length, index + 1, false, null, false);
                break;
            case '+':
                printer.print("? ");
                printer.printKeyword("extends");
                printer.print(' ');
                index = writeSignature(
                    loader, printer, referenceMap, classFile,
                    caSignature, length, index + 1, false, null, false);
                break;
            case '*':
                printer.print('?');
                index++;
                break;
            case 'X', 'Y':
                printer.printKeyword("int");
                System.err.println("<UNDEFINED>");
                index++;
                break;
            default:
                // DEBUG
                new Throwable("SignatureWriter.writeSignature: invalid signature '" + String.valueOf(caSignature) + "'")
                        .printStackTrace();
                // DEBUG
            }

            if (varargsFlag)
            {
                if (dimensionLength > 0)
                {
                    while (--dimensionLength > 0) {
                        printer.print("[]");
                    }
                    printer.print("...");
                }
            }
            else
            {
                while (dimensionLength-- > 0) {
                    printer.print("[]");
                }
            }

            if (index >= length || caSignature[index] != '.') {
                break;
            }

            printer.print('.');
        } while (true);

        return index;
    }

    public static String internalClassNameToClassName(
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
                if (index >= 0) {
                    // Retrait du nom du package
                    internalName = internalName.substring(index + 1);
                }
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
                    // Classe appartenant au même package que la classe courante
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
                } else if (referenceMap.contains(internalName))
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
                        // porte le même nom que la classe du package "java.lang".
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

        return internalName.replace(StringConstants.INTERNAL_INNER_SEPARATOR, StringConstants.INNER_SEPARATOR);
    }

    public static String internalClassNameToShortClassName(
            ReferenceMap referenceMap, ClassFile classFile, String internalClassName)
    {
        int index = internalClassName.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);

        if (index != -1)
        {
            String aPackageName = internalClassName.substring(0, index);

            if (classFile.getInternalPackageName().equals(aPackageName) || referenceMap.contains(internalClassName))
            {
                internalClassName = internalClassName.substring(index + 1);
            } else {
                internalClassName = internalClassName.replace(
                    StringConstants.INTERNAL_PACKAGE_SEPARATOR,
                    StringConstants.PACKAGE_SEPARATOR);
            }
        }

        return internalClassName.replace(
            StringConstants.INTERNAL_INNER_SEPARATOR,
            StringConstants.INNER_SEPARATOR);
    }

    private static boolean isObjectClass(char[] caSignature, int beginIndex, int endIndex)
    {
        int length = StringConstants.INTERNAL_OBJECT_SIGNATURE.length();

        return endIndex-beginIndex == length && StringConstants.INTERNAL_OBJECT_SIGNATURE.equals(
                CharArrayUtil.substring(caSignature, beginIndex, endIndex));
    }
}
