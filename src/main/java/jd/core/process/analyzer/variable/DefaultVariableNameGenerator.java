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
package jd.core.process.analyzer.variable;

import org.jd.core.v1.util.StringConstants;

import java.util.HashSet;
import java.util.Set;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Field;
import jd.core.util.SignatureUtil;

public class DefaultVariableNameGenerator implements VariableNameGenerator
{
    private final Set<String> fieldNames;
    private final Set<String> localNames;

    public DefaultVariableNameGenerator(ClassFile classFile)
    {
        this.fieldNames = new HashSet<>();
        this.localNames = new HashSet<>();

        // Add field names
        Field[] fields = classFile.getFields();

        if (fields != null)
        {
            for (int i=0; i<fields.length; i++) {
                this.fieldNames.add(
                    classFile.getConstantPool().
                        getConstantUtf8(fields[i].getNameIndex()));
            }
        }
    }

    @Override
    public void clearLocalNames()
    {
        this.localNames.clear();
    }

    @Override
    public String generateParameterNameFromSignature(
            String signature, boolean appearsOnceFlag,
            boolean varargsFlag, int anonymousClassDepth)
    {
        String prefix = switch (anonymousClassDepth) {
        case 0 -> "param";
        case 1 -> "paramAnonymous";
        default -> "paramAnonymous" + anonymousClassDepth;
        };

        if (varargsFlag)
        {
            return prefix + "VarArgs";
        }
        int index = SignatureUtil.countDimensionOfArray(signature);
        if (index > 0) {
            prefix += "ArrayOf";
        }
        return generateValidName(
            prefix + getSuffixFromSignature(signature.substring(index)),
            appearsOnceFlag);
    }

    @Override
    public String generateLocalVariableNameFromSignature(
            String signature, boolean appearsOnce)
    {
        int index = SignatureUtil.countDimensionOfArray(signature);

        if (index > 0)
        {
            return generateValidName(
                    "arrayOf" + getSuffixFromSignature(signature.substring(index)),
                    appearsOnce);
        }
        switch (signature.charAt(0))
        {
        case 'L' :
            String s = formatSignature(signature);

            if ("String".equals(s)) {
                return generateValidName("str", appearsOnce);
            }

            return generateValidName("local" + s, appearsOnce);
        case 'B' : return generateValidName("b", appearsOnce);
        case 'C' : return generateValidName("c", appearsOnce);
        case 'D' : return generateValidName("d", appearsOnce);
        case 'F' : return generateValidName("f", appearsOnce);
        case 'I' : return generateValidIntName();
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

    private static String getSuffixFromSignature(String signature)
    {
        switch (signature.charAt(0))
        {
        case 'L' : return formatSignature(signature);
        case 'B' : return "Byte";
        case 'C' : return "Char";
        case 'D' : return "Double";
        case 'F' : return "Float";
        case 'I' : return "Int";
        case 'J' : return "Long";
        case 'S' : return "Short";
        case 'Z' : return "Boolean";
        case '[' : return "Array";
        case 'T' : return formatTemplate(signature);
        default:
            // DEBUG
            new Throwable("NameGenerator.generateParameterNameFromSignature: invalid signature '" + signature + "'").printStackTrace();
            // DEBUG
            return "?";
        }
    }

    private static String formatSignature(String signature)
    {
        // cut 'L' and ';'
        signature = signature.substring(1, signature.length()-1);

        int index = signature.indexOf(StringConstants.INTERNAL_BEGIN_TEMPLATE);
        if (index != -1) {
            signature = signature.substring(0, index);
        }

        index = signature.lastIndexOf(StringConstants.INTERNAL_INNER_SEPARATOR);
        if (index != -1) {
            signature = signature.substring(index+1);
        }

        index = signature.lastIndexOf(StringConstants.INTERNAL_PACKAGE_SEPARATOR);
        if (index != -1) {
            signature = signature.substring(index+1);
        }

        /* if (Character.isUpperCase(signature.charAt(0))) */
            return signature;

        /* return Character.toUpperCase(signature.charAt(0)) + signature.substring(1); */
    }

    private static String formatTemplate(String signature)
    {
        return signature.substring(1, signature.length()-1);
    }

    private String generateValidName(String name, boolean appearsOnceFlag)
    {
        if (Character.isUpperCase(name.charAt(0))) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        if (appearsOnceFlag && !this.fieldNames.contains(name) &&
            !this.localNames.contains(name))
        {
            this.localNames.add(name);
            return name;
        }

        String newName;
        for (int index=1; true; index++)
        {
            newName = name + index;

            if (!this.fieldNames.contains(newName) &&
                !this.localNames.contains(newName))
            {
                this.localNames.add(newName);
                return newName;
            }
        }
    }

    private String generateValidIntName()
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
