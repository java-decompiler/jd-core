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
package jd.core.process.analyzer.classfile;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastLabel;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.reconstructor.AssignmentInstructionReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.DotClass118AReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.DotClass14Reconstructor;
import jd.core.process.analyzer.classfile.reconstructor.DupStoreThisReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.InitDexEnumFieldsReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.InitInstanceFieldsReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.InitStaticFieldsReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.NewInstructionReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.OuterReferenceReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.PostIncReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.PreIncReconstructor;
import jd.core.process.analyzer.classfile.reconstructor.SimpleNewInstructionReconstructor;
import jd.core.process.analyzer.classfile.visitor.CheckCastAndConvertInstructionVisitor;
import jd.core.process.analyzer.classfile.visitor.ReplaceStringBuxxxerVisitor;
import jd.core.process.analyzer.classfile.visitor.SetConstantTypeInStringIndexOfMethodsVisitor;
import jd.core.process.analyzer.instruction.bytecode.InstructionListBuilder;
import jd.core.process.analyzer.instruction.fast.DupLocalVariableAnalyzer;
import jd.core.process.analyzer.instruction.fast.FastInstructionListBuilder;
import jd.core.process.analyzer.instruction.fast.ReturnLineNumberAnalyzer;
import jd.core.process.analyzer.variable.DefaultVariableNameGenerator;
import jd.core.util.SignatureUtil;

public final class ClassFileAnalyzer
{
    private ClassFileAnalyzer() {
    }

    public static void analyze(ReferenceMap referenceMap, ClassFile classFile)
    {
        // Creation du tableau associatif [nom de classe interne, objet class].
        // Ce tableau est utilisé pour la suppression des accesseurs des
        // classes internes.
        Map<String, ClassFile> innerClassesMap;
        if (classFile.getInnerClassFiles() != null)
        {
            innerClassesMap = new HashMap<>(10);
            innerClassesMap.put(classFile.getThisClassName(), classFile);
            populateInnerClassMap(innerClassesMap, classFile);
        }
        else
        {
            innerClassesMap = null;
        }

        // Generation des listes d'instructions
        // Creation du tableau des variables locales si necessaire
        analyzeClass(referenceMap, innerClassesMap, classFile);
    }

    private static void populateInnerClassMap(
            Map<String, ClassFile> innerClassesMap, ClassFile classFile)
    {
        List<ClassFile> innerClassFiles = classFile.getInnerClassFiles();

        if (innerClassFiles != null)
        {
            int length = innerClassFiles.size();

            ClassFile innerClassFile;
            for (int i=0; i<length; ++i)
            {
                innerClassFile = innerClassFiles.get(i);
                innerClassesMap.put(
                        innerClassFile.getThisClassName(), innerClassFile);
                populateInnerClassMap(innerClassesMap, innerClassFile);
            }
        }
    }

    private static void analyzeClass(
            ReferenceMap referenceMap,
            Map<String, ClassFile> innerClassesMap,
            ClassFile classFile)
    {
        if ((classFile.getAccessFlags() & Const.ACC_SYNTHETIC) != 0)
        {
            analyzeSyntheticClass(classFile);
        }
        else
        {
            // L'analyse preliminaire permet d'identifier l'attribut de chaque
            // classe interne non statique portant la reference vers la classe
            // externe. 'PreAnalyzeMethods' doit être execute avant l'analyse
            // des classes internes. Elle permet egalement de construire la liste
            // des accesseurs et de parser les tableaux "SwitchMap" produit par le
            // compilateur d'Eclipse et utilisé pour le Switch+Enum.
            preAnalyzeMethods(classFile);

            // Analyse des classes internes avant l'analyse de la classe pour
            // afficher correctement des classes anonymes.
            List<ClassFile> innerClassFiles = classFile.getInnerClassFiles();
            if (innerClassFiles != null)
            {
                int length = innerClassFiles.size();
                for (int i=0; i<length; i++) {
                    analyzeClass(referenceMap, innerClassesMap, innerClassFiles.get(i));
                }
            }

            // Analyse de la classe
            checkUnicityOfFieldNames(classFile);
            checkUnicityOfFieldrefNames(classFile);
            analyzeMethods(referenceMap, innerClassesMap, classFile);
            checkAssertionsDisabledField(classFile);

            if ((classFile.getAccessFlags() & Const.ACC_ENUM) != 0) {
                analyzeEnum(classFile);
            }
        }
    }

    private static void analyzeSyntheticClass(ClassFile classFile)
    {
        // Recherche des classes internes utilisees par les instructions
        // Switch+Enum generees par les compilateurs autre qu'Eclipse.

        if ((classFile.getAccessFlags() & Const.ACC_STATIC) != 0 &&
                classFile.getOuterClass() != null &&
                classFile.getInternalAnonymousClassName() != null &&
                classFile.getFields() != null &&
                classFile.getMethods() != null &&
                classFile.getFields().length > 0 &&
                classFile.getMethods().length == 1 &&
                (classFile.getMethod(0).getAccessFlags() &
                        (Const.ACC_PUBLIC|Const.ACC_PROTECTED|Const.ACC_PRIVATE|Const.ACC_STATIC|Const.ACC_FINAL|Const.ACC_SYNTHETIC)) ==
                        Const.ACC_STATIC)
        {
            ClassFile outerClassFile = classFile.getOuterClass();
            ConstantPool outerConstants = outerClassFile.getConstantPool();
            ConstantPool constants = classFile.getConstantPool();
            Method method = classFile.getMethod(0);

            try
            {
                analyzeMethodref(classFile);

                // Build instructions
                List<Instruction> list = new ArrayList<>();
                List<Instruction> listForAnalyze = new ArrayList<>();

                InstructionListBuilder.build(
                        classFile, method, list, listForAnalyze);

                /* Parse static method
                 * static {
                 *  $SwitchMap$basic$data$TestEnum$enum2 = new int[enum2.values().length];
                 *  try { $SwitchMap$basic$data$TestEnum$enum2[enum2.E.ordinal()] = 1; } catch(NoSuchFieldError ex) { }
                 *  try { $SwitchMap$basic$data$TestEnum$enum2[enum2.F.ordinal()] = 2; } catch(NoSuchFieldError ex) { }
                 *  $SwitchMap$basic$data$TestEnum$enum1 = new int[enum1.values().length];
                 *  try { $SwitchMap$basic$data$TestEnum$enum1[enum1.A.ordinal()] = 1; } catch(NoSuchFieldError ex) { }
                 *  try { $SwitchMap$basic$data$TestEnum$enum1[enum1.B.ordinal()] = 2; } catch(NoSuchFieldError ex) { }
                 * }
                 */
                int length = list.size();

                PutStatic ps;
                ConstantFieldref cfr;
                ConstantNameAndType cnat;
                Field field;
                String fieldName;
                List<Integer> enumNameIndexes;
                int outerFieldNameIndex;
                Instruction instruction;
                String enumName;
                int outerEnumNameIndex;
                for (int index=0; index<length; index++)
                {
                    if (list.get(index).getOpcode() != Const.PUTSTATIC) {
                        break;
                    }

                    ps = (PutStatic)list.get(index);
                    cfr = constants.getConstantFieldref(ps.getIndex());
                    if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                        break;
                    }

                    cnat = constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

                    // Search field
                    field = searchField(classFile, cnat);
                    if (field == null || (field.getAccessFlags() &
                            (Const.ACC_PUBLIC|Const.ACC_PROTECTED|Const.ACC_PRIVATE|Const.ACC_STATIC|Const.ACC_FINAL|Const.ACC_SYNTHETIC)) !=
                            (Const.ACC_STATIC|Const.ACC_SYNTHETIC|Const.ACC_FINAL)) {
                        break;
                    }

                    fieldName = constants.getConstantUtf8(cnat.getNameIndex());
                    if (! fieldName.startsWith("$SwitchMap$")) {
                        break;
                    }

                    enumNameIndexes = new ArrayList<>();

                    for (index+=3; index<length; index+=3)
                    {
                        instruction = list.get(index-2);

                        if (instruction.getOpcode() != ByteCodeConstants.ARRAYSTORE ||
                                list.get(index-1).getOpcode() != Const.GOTO ||
                                list.get(index).getOpcode() != Const.ASTORE) {
                            break;
                        }

                        instruction = ((ArrayStoreInstruction)instruction).getIndexref();

                        if (instruction.getOpcode() != Const.INVOKEVIRTUAL) {
                            break;
                        }

                        instruction = ((Invokevirtual)instruction).getObjectref();

                        if (instruction.getOpcode() != Const.GETSTATIC) {
                            break;
                        }

                        cfr = constants.getConstantFieldref(
                                ((GetStatic)instruction).getIndex());
                        cnat = constants.getConstantNameAndType(
                                cfr.getNameAndTypeIndex());
                        enumName = constants.getConstantUtf8(cnat.getNameIndex());
                        outerEnumNameIndex = outerConstants.addConstantUtf8(enumName);

                        // Add enum name index
                        enumNameIndexes.add(outerEnumNameIndex);
                    }

                    outerFieldNameIndex = outerConstants.addConstantUtf8(fieldName);

                    // Key = indexe du nom de na classe interne dans le
                    // pool de constantes de la classe externe
                    outerClassFile.getSwitchMaps().put(
                            outerFieldNameIndex, enumNameIndexes);

                    index -= 3;
                }
            }
            catch (Exception e)
            {
                assert ExceptionUtil.printStackTrace(e);
                method.setContainsError(true);
            }
        }
    }

    private static Field searchField(
            ClassFile classFile, ConstantNameAndType cnat)
    {
        Field[] fields = classFile.getFields();
        int i = fields.length;

        Field field;
        while (i-- > 0)
        {
            field = fields[i];

            if (field.getNameIndex() == cnat.getNameIndex() &&
                    field.getDescriptorIndex() == cnat.getSignatureIndex()) {
                return field;
            }
        }

        return null;
    }

    private static void analyzeMethodref(ClassFile classFile)
    {
        ConstantPool constants = classFile.getConstantPool();

        Constant constant;
        for (int i=constants.size()-1; i>=0; --i)
        {
            constant = constants.get(i);

            if (constant instanceof ConstantMethodref)
            {
                // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                ConstantMethodref cmr = (ConstantMethodref) constant;
                ConstantNameAndType cnat =
                        constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                if (cnat != null)
                {
                    String signature = constants.getConstantUtf8(
                            cnat.getSignatureIndex());
                    cmr.setParameterSignatures(
                            SignatureUtil.getParameterSignatures(signature));
                    cmr.setReturnedSignature(
                            SignatureUtil.getMethodReturnedSignature(signature));
                }
            }
        }
    }

    private static void checkUnicityOfFieldNames(ClassFile classFile)
    {
        Field[] fields = classFile.getFields();
        if (fields == null) {
            return;
        }

        ConstantPool constants = classFile.getConstantPool();
        Map<String, List<Field>> map =
                new HashMap<>();

        // Populate map
        int i = fields.length;
        while (i-- > 0)
        {
            Field field = fields[i];

            if ((field.getAccessFlags() & (Const.ACC_PUBLIC|Const.ACC_PROTECTED)) != 0) {
                continue;
            }

            String name = constants.getConstantUtf8(field.getNameIndex());

            map.computeIfAbsent(name, k -> new ArrayList<>(5)).add(field);
        }

        // Check unicity
        Iterator<String> iteratorName = map.keySet().iterator();
        String name;
        List<Field> list;
        int j;
        Field field;
        String newName;
        int newNameIndex;
        while (iteratorName.hasNext())
        {
            name = iteratorName.next();
            list = map.get(name);

            j = list.size();
            if (j < 2) {
                continue;
            }

            // Change attribute names;
            while (j-- > 0)
            {
                field = list.get(j);

                // Generate new attribute names
                newName = FieldNameGenerator.generateName(
                        constants.getConstantUtf8(field.getDescriptorIndex()),
                        constants.getConstantUtf8(field.getNameIndex()));
                // Add new constant string
                newNameIndex = constants.addConstantUtf8(newName);
                // Update name index
                field.setNameIndex(newNameIndex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkUnicityOfFieldrefNames(ClassFile classFile)
    {
        ConstantPool constants = classFile.getConstantPool();

        // Popuplate array
        int i = constants.size();
        Object[] array = new Object[i];

        Constant constant;
        ConstantFieldref cfr;
        while (i-- > 0)
        {
            constant = constants.get(i);

            if (!(constant instanceof ConstantFieldref)) {
                continue;
            }

            cfr = (ConstantFieldref)constant;
            Map<String, List<ConstantNameAndType>> map =
                    (Map<String, List<ConstantNameAndType>>)array[cfr.getClassIndex()];

            if (map == null)
            {
                map = new HashMap<>();
                array[cfr.getClassIndex()] = map;
            }

            ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
            String name = constants.getConstantUtf8(cnat.getNameIndex());
            List<ConstantNameAndType> list = map.get(name);

            if (list != null)
            {
                if (list.get(0).getSignatureIndex() != cnat.getSignatureIndex())
                {
                    // Same name and different signature
                    list.add(cnat);
                }
            }
            else
            {
                list = new ArrayList<>(5);
                map.put(name, list);
                list.add(cnat);
            }
        }

        // For each class in constant pool, check unicity of name of 'Fieldref'
        i = array.length;
        Map<String, List<ConstantNameAndType>> map;
        Iterator<String> iterator;
        String name;
        List<ConstantNameAndType> list;
        int k;
        ConstantNameAndType cnat;
        String signature;
        String newName;
        while (i-- > 0)
        {
            if (array[i] == null) {
                continue;
            }

            map = (Map<String, List<ConstantNameAndType>>)array[i];

            iterator = map.keySet().iterator();
            while (iterator.hasNext())
            {
                name = iterator.next();
                list = map.get(name);

                k = list.size();
                if (k < 2) {
                    continue;
                }

                while (k-- > 0)
                {
                    cnat = list.get(k);
                    signature = constants.getConstantUtf8(cnat.getSignatureIndex());
                    newName = FieldNameGenerator.generateName(signature, name);
                    cnat.setNameIndex(constants.addConstantUtf8(newName));
                }
            }
        }
    }

    private static void checkAssertionsDisabledField(ClassFile classFile)
    {
        ConstantPool constants = classFile.getConstantPool();
        Field[] fields = classFile.getFields();

        if (fields == null) {
            return;
        }

        int i = fields.length;
        Field field;
        String name;
        while (i-- > 0)
        {
            field = fields[i];

            if ((field.getAccessFlags() &
                    (Const.ACC_PUBLIC|Const.ACC_PROTECTED|
                            Const.ACC_PRIVATE|Const.ACC_SYNTHETIC|
                            Const.ACC_STATIC|Const.ACC_FINAL))
                    != (Const.ACC_STATIC|Const.ACC_FINAL) || field.getValueAndMethod() == null) {
                continue;
            }

            name = constants.getConstantUtf8(field.getNameIndex());
            if (! "$assertionsDisabled".equals(name)) {
                continue;
            }

            field.setAccessFlags(field.getAccessFlags() | Const.ACC_SYNTHETIC);
        }
    }

    private static boolean hasAAccessorMethodName(ClassFile classFile, Method method)
    {
        String methodName =
                classFile.getConstantPool().getConstantUtf8(method.getNameIndex());

        if (! methodName.startsWith("access$")) {
            return false;
        }

        int i = methodName.length();

        while (i-- > "access$".length())
        {
            if (! Character.isDigit(methodName.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasAEclipseSwitchTableMethodName(
            ClassFile classFile, Method method)
    {
        String methodName =
                classFile.getConstantPool().getConstantUtf8(method.getNameIndex());

        if (! methodName.startsWith("$SWITCH_TABLE$")) {
            return false;
        }

        String methodDescriptor =
                classFile.getConstantPool().getConstantUtf8(method.getDescriptorIndex());

        return "()[I".equals(methodDescriptor);
    }

    /** Parse Eclipse SwitchTable method
     * static int[] $SWITCH_TABLE$basic$data$TestEnum$enum1()
     * {
     *   if($SWITCH_TABLE$basic$data$TestEnum$enum1 != null)
     *     return $SWITCH_TABLE$basic$data$TestEnum$enum1;
     *   int[] ai = new int[enum1.values().length];
     *   try { ai[enum1.A.ordinal()] = 1; } catch(NoSuchFieldError _ex) { }
     *   try { ai[enum1.B.ordinal()] = 2; } catch(NoSuchFieldError _ex) { }
     *   return $SWITCH_TABLE$basic$data$TestEnum$enum1 = ai;
     * }
     *
     * and parse DEX SwitchTable method
     * static int[] $SWITCH_TABLE$gr$androiddev$FuelPrices$StaticTools$LocationProvider()
     * {
     *   int[] local0 = $SWITCH_TABLE$gr$androiddev$FuelPrices$StaticTools$LocationProvider;
     *   if (local0 != null)
     *     return local0;
     *   int array = new int[StaticTools$LocationProvider.values().length()];
     *   try { array[StaticTools$LocationProvider.ANY.ordinal()] = 2; } catch (java/lang/NoSuchFieldError) --> 73
     *   try { array[StaticTools$LocationProvider.BESTOFBOTH.ordinal()] = 1; } catch (java/lang/NoSuchFieldError) --> 69
     *   try { array[StaticTools$LocationProvider.GPS.ordinal()] = 3; } catch (java/lang/NoSuchFieldError) --> 64
     *   try { array[StaticTools$LocationProvider.NETWORK.ordinal()] = 4; } catch (java/lang/NoSuchFieldError) --> 59
     *   $SWITCH_TABLE$gr$androiddev$FuelPrices$StaticTools$LocationProvider = array;
     *   return array;
     *   59: catch (java/lang/NoSuchFieldError) {}
     *   64: catch (java/lang/NoSuchFieldError) {}
     *   69: catch (java/lang/NoSuchFieldError) {}
     *   73: catch (java/lang/NoSuchFieldError) {}
     * }
     */
    private static void parseEclipseOrDexSwitchTableMethod(
            ClassFile classFile, Method method)
    {
        List<Instruction> list = method.getInstructions();
        int length = list.size();

        if (length >= 6 &&
                list.get(0).getOpcode() == ByteCodeConstants.DUPSTORE &&
                list.get(1).getOpcode() == ByteCodeConstants.IFXNULL &&
                list.get(2).getOpcode() == ByteCodeConstants.XRETURN &&
                list.get(3).getOpcode() == Const.POP &&
                list.get(4).getOpcode() == Const.ASTORE)
        {
            // Eclipse pattern
            ConstantPool constants = classFile.getConstantPool();
            List<Integer> enumNameIndexes = new ArrayList<>();

            Instruction instruction;
            ConstantFieldref cfr;
            ConstantNameAndType cnat;
            for (int index=5+2; index<length; index+=3)
            {
                instruction = list.get(index-2);

                if (instruction.getOpcode() != ByteCodeConstants.ARRAYSTORE ||
                        list.get(index-1).getOpcode() != Const.GOTO ||
                        list.get(index).getOpcode() != Const.POP) {
                    break;
                }

                instruction = ((ArrayStoreInstruction)instruction).getIndexref();

                if (instruction.getOpcode() != Const.INVOKEVIRTUAL) {
                    break;
                }

                instruction = ((Invokevirtual)instruction).getObjectref();

                if (instruction.getOpcode() != Const.GETSTATIC) {
                    break;
                }

                cfr = constants.getConstantFieldref(((GetStatic)instruction).getIndex());
                cnat = constants.getConstantNameAndType(
                        cfr.getNameAndTypeIndex());

                // Add enum name index
                enumNameIndexes.add(cnat.getNameIndex());
            }

            classFile.getSwitchMaps().put(
                    method.getNameIndex(), enumNameIndexes);
        }
        else if (length >= 7 &&
                list.get(0).getOpcode() == Const.ASTORE &&
                list.get(1).getOpcode() == ByteCodeConstants.IFXNULL &&
                list.get(2).getOpcode() == ByteCodeConstants.XRETURN &&
                list.get(3).getOpcode() == Const.ASTORE &&
                list.get(4).getOpcode() == ByteCodeConstants.ARRAYSTORE)
        {
            // Dalvik pattern
            ConstantPool constants = classFile.getConstantPool();
            List<Integer> enumNameIndexes = new ArrayList<>();

            Instruction instruction;
            ConstantFieldref cfr;
            ConstantNameAndType cnat;
            for (int index=4; index<length; index++)
            {
                instruction = list.get(index);

                if (instruction.getOpcode() != ByteCodeConstants.ARRAYSTORE) {
                    break;
                }

                instruction = ((ArrayStoreInstruction)instruction).getIndexref();

                if (instruction.getOpcode() != Const.INVOKEVIRTUAL) {
                    break;
                }

                instruction = ((Invokevirtual)instruction).getObjectref();

                if (instruction.getOpcode() != Const.GETSTATIC) {
                    break;
                }

                cfr = constants.getConstantFieldref(((GetStatic)instruction).getIndex());
                cnat = constants.getConstantNameAndType(
                        cfr.getNameAndTypeIndex());

                // Add enum name index
                enumNameIndexes.add(cnat.getNameIndex());
            }

            classFile.getSwitchMaps().put(
                    method.getNameIndex(), enumNameIndexes);
        }
    }

    private static void preAnalyzeMethods(ClassFile classFile)
    {
        analyzeMethodref(classFile);

        Method[] methods = classFile.getMethods();

        if (methods == null) {
            return;
        }

        DefaultVariableNameGenerator variableNameGenerator =
                new DefaultVariableNameGenerator(classFile);
        int outerThisFieldrefIndex = 0;

        for (final Method method : methods)
        {
            try
            {
                if (method.getCode() == null)
                {
                    if ((method.getAccessFlags() &
                            (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) == 0)
                    {
                        // Create missing local variable table
                        LocalVariableAnalyzer.analyze(
                                classFile, method, variableNameGenerator, null, null);
                    }
                }
                else
                {
                    outerThisFieldrefIndex = preAnalyzeSingleMethod(classFile, variableNameGenerator, outerThisFieldrefIndex, method);
                }
            }
            catch (Exception e)
            {
                assert ExceptionUtil.printStackTrace(e);
                method.setContainsError(true);
            }
        }

        if (outerThisFieldrefIndex != 0) {
            analyzeOuterReferences(classFile, outerThisFieldrefIndex);
        }
    }

    public static int preAnalyzeSingleMethod(ClassFile classFile, DefaultVariableNameGenerator variableNameGenerator, int outerThisFieldrefIndex, final Method method) {
        // Build instructions
        List<Instruction> list = new ArrayList<>();
        List<Instruction> listForAnalyze = new ArrayList<>();

        InstructionListBuilder.build(
                classFile, method, list, listForAnalyze);
        method.setInstructions(list);

        if ((method.getAccessFlags() & (Const.ACC_PUBLIC|Const.ACC_PROTECTED|Const.ACC_PRIVATE|Const.ACC_STATIC)) == Const.ACC_STATIC &&
                hasAAccessorMethodName(classFile, method))
        {
            // Recherche des accesseurs
            AccessorAnalyzer.analyze(classFile, method);
            // Setup access flag : JDK 1.4 not set synthetic flag...
            method.setAccessFlags(method.getAccessFlags() | Const.ACC_SYNTHETIC);
        }
        else if ((method.getAccessFlags() &
                (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) == 0)
        {
            // Create missing local variable table
            LocalVariableAnalyzer.analyze(
                    classFile, method, variableNameGenerator, list, listForAnalyze);

            // Recherche du numéro de l'attribut contenant la reference
            // de la classe externe
            outerThisFieldrefIndex = searchOuterThisFieldrefIndex(
                    classFile, method, list, outerThisFieldrefIndex);
        }
        else if ((method.getAccessFlags() & (Const.ACC_PUBLIC|Const.ACC_PROTECTED|Const.ACC_PRIVATE|Const.ACC_STATIC|Const.ACC_SYNTHETIC))
                == (Const.ACC_STATIC|Const.ACC_SYNTHETIC) &&
                hasAEclipseSwitchTableMethodName(classFile, method))
        {
            // Parse "static int[] $SWITCH_TABLE$...()" method
            parseEclipseOrDexSwitchTableMethod(classFile, method);
        }
        return outerThisFieldrefIndex;
    }

    private static void analyzeMethods(
            ReferenceMap referenceMap,
            Map<String, ClassFile> innerClassesMap,
            ClassFile classFile)
    {
        Method[] methods = classFile.getMethods();

        if (methods == null) {
            return;
        }

        // Initialisation du reconstructeur traitant l'acces des champs et
        // méthodes externes si la classe courante est une classe interne ou
        // si elle contient des classes internes
        OuterReferenceReconstructor outerReferenceReconstructor =
                innerClassesMap != null ?
                        new OuterReferenceReconstructor(innerClassesMap, classFile) : null;

        for (final Method method : methods)
        {
            if ((method.getAccessFlags() &
                    (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) != 0 ||
                    method.getCode() == null ||
                    method.containsError()) {
                continue;
            }

            try
            {
                analyzeSingleMethod(referenceMap, classFile, outerReferenceReconstructor, method);
            }
            catch (Exception e)
            {
                assert ExceptionUtil.printStackTrace(e);
                method.setContainsError(true);
            }
        }

        // Recherche des initialisations des attributs statiques Enum
        InitDexEnumFieldsReconstructor.reconstruct(classFile);
        // Recherche des initialisations des attributs statiques
        InitStaticFieldsReconstructor.reconstruct(classFile);
        // Recherche des initialisations des attributs d'instance
        InitInstanceFieldsReconstructor.reconstruct(classFile);

        postAnalyzeMethods(classFile, methods);
    }

    private static void postAnalyzeMethods(ClassFile classFile, Method[] methods) {
        for (final Method method : methods)
        {
            if ((method.getAccessFlags() &
                    (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) != 0 ||
                    method.getCode() == null ||
                    method.getFastNodes() == null ||
                    method.containsError()) {
                continue;
            }
            postAnalyzeSingleMethod(classFile, method);
        }
    }

    public static void analyzeSingleMethod(ReferenceMap referenceMap, ClassFile classFile, OuterReferenceReconstructor outerReferenceReconstructor, final Method method) {
        List<Instruction> list = method.getInstructions();

        // Recontruct access to outer fields and methods
        if (outerReferenceReconstructor != null) {
            outerReferenceReconstructor.reconstruct(method, list);
        }
        // Re-construct 'new' intruction
        NewInstructionReconstructor.reconstruct(classFile, method, list);
        SimpleNewInstructionReconstructor.reconstruct(classFile, method, list);
        // Recontruction des instructions de pre-incrementation non entier
        PreIncReconstructor.reconstruct(list);
        // Recontruction des instructions de post-incrementation non entier
        PostIncReconstructor.reconstruct(list);
        // Recontruction du mot clé '.class' pour le JDK 1.1.8 - A
        DotClass118AReconstructor.reconstruct(
                referenceMap, classFile, list);
        // Recontruction du mot clé '.class' pour le JDK 1.4
        DotClass14Reconstructor.reconstruct(
                referenceMap, classFile, list);
        // Replace StringBuffer and StringBuilder in java source line
        replaceStringBufferAndStringBuilder(classFile, list);
        // Remove unused pop instruction
        removeUnusedPopInstruction(list);
        // Transformation des tests sur des types 'long' et 'double'
        transformTestOnLongOrDouble(list);
        // Set constant type of "String.indexOf(...)" methods
        setConstantTypeInStringIndexOfMethods(classFile, list);
        // Elimine la séquence DupStore(this) ... DupLoad() ... DupLoad().
        // Cette operation doit être executee avant
        // 'AssignmentInstructionReconstructor'.
        DupStoreThisReconstructor.reconstruct(list);
        // Recontruction des affectations multiples
        // Cette operation doit être executee avant
        // 'InitArrayInstructionReconstructor', 'TernaryOpReconstructor'
        // et la construction des instructions try-catch et finally.
        // Cette operation doit être executee après 'DupStoreThisReconstructor'.
        AssignmentInstructionReconstructor.reconstruct(list);
        // Elimine les doubles casts et ajoute des casts devant les
        // constantes numeriques si necessaire.
        CheckCastAndConvertInstructionVisitor.visit(
                classFile.getConstantPool(), list);

        // Build fast instructions
        List<Instruction> fastList =
                new ArrayList<>(list);
        method.setFastNodes(fastList);

        // DEBUG //
        //ConstantPool debugConstants = classFile.getConstantPool();
        //String debugMethodName = debugConstants.getConstantUtf8(method.nameIndex);
        // DEBUG //
        FastInstructionListBuilder.build(
                referenceMap, classFile, method, fastList);

        // Ajout des déclarations des variables locales temporaires
        DupLocalVariableAnalyzer.declare(classFile, method, fastList);
    }

    public static void postAnalyzeSingleMethod(ClassFile classFile, final Method method) {
        try
        {
            // Remove empty and enum super call
            analyseAndModifyConstructors(classFile, method);
            // Check line number of 'return'
            ReturnLineNumberAnalyzer.check(method);
            // Remove last instruction 'return'
            removeLastReturnInstruction(method);
        }
        catch (Exception e)
        {
            assert ExceptionUtil.printStackTrace(e);
            method.setContainsError(true);
        }
    }

    private static int searchOuterThisFieldrefIndex(
            ClassFile classFile, Method method,
            List<Instruction> list, int outerThisFieldrefIndex)
    {
        // Is classFile an inner class ?
        if (!classFile.isAInnerClass() ||
                (classFile.getAccessFlags() & Const.ACC_STATIC) != 0) {
            return 0;
        }

        ConstantPool constants = classFile.getConstantPool();

        // Is method a constructor ?
        if (method.getNameIndex() != constants.getInstanceConstructorIndex()) {
            return outerThisFieldrefIndex;
        }

        // Is parameters counter greater than 0 ?
        AttributeSignature as = method.getAttributeSignature();
        String methodSignature = constants.getConstantUtf8(
                as==null ? method.getDescriptorIndex() : as.getSignatureIndex());

        if (methodSignature.charAt(1) == ')') {
            return 0;
        }

        // Search instruction 'PutField(#, ALoad(1))' before super <init>
        // method call.
        int length = list.size();

        Instruction instruction;
        for (int i=0; i<length; i++)
        {
            instruction = list.get(i);

            if (instruction.getOpcode() == Const.PUTFIELD)
            {
                // Is '#' equals to 'outerThisFieldIndex' ?
                PutField pf = (PutField)instruction;

                if (pf.getObjectref().getOpcode() == Const.ALOAD &&
                        pf.getValueref().getOpcode() == Const.ALOAD &&
                        ((ALoad)pf.getObjectref()).getIndex() == 0 &&
                        ((ALoad)pf.getValueref()).getIndex() == 1 && (outerThisFieldrefIndex == 0 ||
                        pf.getIndex() == outerThisFieldrefIndex)) {
                    return pf.getIndex();
                }
            }
            else if (instruction.getOpcode() == Const.INVOKESPECIAL)
            {
                // Is a call to "this()" in constructor ?
                Invokespecial is = (Invokespecial)instruction;
                ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());
                if (cmr.getClassIndex() == classFile.getThisClassIndex())
                {
                    ConstantNameAndType cnat =
                            constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
                    if (cnat.getNameIndex() == constants.getInstanceConstructorIndex())
                    {
                        return outerThisFieldrefIndex;
                    }
                }
            }
        }

        // Instruction 'PutField' not found
        return 0;
    }

    /** Traitement des references externes des classes internes. */
    private static void analyzeOuterReferences(
            ClassFile classFile, int outerThisFieldrefIndex)
    {
        Method[] methods = classFile.getMethods();

        if (methods == null) {
            return;
        }

        int length = methods.length;

        // Recherche de l'attribut portant la reference vers la classe
        // externe.
        ConstantPool constants = classFile.getConstantPool();
        ConstantFieldref cfr = constants.getConstantFieldref(outerThisFieldrefIndex);

        if (cfr.getClassIndex() == classFile.getThisClassIndex())
        {
            ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
            Field[] fields = classFile.getFields();

            if (fields != null)
            {
                Field field;
                for (int i=fields.length-1; i>=0; --i)
                {
                    field = fields[i];

                    if (field.getNameIndex() == cnat.getNameIndex() &&
                            field.getDescriptorIndex() == cnat.getSignatureIndex())
                    {
                        classFile.setOuterThisField(field);
                        // Ensure outer this field is a synthetic field.
                        field.setAccessFlags(field.getAccessFlags() | Const.ACC_SYNTHETIC);
                        break;
                    }
                }
            }
        }

        List<Instruction> list;
        int listLength;
        for (int i=0; i<length; i++)
        {
            final Method method = methods[i];

            if (method.getCode() == null || method.containsError()) {
                continue;
            }

            list = method.getInstructions();

            if (list == null) {
                continue;
            }

            listLength = list.size();

            if (method.getNameIndex() == constants.getInstanceConstructorIndex())
            {
                Instruction instruction;
                // Remove PutField instruction with index = outerThisFieldrefIndex
                // in constructors
                for (int index=0; index<listLength; index++)
                {
                    instruction = list.get(index);

                    if (instruction.getOpcode() == Const.PUTFIELD &&
                            ((PutField)instruction).getIndex() == outerThisFieldrefIndex)
                    {
                        list.remove(index);
                        break;
                    }
                }
            }
            else if ((method.getAccessFlags() &
                    (Const.ACC_SYNTHETIC|Const.ACC_STATIC)) == Const.ACC_STATIC &&
                    method.getNameIndex() != constants.getClassConstructorIndex() &&
                    listLength == 1 &&
                    classFile.isAInnerClass())
            {
                // Search accessor method:
                //   static TestInnerClass.InnerClass.InnerInnerClass basic/data/TestInnerClass$InnerClass$InnerInnerClass$InnerInnerInnerClass.access$100(InnerInnerInnerClass x0)
                //   {
                //      Byte code:
                //        0: aload_0
                //        1: getfield 1    basic/data/TestInnerClass$InnerClass$InnerInnerClass$InnerInnerInnerClass:this$1    Lbasic/data/TestInnerClass$InnerClass$InnerInnerClass;
                //        4: areturn
                //   }
                Instruction instruction = list.get(0);
                if (instruction.getOpcode() != ByteCodeConstants.XRETURN) {
                    continue;
                }

                instruction = ((ReturnInstruction)instruction).getValueref();
                if (instruction.getOpcode() != Const.GETFIELD) {
                    continue;
                }

                GetField gf = (GetField)instruction;
                if (gf.getObjectref().getOpcode() != Const.ALOAD ||
                        ((ALoad)gf.getObjectref()).getIndex() != 0) {
                    continue;
                }

                cfr = constants.getConstantFieldref(gf.getIndex());
                if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                    continue;
                }

                ConstantNameAndType cnat =
                        constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
                Field outerField = classFile.getOuterThisField();
                if (cnat.getSignatureIndex() != outerField.getDescriptorIndex()) {
                    continue;
                }

                if (cnat.getNameIndex() == outerField.getNameIndex())
                {
                    // Ensure accessor method is a synthetic method
                    method.setAccessFlags(method.getAccessFlags() | Const.ACC_SYNTHETIC);
                }
            }
        }
    }

    /**
     * 1) Retrait de la sequence suivante pour les contructeurs :
     *    Invokespecial(ALoad 0, <init>, [ ])
     * 2) Store super constructor parameter count to display anonymous
     *    class instanciation
     * 3) Store outer parameter position on field for inner and anonymous classes
     */
    private static void analyseAndModifyConstructors(
            ClassFile classFile, Method method)
    {
        ConstantPool constants = classFile.getConstantPool();

        if (method.getNameIndex() == constants.getInstanceConstructorIndex())
        {
            List<Instruction> list = method.getFastNodes();

            Instruction instruction;
            while (!list.isEmpty())
            {
                instruction = list.get(0);

                if (instruction.getOpcode() == Const.INVOKESPECIAL)
                {
                    Invokespecial is = (Invokespecial)instruction;

                    if (is.getObjectref().getOpcode() == Const.ALOAD &&
                            ((ALoad)is.getObjectref()).getIndex() == 0)
                    {
                        ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());
                        ConstantNameAndType cnat =
                                constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                        if (cnat.getNameIndex() == constants.getInstanceConstructorIndex())
                        {
                            if (cmr.getClassIndex() == classFile.getSuperClassIndex())
                            {
                                int count = is.getArgs().size();

                                method.setSuperConstructorParameterCount(count);

                                if ((classFile.getAccessFlags() & Const.ACC_ENUM) != 0)
                                {
                                    if (count == 2)
                                    {
                                        // Retrait de l'appel du constructeur s'il
                                        // n'a que les deux paramètres standard.
                                        list.remove(0);
                                    }
                                } else if (count == 0)
                                {
                                    // Retrait de l'appel du constructeur s'il
                                    // n'a aucun parametre.
                                    list.remove(0);
                                }
                            }

                            break;
                        }
                    }
                }
                else if (instruction.getOpcode() == Const.PUTFIELD)
                {
                    PutField pf = (PutField)instruction;

                    if (pf.getValueref().getOpcode() == ByteCodeConstants.LOAD
                     || pf.getValueref().getOpcode() == Const.ALOAD
                     || pf.getValueref().getOpcode() == Const.ILOAD) {
                        IndexInstruction ii = (IndexInstruction)pf.getValueref();
                        // Rappel sur l'ordre des parametres passes aux constructeurs:
                        //  <init>(outer this, p1, p2, ..., outer local var1, ...)
                        // Rappel sur l'organisation des variables locales:
                        //  0: this
                        //  1: outer this
                        //  2: p1
                        //  ...
                        if (ii.getIndex() > 1)
                        {
                            // Stockage de la position du parametre du
                            // constructeur initialisant le champs
                            ConstantFieldref cfr = constants.getConstantFieldref(pf.getIndex());
                            ConstantNameAndType cnat =
                                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
                            Field field =
                                    classFile.getField(cnat.getNameIndex(), cnat.getSignatureIndex());
                            field.setAnonymousClassConstructorParameterIndex(ii.getIndex() - 1);
                            // Ensure this field is a synthetic field.
                            field.setAccessFlags(field.getAccessFlags() | Const.ACC_SYNTHETIC);
                        }
                    }
                }

                // Retrait des instructions precedents l'appel au constructeur
                // de la classe mere.
                list.remove(0);
            }
        }
    }

    private static void removeLastReturnInstruction(Method method)
    {
        List<Instruction> list = method.getFastNodes();

        if (list != null)
        {
            int length = list.size();

            if (length > 0)
            {
                int lastOpCode = list.get(length-1).getOpcode();
                if (lastOpCode == Const.RETURN) {
                    list.remove(length-1);
                } else if (lastOpCode == FastConstants.LABEL) {
                    FastLabel fl = (FastLabel)list.get(length-1);
                    if (fl.getInstruction().getOpcode() == Const.RETURN) {
                        fl.setInstruction(null);
                    }
                }
            }
        }
    }

    private static void replaceStringBufferAndStringBuilder(
            ClassFile classFile, List<Instruction> list)
    {
        ReplaceStringBuxxxerVisitor visitor = new ReplaceStringBuxxxerVisitor(
                classFile.getConstantPool());

        int length = list.size();

        for (int i=0; i<length; i++) {
            visitor.visit(list.get(i));
        }
    }

    private static void removeUnusedPopInstruction(List<Instruction> list)
    {
        int index = list.size();

        Instruction instruction;
        while (index-- > 0)
        {
            instruction = list.get(index);

            if (instruction.getOpcode() == Const.POP &&
                      (((Pop)instruction).getObjectref().getOpcode() == Const.GETFIELD
                    || ((Pop)instruction).getObjectref().getOpcode() == Const.GETSTATIC
                    || ((Pop)instruction).getObjectref().getOpcode() == ByteCodeConstants.OUTERTHIS
                    || ((Pop)instruction).getObjectref().getOpcode() == Const.ALOAD
                    || ((Pop)instruction).getObjectref().getOpcode() == Const.ILOAD
                    || ((Pop)instruction).getObjectref().getOpcode() == ByteCodeConstants.LOAD)) {
                list.remove(index);
            }
        }
    }

    private static void transformTestOnLongOrDouble(List<Instruction> list)
    {
        int index = list.size();

        Instruction instruction;
        while (index-- > 0)
        {
            instruction = list.get(index);

            if (instruction.getOpcode() == ByteCodeConstants.IF)
            {
                IfInstruction ii = (IfInstruction)instruction;

                if ((ii.getCmp() == ByteCodeConstants.CMP_EQ
                  || ii.getCmp() == ByteCodeConstants.CMP_NE
                  || ii.getCmp() == ByteCodeConstants.CMP_LT
                  || ii.getCmp() == ByteCodeConstants.CMP_GE
                  || ii.getCmp() == ByteCodeConstants.CMP_GT
                  || ii.getCmp() == ByteCodeConstants.CMP_LE)
                  && ii.getValue().getOpcode() == ByteCodeConstants.BINARYOP)
                {
                    BinaryOperatorInstruction boi =
                            (BinaryOperatorInstruction)ii.getValue();
                    if ("<".equals(boi.getOperator()))
                    {
                        // Instruction 'boi' = ?CMP, ?CMPL or ?CMPG
                        list.set(index, new IfCmp(
                                ByteCodeConstants.IFCMP, ii.getOffset(),
                                ii.getLineNumber(), ii.getCmp(),
                                boi.getValue1(), boi.getValue2(), ii.getBranch()));
                    }
                }
            }
        }
    }

    private static void setConstantTypeInStringIndexOfMethods(
            ClassFile classFile, List<Instruction> list)
    {
        SetConstantTypeInStringIndexOfMethodsVisitor visitor =
                new SetConstantTypeInStringIndexOfMethodsVisitor(
                        classFile.getConstantPool());

        visitor.visit(list);
    }

    private static void analyzeEnum(ClassFile classFile)
    {
        if (classFile.getFields() == null) {
            return;
        }

        ConstantPool constants = classFile.getConstantPool();
        String enumArraySignature = "[" + classFile.getInternalClassName();

        // Recherche du champ statique possedant un acces ACC_ENUM et un
        // type '[LenumXXXX;'
        Field[] fields = classFile.getFields();
        Field field;
        Instruction instruction;
        String fieldName;
        for (int i=fields.length-1; i>=0; --i)
        {
            field = fields[i];

            if ((field.getAccessFlags() & (Const.ACC_SYNTHETIC|Const.ACC_ENUM)) == 0 ||
                    field.getValueAndMethod() == null) {
                continue;
            }

            instruction = field.getValueAndMethod().value();

            if (instruction.getOpcode() != ByteCodeConstants.INITARRAY &&
                    instruction.getOpcode() != ByteCodeConstants.NEWANDINITARRAY ||
                    !constants.getConstantUtf8(field.getDescriptorIndex()).equals(enumArraySignature)) {
                continue;
            }

            fieldName = constants.getConstantUtf8(field.getNameIndex());
            if (! StringConstants.ENUM_VALUES_ARRAY_NAME.equals(fieldName) &&
                    ! StringConstants.ENUM_VALUES_ARRAY_NAME_ECLIPSE.equals(fieldName)) {
                continue;
            }

            // Stockage des valeurs de l'enumeration
            classFile.setEnumValues(((InitArrayInstruction)instruction).getValues());
            break;
        }
    }
}
