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
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.util.StringConstants;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.ExceptionLoad;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.ILoad;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.process.analyzer.classfile.visitor.AddCheckCastVisitor;
import jd.core.process.analyzer.classfile.visitor.SearchInstructionByOffsetVisitor;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.process.analyzer.util.InstructionUtil;
import jd.core.process.analyzer.variable.DefaultVariableNameGenerator;
import jd.core.util.SignatureUtil;
import jd.core.util.UtilConstants;

public final class LocalVariableAnalyzer
{
    private LocalVariableAnalyzer() {
    }
    /**
     * Indexe de signature pour les variables locales de type inconnu. Si le
     * type de la variable n'a pu être determiné, la variable sera type
     * 'Object'.
     */
    private static final int UNDEFINED_TYPE = -1;
    /**
     * Indexe de signature pour les variables locales de type numérique inconnu.
     * Si le type de la variable n'a pu être determiné, la variable sera type
     * 'int'.
     */
    private static final int NUMBER_TYPE = -2;
    /**
     * Indexe de signature pour les variables locales de type 'Object' et
     * nécessitant l'insertion d'instructions 'cast'.
     */
    private static final int OBJECT_TYPE = -3;

    public static void analyze(
            ClassFile classFile, Method method,
            DefaultVariableNameGenerator variableNameGenerator,
            List<Instruction> list, List<Instruction> listForAnalyze)
    {
        ConstantPool constants = classFile.getConstantPool();
        variableNameGenerator.clearLocalNames();

        // DEBUG String debugClassName = classFile.getInternalClassName();
        // DEBUG String debugMethodName = constants.getConstantUtf8(method.nameIndex);

        // Reconstruction de la Liste des variables locales
        byte[] code = method.getCode();
        int codeLength = code == null ? 0 : code.length;
        LocalVariables localVariables = method.getLocalVariables();

        if (localVariables == null)
        {
            // Ajout d'entrées dans le tableau pour les parametres
            localVariables = new LocalVariables();
            method.setLocalVariables(localVariables);

            // Add this
            if ((method.getAccessFlags() & Const.ACC_STATIC) == 0)
            {
                int nameIndex = constants.addConstantUtf8(
                        StringConstants.THIS_LOCAL_VARIABLE_NAME);
                int signatureIndex =
                        constants.addConstantUtf8(classFile.getInternalClassName());
                LocalVariable lv =
                        new LocalVariable(0, codeLength, nameIndex, signatureIndex, 0);
                localVariables.add(lv);
            }

            if (method.getNameIndex() == constants.getInstanceConstructorIndex() &&
                    classFile.isAInnerClass() &&
                    (classFile.getAccessFlags() & Const.ACC_STATIC) == 0)
            {
                // Add outer this
                int nameIndex = constants.addConstantUtf8(
                        StringConstants.OUTER_THIS_LOCAL_VARIABLE_NAME);
                String internalClassName = classFile.getInternalClassName();
                int lastInnerClassSeparatorIndex =
                        internalClassName.lastIndexOf(StringConstants.INTERNAL_INNER_SEPARATOR);

                String internalOuterClassName =
                        internalClassName.substring(0, lastInnerClassSeparatorIndex) + ';';

                int signatureIndex = constants.addConstantUtf8(internalOuterClassName);
                LocalVariable lv =
                        new LocalVariable(0, codeLength, nameIndex, signatureIndex, 1);
                localVariables.add(lv);
            }

            // Add Parameters
            analyzeMethodParameter(
                    classFile, constants, method, localVariables,
                    variableNameGenerator, codeLength);

            localVariables.setIndexOfFirstLocalVariable(localVariables.size());

            if (code != null)
            {
                generateMissingMonitorLocalVariables(
                        constants, localVariables, listForAnalyze);
            }
        }
        else
        {
            // Traitement des entrées correspondant aux parametres
            AttributeSignature as = method.getAttributeSignature();
            String methodSignature = constants.getConstantUtf8(
                    as==null ? method.getDescriptorIndex() : as.getSignatureIndex());

            int indexOfFirstLocalVariable =
                    ((method.getAccessFlags() & Const.ACC_STATIC) == 0 ? 1 : 0) +
                    SignatureUtil.getParameterSignatureCount(methodSignature);

            if (indexOfFirstLocalVariable > localVariables.size())
            {
                // Dans le cas des méthodes générées automatiquement par le
                // compilateur (comme par exemple les méthode des enums), le
                // tableau des variables locales est incomplet.
                // Add Parameters
                analyzeMethodParameter(
                        classFile, constants, method, localVariables,
                        variableNameGenerator, codeLength);
            }

            localVariables.setIndexOfFirstLocalVariable(
                    indexOfFirstLocalVariable);

            if (code != null)
            {
                generateMissingMonitorLocalVariables(
                        constants, localVariables, listForAnalyze);
                checkLocalVariableRanges(
                        constants, code, localVariables,
                        variableNameGenerator, listForAnalyze);
            }

            // La fusion des variables locales genere des erreurs. Mise en
            // commentaire a la version 0.5.3.
            //  fr.oseo.fui.actions.partenaire.FicheInformationAction:
            //   InterlocuteurBO interlocuteur;
            //   for (InterlocuteurBO partenaire = projet.getPartenaires().iterator(); partenaire.hasNext(); )
            //   {
            //    interlocuteur = (InterlocuteurBO)partenaire.next();
            //    ...
            //   }
            //   ...
            //   for (partenaire = projet.getPartenaires().iterator(); partenaire.hasNext(); )
            //   {
            //    interlocuteur = (InterlocuteurBO)partenaire.next();
            //    ...
            //   }
            //MergeLocalVariables(localVariables);
        }

        // Add local variables
        // Create new local variables, set range and type
        if (code != null)
        {
            String returnedSignature = getReturnedSignature(classFile, method);

            analyzeMethodCode(
                    constants, localVariables, list, listForAnalyze,
                    returnedSignature);

            // Upgrade byte type to char type
            // Substitution des types byte par char dans les instructions
            // bipush et sipush
            setConstantTypes(
                    constants, localVariables,
                    list, listForAnalyze, returnedSignature);

            initialyzeExceptionLoad(listForAnalyze, localVariables);
        }

        generateLocalVariableNames(
                constants, localVariables, variableNameGenerator);
    }

    private static void analyzeMethodParameter(
            ClassFile classFile, ConstantPool constants,
            Method method, LocalVariables localVariables,
            DefaultVariableNameGenerator variableNameGenerator, int codeLength)
    {
        // Le descripteur et la signature sont differentes pour les
        // constructeurs des Enums !
        AttributeSignature as = method.getAttributeSignature();
        String methodSignature = constants.getConstantUtf8(
                as == null ? method.getDescriptorIndex() : as.getSignatureIndex());
        List<String> parameterTypes =
                SignatureUtil.getParameterSignatures(methodSignature);

        if (parameterTypes != null)
        {
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
                    (method.getAccessFlags() & Const.ACC_STATIC) != 0;
            int variableIndex = staticMethodFlag ? 0 : 1;

            int firstVisibleParameterCounter = 0;

            if (method.getNameIndex() == constants.getInstanceConstructorIndex())
            {
                if ((classFile.getAccessFlags() & Const.ACC_ENUM) != 0)
                {
                    if (as == null) {
                        firstVisibleParameterCounter = 2;
                    } else {
                        variableIndex = 3;
                    }
                } else if (classFile.isAInnerClass() && (classFile.getAccessFlags() & Const.ACC_STATIC) == 0) {
                    firstVisibleParameterCounter = 1;
                }
            }

            int anonymousClassDepth = 0;
            ClassFile anonymousClassFile = classFile;

            while (anonymousClassFile != null &&
                    anonymousClassFile.getInternalAnonymousClassName() != null)
            {
                anonymousClassDepth++;
                anonymousClassFile = anonymousClassFile.getOuterClass();
            }

            final int length = parameterTypes.size();

            final int varargsParameterIndex;

            if ((method.getAccessFlags() & Const.ACC_VARARGS) == 0)
            {
                varargsParameterIndex = Integer.MAX_VALUE;
            }
            else
            {
                varargsParameterIndex = length - 1;
            }

            for (int parameterIndex=0; parameterIndex<length; parameterIndex++)
            {
                final String signature = parameterTypes.get(parameterIndex);

                if (/* (parameterIndex >= firstVisibleParameterCounter) && */
                        localVariables.getLocalVariableWithIndexAndOffset(variableIndex, 0) == null)
                {
                    boolean appearsOnceFlag = signatureAppearsOnceInParameters(
                            parameterTypes, firstVisibleParameterCounter,
                            length, signature);
                    final String name =
                            variableNameGenerator.generateParameterNameFromSignature(
                                    signature, appearsOnceFlag,
                                    parameterIndex==varargsParameterIndex,
                                    anonymousClassDepth);

                    int nameIndex = constants.addConstantUtf8(name);
                    int signatureIndex = constants.addConstantUtf8(signature);
                    LocalVariable lv = new LocalVariable(
                            0, codeLength, nameIndex, signatureIndex, variableIndex);
                    localVariables.add(lv);
                }

                final char firstChar = signature.charAt(0);
                variableIndex +=
                        firstChar == 'D' || firstChar == 'J' ? 2 : 1;
            }
        }
    }

    private static void generateMissingMonitorLocalVariables(
            ConstantPool constants, LocalVariables localVariables,
            List<Instruction> listForAnalyze)
    {
        int length = listForAnalyze.size();

        Instruction instruction;
        MonitorEnter mEnter;
        int monitorLocalVariableIndex;
        int monitorLocalVariableOffset;
        int monitorLocalVariableLength;
        int monitorExitCount;
        int j;
        LocalVariable lv;
        for (int i=1; i<length; i++)
        {
            instruction = listForAnalyze.get(i);

            if (instruction.getOpcode() != Const.MONITORENTER) {
                continue;
            }

            mEnter = (MonitorEnter)instruction;
            monitorLocalVariableLength = 1;

            if (mEnter.getObjectref().getOpcode() == ByteCodeConstants.DUPLOAD)
            {
                /* DupStore( ? ) AStore( DupLoad ) MonitorEnter( DupLoad ) */
                instruction = listForAnalyze.get(i-1);
                if (instruction.getOpcode() != Const.ASTORE) {
                    continue;
                }
                AStore astore = (AStore)instruction;
                if (astore.getValueref().getOpcode() != ByteCodeConstants.DUPLOAD) {
                    continue;
                }
                DupLoad dupload1 = (DupLoad)mEnter.getObjectref();
                DupLoad dupload2 = (DupLoad)astore.getValueref();
                if (dupload1.getDupStore() != dupload2.getDupStore()) {
                    continue;
                }
                monitorLocalVariableIndex = astore.getIndex();
                monitorLocalVariableOffset = astore.getOffset();
            }
            else if (mEnter.getObjectref().getOpcode() == Const.ALOAD)
            {
                /* AStore( ? ) MonitorEnter( ALoad ) */
                ALoad aload = (ALoad)mEnter.getObjectref();
                instruction = listForAnalyze.get(i-1);
                if (instruction.getOpcode() != Const.ASTORE) {
                    continue;
                }
                AStore astore = (AStore)instruction;
                if (astore.getIndex() != aload.getIndex()) {
                    continue;
                }
                monitorLocalVariableIndex = astore.getIndex();
                monitorLocalVariableOffset = astore.getOffset();
            }
            else
            {
                continue;
            }

            // Recherche des intructions MonitorExit correspondantes
            monitorExitCount = 0;
            // Recherche en avant
            j = i;
            while (++j < length)
            {
                instruction = listForAnalyze.get(j);
                if (instruction.getOpcode() != Const.MONITOREXIT || ((MonitorExit)instruction).getObjectref().getOpcode() != Const.ALOAD) {
                    continue;
                }
                ALoad al = (ALoad)((MonitorExit)instruction).getObjectref();
                if (al.getIndex() == monitorLocalVariableIndex)
                {
                    monitorLocalVariableLength =
                            al.getOffset() - monitorLocalVariableOffset;
                    monitorExitCount++;
                }
            }

            if (monitorExitCount == 1)
            {
                // Recherche en arriere (Jikes 1.22)
                j = i;
                ALoad al;
                while (j-- > 0)
                {
                    instruction = listForAnalyze.get(j);
                    if (instruction.getOpcode() != Const.MONITOREXIT || ((MonitorExit)instruction).getObjectref().getOpcode() != Const.ALOAD) {
                        continue;
                    }
                    al = (ALoad)((MonitorExit)instruction).getObjectref();
                    if (al.getIndex() == monitorLocalVariableIndex)
                    {
                        monitorLocalVariableLength +=
                                monitorLocalVariableOffset - al.getOffset();
                        monitorLocalVariableOffset = al.getOffset();

                        monitorExitCount++;
                        break;
                    }
                }
            }

            if (monitorExitCount < 2) {
                continue;
            }

            // Verification de l'existance d'une variable locale
            lv = localVariables.getLocalVariableWithIndexAndOffset(
                    monitorLocalVariableIndex, monitorLocalVariableOffset);

            // Creation d'une variable locale
            if (lv == null ||
                    lv.getStartPc()+lv.getLength() < monitorLocalVariableOffset+monitorLocalVariableLength)
            {
                int signatureIndex =
                        constants.addConstantUtf8(StringConstants.INTERNAL_OBJECT_SIGNATURE);
                localVariables.add(new LocalVariable(
                        monitorLocalVariableOffset, monitorLocalVariableLength,
                        signatureIndex, signatureIndex, monitorLocalVariableIndex));
            }
        }
    }

    /**
     * Verification de la portee de chaque variable : la portee generee par les
     * compilateurs est incorrecte : elle commence une instruction trop tard!
     * De plus, la longueur de la portee est tres importante. Elle est
     * recalculée.
     */
    private static void checkLocalVariableRanges(
            ConstantPool constants, byte[] code, LocalVariables localVariables,
            DefaultVariableNameGenerator variableNameGenerator,
            List<Instruction> listForAnalyze)
    {
        // Reset length
        int length = localVariables.size();

        // Remise à  1 de la longueur des portées
        for (int i=localVariables.getIndexOfFirstLocalVariable(); i<length; i++) {
            localVariables.getLocalVariableAt(i).setLength(1);
        }

        // Update range
        length = listForAnalyze.size();

        Instruction instruction;
        for (int i=0; i<length; i++)
        {
            instruction = listForAnalyze.get(i);
            switch (instruction.getOpcode())
            {
            case ByteCodeConstants.PREINC,
                 ByteCodeConstants.POSTINC:
            {
                instruction = ((IncInstruction)instruction).getValue();
                if (instruction.getOpcode() == Const.ILOAD ||
                        instruction.getOpcode() == ByteCodeConstants.LOAD) {
                    checkLocalVariableRangesForIndexInstruction(
                            code, localVariables, (IndexInstruction)instruction);
                }
            }
            break;
            case Const.ASTORE:
            {
                AStore astore = (AStore)instruction;
                // ExceptionLoad ?
                if (astore.getValueref().getOpcode() == ByteCodeConstants.EXCEPTIONLOAD)
                {
                    ExceptionLoad el =
                            (ExceptionLoad)astore.getValueref();

                    if (el.getExceptionNameIndex() != 0)
                    {
                        LocalVariable lv =
                                localVariables.getLocalVariableWithIndexAndOffset(
                                        astore.getIndex(), astore.getOffset());

                        if (lv == null)
                        {
                            // Variable non trouvée. Recherche de la variable avec
                            // l'offset suivant car les compilateurs place 'startPc'
                            // une instruction plus après.
                            int nextOffset =
                                    ByteCodeUtil.nextInstructionOffset(code, astore.getOffset());
                            lv = localVariables.getLocalVariableWithIndexAndOffset(
                                    astore.getIndex(), nextOffset);
                            if (lv == null)
                            {
                                // Create a new local variable for exception
                                lv = new LocalVariable(
                                        astore.getOffset(), 1, -1,
                                        el.getExceptionNameIndex(), astore.getIndex(), true);
                                localVariables.add(lv);
                                String signature =
                                        constants.getConstantUtf8(el.getExceptionNameIndex());
                                boolean appearsOnce = signatureAppearsOnceInLocalVariables(
                                        localVariables, localVariables.size(),
                                        el.getExceptionNameIndex());
                                String name =
                                        variableNameGenerator.generateLocalVariableNameFromSignature(
                                                signature, appearsOnce);
                                lv.setNameIndex(constants.addConstantUtf8(name));
                            }
                            else
                            {
                                // Variable trouvée. Mise à  jour de 'startPc' de la
                                // portée.
                                lv.updateRange(astore.getOffset());
                            }
                        }
                    }
                }
                else if (i+1 < length &&
                        astore.getValueref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                        listForAnalyze.get(i+1).getOpcode() == Const.MONITORENTER)
                {
                    // Monitor ?
                    LocalVariable lv =
                            localVariables.getLocalVariableWithIndexAndOffset(
                                    astore.getIndex(), astore.getOffset());
                    if (lv == null)
                    {
                        MonitorEnter me = (MonitorEnter)listForAnalyze.get(i+1);
                        if (me.getObjectref().getOpcode() == ByteCodeConstants.DUPLOAD &&
                                ((DupLoad)astore.getValueref()).getDupStore() ==
                                ((DupLoad)me.getObjectref()).getDupStore())
                        {
                            // Create a new local variable for monitor
                            int signatureIndex = constants.addConstantUtf8(
                                    StringConstants.INTERNAL_OBJECT_SIGNATURE);
                            localVariables.add(new LocalVariable(
                                    astore.getOffset(), 1, signatureIndex,
                                    signatureIndex, astore.getIndex()));
                        }
                        else
                        {
                            // Default case
                            checkLocalVariableRangesForIndexInstruction(
                                    code, localVariables, astore);
                        }
                    }
                }
                else
                {
                    // Default case
                    checkLocalVariableRangesForIndexInstruction(
                            code, localVariables, astore);
                }
            }
            break;
            case Const.ISTORE,
                 Const.ILOAD,
                 ByteCodeConstants.STORE,
                 ByteCodeConstants.LOAD,
                 Const.ALOAD,
                 Const.IINC:
                checkLocalVariableRangesForIndexInstruction(
                        code, localVariables, (IndexInstruction)instruction);
                break;
            }
        }
    }

    private static void checkLocalVariableRangesForIndexInstruction(
            byte[] code, LocalVariables localVariables, IndexInstruction ii)
    {
        LocalVariable lv =
                localVariables.getLocalVariableWithIndexAndOffset(ii.getIndex(), ii.getOffset());

        if (lv == null)
        {
            // Variable non trouvée. Recherche de la variable avec
            // l'offset suivant car les compilateurs place 'startPc'
            // une instruction plus après.
            int nextOffset = ByteCodeUtil.nextInstructionOffset(code, ii.getOffset());
            lv = localVariables.getLocalVariableWithIndexAndOffset(ii.getIndex(), nextOffset);
            if (lv != null)
            {
                // Variable trouvée. Mise à  jour de 'startPc' de la
                // portée.
                lv.updateRange(ii.getOffset());
            }
            else
            {
                // Mise à  jour de la longueur de la portées de la
                // variable possedant le même index et precedement
                // definie.
                lv = localVariables.searchLocalVariableWithIndexAndOffset(ii.getIndex(), ii.getOffset());
                if (lv != null) {
                    lv.updateRange(ii.getOffset());
                }
            }
        }
        else
        {
            // Mise à  jour de la longeur de la portée
            lv.updateRange(ii.getOffset());
        }
    }

    // La fusion des variables locales genere des erreurs. Mise en
    // commentaire a la version 0.5.3.
    //  fr.oseo.fui.actions.partenaire.FicheInformationAction:
    //   InterlocuteurBO interlocuteur;
    //   for (InterlocuteurBO partenaire = projet.getPartenaires().iterator(); partenaire.hasNext(); )
    //   {
    //    interlocuteur = (InterlocuteurBO)partenaire.next();
    //    ...
    //   }
    //   ...
    //   for (partenaire = projet.getPartenaires().iterator(); partenaire.hasNext(); )
    //   {
    //    interlocuteur = (InterlocuteurBO)partenaire.next();
    //    ...
    //   }
    /*
     * Fusion des entrees du tableau possédant les mêmes numéro de slot,
     * le même nom et le même type. Le tableau genere pour le code suivant
     * contient deux entrees pour la variable 'a' !
        int a;
        if (e == null)
            a = 1;
        else
            a = 2;
        System.out.println(a);
     */
    //    private static void MergeLocalVariables(LocalVariables localVariables)
    //    {
    //        for (int i=localVariables.size()-1; i>0; --i)
    //        {
    //            LocalVariable lv1 = localVariables.getLocalVariableAt(i);
    //            for (int j=i-1; j>=0; --j)
    //            {
    //                LocalVariable lv2 = localVariables.getLocalVariableAt(j);
    //                if ((lv1.index == lv2.index) &&
    //                    (lv1.signatureIndex == lv2.signatureIndex) &&
    //                    (lv1.nameIndex == lv2.nameIndex))
    //                {
    //                    localVariables.remove(i);
    //                    lv2.updateRange(lv1.startPc);
    //                    lv2.updateRange(lv1.startPc+lv1.length-1);
    //                    break;
    //                }
    //            }
    //        }
    //    }

    // Create new local variables, set range and type, update attribute
    // 'exception'
    /**
     * Strategie :
     *     - Recherche de tous les instructions '?store' et '?load'
     *  - Determiner le type de la viariable
     *  - Si la variable n'est pas encore definie, ajouter une entrée dans la
     *    Liste
     *  - Sinon, si le type est compatible
     */
    private static void analyzeMethodCode(
            ConstantPool constants,
            LocalVariables localVariables, List<Instruction> list,
            List<Instruction> listForAnalyze, String returnedSignature)
    {
        // Recherche des instructions d'ecriture des variables locales.
        int length = listForAnalyze.size();

        for (int i=0; i<length; i++)
        {
            Instruction instruction = listForAnalyze.get(i);

            if (instruction.getOpcode() == Const.ISTORE
             || instruction.getOpcode() == ByteCodeConstants.STORE
             || instruction.getOpcode() == Const.ASTORE
             || instruction.getOpcode() == Const.ILOAD
             || instruction.getOpcode() == ByteCodeConstants.LOAD
             || instruction.getOpcode() == Const.ALOAD
             || instruction.getOpcode() == Const.IINC) {
                subAnalyzeMethodCode(
                        constants, localVariables, listForAnalyze,
                        ((IndexInstruction)instruction).getIndex(), i,
                        returnedSignature);
            }
        }

        // Analyse inverse
        boolean change;

        Instruction instruction;
        do
        {
            change = false;

            for (int i=0; i<length; i++)
            {
                instruction = listForAnalyze.get(i);
                switch (instruction.getOpcode())
                {
                case Const.ISTORE:
                {
                    StoreInstruction si = (StoreInstruction)instruction;
                    if (si.getValueref().getOpcode() == Const.ILOAD)
                    {
                        // Contrainte du type de la variable liée à  ILoad par
                        // le type de la variable liée à  IStore.
                        change |= reverseAnalyzeIStore(localVariables, si);
                    }
                }
                break;
                case Const.PUTSTATIC:
                {
                    PutStatic ps = (PutStatic)instruction;
                    if (ps.getValueref().getOpcode() == Const.ILOAD || ps.getValueref().getOpcode() == Const.ALOAD) {
                        // Contrainte du type de la variable liée à  ILoad par
                        // le type de la variable liée à  PutStatic.
                        LoadInstruction load = (LoadInstruction)ps.getValueref();
                        change |= reverseAnalyzePutStaticPutField(
                                constants, localVariables, ps, load);
                    }
                }
                break;
                case Const.PUTFIELD:
                {
                    PutField pf = (PutField)instruction;
                    if (pf.getValueref().getOpcode() == Const.ILOAD || pf.getValueref().getOpcode() == Const.ALOAD) {
                        // Contrainte du type de la variable liée à  ILoad
                        // par le type de la variable liée à  PutField.
                        LoadInstruction load = (LoadInstruction)pf.getValueref();
                        change |= reverseAnalyzePutStaticPutField(
                                constants, localVariables, pf, load);
                    }
                }
                break;
                }
            }
        }
        while (change);

        // Selection d'un type pour les variables non encore typées.
        int internalObjectSignatureIndex =
                constants.addConstantUtf8(StringConstants.INTERNAL_OBJECT_SIGNATURE);

        length = localVariables.size();

        for (int i=0; i<length; i++)
        {
            LocalVariable lv = localVariables.getLocalVariableAt(i);

            switch (lv.getSignatureIndex())
            {
            case UNDEFINED_TYPE:
                lv.setSignatureIndex(constants.addConstantUtf8(
                        StringConstants.INTERNAL_OBJECT_SIGNATURE));
                break;
            case NUMBER_TYPE:
                lv.setSignatureIndex(constants.addConstantUtf8(
                        SignatureUtil.getSignatureFromTypesBitField(lv.getTypesBitField())));
                break;
            case OBJECT_TYPE:
                // Plusieurs types sont affectés à  la même variable. Le
                // decompilateur ne connait pas le graphe d'heritage des
                // classes decompilées. Le type de la variable est valué à 
                // 'Object'. Des instructions 'cast' supplémentaires doivent
                // être ajoutés. Voir la limitation de JAD sur ce point.
                lv.setSignatureIndex(internalObjectSignatureIndex);
                break;
            }
        }

        LocalVariable lv;
        // Ajout d'instructions "cast"
        for (int i=0; i<length; i++)
        {
            lv = localVariables.getLocalVariableAt(i);
            if (lv.getSignatureIndex() == internalObjectSignatureIndex) {
                addCastInstruction(constants, list, localVariables, lv);
            }
        }
    }

    /** Analyse du type de la variable locale No varIndex. */
    private static void subAnalyzeMethodCode(
            ConstantPool constants, LocalVariables localVariables,
            List<Instruction> listForAnalyze,
            int varIndex, int startIndex, String returnedSignature)
    {
        IndexInstruction firstInstruction =
                (IndexInstruction)listForAnalyze.get(startIndex);

        LocalVariable lv =
                localVariables.getLocalVariableWithIndexAndOffset(
                        firstInstruction.getIndex(), firstInstruction.getOffset());

        if (lv != null)
        {
            // Variable locale deja traitée

            // Verification que l'attribut 'exception' est correctement
            // positionné.
            if (firstInstruction.getOpcode() == Const.ASTORE)
            {
                AStore astore = (AStore)firstInstruction;
                if (astore.getValueref().getOpcode() == ByteCodeConstants.EXCEPTIONLOAD) {
                    lv.setExceptionOrReturnAddress(true);
                }
            }

            return;
        }

        final int length = listForAnalyze.size();

        Instruction instruction;
        // Recherche des instructions de lecture, d'ecriture et de comparaison
        // des variables locales.
        for (int i=startIndex; i<length; i++)
        {
            instruction = listForAnalyze.get(i);
            switch (instruction.getOpcode())
            {
            case Const.ISTORE:
                if (((IndexInstruction)instruction).getIndex() == varIndex) {
                    analyzeIStore(constants, localVariables, instruction);
                }
                break;
            case ByteCodeConstants.STORE:
                if (((IndexInstruction)instruction).getIndex() == varIndex) {
                    analyzeStore(constants, localVariables, instruction);
                }
                break;
            case Const.ASTORE:
                if (((IndexInstruction)instruction).getIndex() == varIndex) {
                    analyzeAStore(constants, localVariables, instruction);
                }
                break;
            case ByteCodeConstants.PREINC,
                 ByteCodeConstants.POSTINC:
                instruction = ((IncInstruction)instruction).getValue();
                if (instruction.getOpcode() != Const.ILOAD &&
                        instruction.getOpcode() != ByteCodeConstants.LOAD) {
                    break;
                }
                // intended fall through
            case Const.ILOAD,
                 Const.IINC:
                if (((IndexInstruction)instruction).getIndex() == varIndex) {
                    analyzeILoad(localVariables, instruction);
                }
                break;
            case ByteCodeConstants.LOAD,
                 ByteCodeConstants.EXCEPTIONLOAD:
                if (((IndexInstruction)instruction).getIndex() == varIndex) {
                    analyzeLoad(localVariables, instruction);
                }
                break;
            case Const.ALOAD:
                if (((IndexInstruction)instruction).getIndex() == varIndex) {
                    analyzeALoad(localVariables, instruction);
                }
                break;
            case Const.INVOKEINTERFACE,
                 Const.INVOKEVIRTUAL,
                 Const.INVOKESPECIAL,
                 Const.INVOKESTATIC:
                analyzeInvokeInstruction(
                        constants, localVariables, instruction, varIndex);
                break;
            case ByteCodeConstants.BINARYOP:
                BinaryOperatorInstruction boi =
                (BinaryOperatorInstruction)instruction;
                analyzeBinaryOperator(
                        constants, localVariables, instruction,
                        boi.getValue1(), boi.getValue2(), varIndex);
                break;
            case ByteCodeConstants.IFCMP:
                IfCmp ic = (IfCmp)instruction;
                analyzeBinaryOperator(
                        constants, localVariables, instruction,
                        ic.getValue1(), ic.getValue2(), varIndex);
                break;
            case ByteCodeConstants.XRETURN:
                analyzeReturnInstruction(
                        constants, localVariables, instruction,
                        varIndex, returnedSignature);
                break;
            }
        }
    }

    private static void analyzeIStore(
            ConstantPool constants, LocalVariables localVariables,
            Instruction instruction)
    {
        StoreInstruction store = (StoreInstruction)instruction;
        int index = store.getIndex();
        int offset = store.getOffset();

        LocalVariable lv =
                localVariables.searchLocalVariableWithIndexAndOffset(index, offset);
        String signature =
                store.getReturnedSignature(constants, localVariables);

        if (lv == null)
        {
            int typesBitField;

            if (signature == null)
            {
                if (store.getValueref().getOpcode() == Const.ILOAD)
                {
                    ILoad iload = (ILoad)store.getValueref();
                    lv = localVariables.getLocalVariableWithIndexAndOffset(
                            iload.getIndex(), iload.getOffset());
                    typesBitField = lv == null ?
                            ByteCodeConstants.TBF_INT_INT|ByteCodeConstants.TBF_INT_SHORT|
                            ByteCodeConstants.TBF_INT_BYTE|ByteCodeConstants.TBF_INT_CHAR|
                            ByteCodeConstants.TBF_INT_BOOLEAN:
                                lv.getTypesBitField();
                }
                else
                {
                    typesBitField =
                            ByteCodeConstants.TBF_INT_INT|ByteCodeConstants.TBF_INT_SHORT|
                            ByteCodeConstants.TBF_INT_BYTE|ByteCodeConstants.TBF_INT_CHAR|
                            ByteCodeConstants.TBF_INT_BOOLEAN;
                }
            }
            else
            {
                typesBitField = SignatureUtil.createTypesBitField(signature);
            }

            localVariables.add(new LocalVariable(
                    offset, 1, -1, NUMBER_TYPE, index, typesBitField));
        } else if (signature == null)
        {
            lv.updateRange(offset);
        }
        else
        {
            // Une variable est trouvée. Le type est il compatible ?
            int typesBitField =
                    SignatureUtil.createTypesBitField(signature);
            switch (lv.getSignatureIndex())
            {
            case NUMBER_TYPE:
                if ((typesBitField & lv.getTypesBitField()) != 0)
                {
                    // Reduction de champ de bits
                    lv.setTypesBitField(lv.getTypesBitField() & typesBitField);
                    lv.updateRange(offset);
                }
                else
                {
                    // Type incompatible => creation de variables
                    localVariables.add(new LocalVariable(
                            offset, 1, -1, NUMBER_TYPE, index, typesBitField));
                }
                break;
            case UNDEFINED_TYPE,
                 OBJECT_TYPE:
                // Type incompatible => creation de variables
                localVariables.add(new LocalVariable(
                        offset, 1, -1, NUMBER_TYPE, index, typesBitField));
                break;
            default:
                String signatureLV =
                constants.getConstantUtf8(lv.getSignatureIndex());
                int typesBitFieldLV =
                        SignatureUtil.createTypesBitField(signatureLV);

                if ((typesBitField & typesBitFieldLV) != 0)
                {
                    lv.updateRange(offset);
                }
                else
                {
                    // Type incompatible => creation de variables
                    localVariables.add(new LocalVariable(
                            offset, 1, -1, NUMBER_TYPE, index, typesBitField));
                }
            }
        }
    }

    private static void analyzeILoad(
            LocalVariables localVariables, Instruction instruction)
    {
        IndexInstruction load = (IndexInstruction)instruction;
        int index = load.getIndex();
        int offset = load.getOffset();

        LocalVariable lv =
                localVariables.searchLocalVariableWithIndexAndOffset(index, offset);

        if (lv == null)
        {
            // La premiere instruction utilisant ce slot est de type 'Load'.
            // Impossible de determiner le type d'entier pour le moment.
            localVariables.add(new LocalVariable(
                    offset, 1, -1, NUMBER_TYPE, index,
                    ByteCodeConstants.TBF_INT_INT|ByteCodeConstants.TBF_INT_SHORT|
                    ByteCodeConstants.TBF_INT_BYTE|ByteCodeConstants.TBF_INT_CHAR|
                    ByteCodeConstants.TBF_INT_BOOLEAN));
        }
        else
        {
            lv.updateRange(offset);
        }
    }

    private static void analyzeLoad(
            LocalVariables localVariables, Instruction instruction)
    {
        IndexInstruction load = (IndexInstruction)instruction;
        int index = load.getIndex();
        int offset = load.getOffset();

        LocalVariable lv =
                localVariables.searchLocalVariableWithIndexAndOffset(index, offset);

        if (lv == null)
        {
            localVariables.add(new LocalVariable(
                    offset, 1, -1, -1, index));
        }
        else
        {
            lv.updateRange(offset);
        }
    }

    private static void analyzeALoad(
            LocalVariables localVariables, Instruction instruction)
    {
        IndexInstruction load = (IndexInstruction)instruction;
        int index = load.getIndex();
        int offset = load.getOffset();

        LocalVariable lv =
                localVariables.searchLocalVariableWithIndexAndOffset(index, offset);

        if (lv == null)
        {
            localVariables.add(new LocalVariable(
                    offset, 1, -1, UNDEFINED_TYPE, index));
        }
        else
        {
            lv.updateRange(offset);
        }
    }

    private static void analyzeInvokeInstruction(
            ConstantPool constants, LocalVariables localVariables,
            Instruction instruction, int varIndex)
    {
        final InvokeInstruction invokeInstruction =
                (InvokeInstruction)instruction;
        final List<Instruction> args = invokeInstruction.getArgs();
        final List<String> argSignatures =
                invokeInstruction.getListOfParameterSignatures(constants);
        final int nbrOfArgs = args.size();

        for (int j=0; j<nbrOfArgs; j++)
        {
            analyzeArgOrReturnedInstruction(
                    constants, localVariables, args.get(j),
                    varIndex, argSignatures.get(j));
        }
    }

    private static void analyzeArgOrReturnedInstruction(
            ConstantPool constants, LocalVariables localVariables,
            Instruction instruction, int varIndex, String signature)
    {
        LoadInstruction li;
        if (instruction.getOpcode() == Const.ILOAD) {
            li = (LoadInstruction)instruction;
            if (li.getIndex() == varIndex)
            {
                LocalVariable lv =
                        localVariables.searchLocalVariableWithIndexAndOffset(li.getIndex(), li.getOffset());
                if (lv != null) {
                    lv.setTypesBitField(lv.getTypesBitField() & SignatureUtil.createArgOrReturnBitFields(signature));
                }
            }
        } else if (instruction.getOpcode() == Const.ALOAD) {
            li = (LoadInstruction)instruction;
            if (li.getIndex() == varIndex)
            {
                LocalVariable lv =
                        localVariables.searchLocalVariableWithIndexAndOffset(
                                li.getIndex(), li.getOffset());
                if (lv != null)
                {
                    if (lv.getSignatureIndex() == UNDEFINED_TYPE) {
                        lv.setSignatureIndex(constants.addConstantUtf8(signature));
                    } else if (lv.getSignatureIndex() == NUMBER_TYPE) {
                        new Throwable("type inattendu").printStackTrace();
                        // NE PAS GENERER DE CONFLIT DE TYPE LORSQUE LE TYPE
                        // D'UNE VARIABLE EST DIFFERENT DU TYPE D'UN PARAMETRE.
                        /* case OBJECT_TYPE:
                            break;
                        default:
                            String signature =
                                constants.getConstantUtf8(lv.signatureIndex);
                            String argSignature = argSignatures.get(j);

                            if (!argSignature.equals(signature) &&
                                !argSignature.equals(
                                    Constants.INTERNAL_OBJECT_SIGNATURE))
                            {
                                // La signature du parametre ne correspond pas
                                // a la signature de l'objet passé en parametre
                                lv.signatureIndex = OBJECT_TYPE;
                            }*/
                    }
                }
            }
        }
    }

    /** Reduction de l'ensemble des types entiers. */
    private static void analyzeBinaryOperator(
            ConstantPool constants, LocalVariables localVariables,
            Instruction instruction, Instruction i1, Instruction i2,
            int varIndex)
    {
        if (
                (i1.getOpcode() != Const.ILOAD || ((ILoad)i1).getIndex() != varIndex) &&
                (i2.getOpcode() != Const.ILOAD || ((ILoad)i2).getIndex() != varIndex)
                ) {
            return;
        }

        LocalVariable lv1 = i1.getOpcode() == Const.ILOAD ?
                localVariables.searchLocalVariableWithIndexAndOffset(
                        ((ILoad)i1).getIndex(), i1.getOffset()) : null;

        LocalVariable lv2 = i2.getOpcode() == Const.ILOAD ?
                localVariables.searchLocalVariableWithIndexAndOffset(
                        ((ILoad)i2).getIndex(), i2.getOffset()) : null;

        if (lv1 != null)
        {
            lv1.updateRange(instruction.getOffset());
            if (lv2 != null) {
                lv2.updateRange(instruction.getOffset());
            }

            if (lv1.getSignatureIndex() == NUMBER_TYPE)
            {
                // Reduction des types de lv1
                if (lv2 != null)
                {
                    if (lv2.getSignatureIndex() == NUMBER_TYPE)
                    {
                        // Reduction des types de lv1 & lv2
                        lv1.setTypesBitField(lv1.getTypesBitField() & lv2.getTypesBitField());
                        lv2.setTypesBitField(lv2.getTypesBitField() & lv1.getTypesBitField());
                    }
                    else
                    {
                        lv1.setSignatureIndex(lv2.getSignatureIndex());
                    }
                }
                else
                {
                    String signature =
                            i2.getReturnedSignature(constants, localVariables);

                    if (SignatureUtil.isIntegerSignature(signature))
                    {
                        int type = SignatureUtil.createTypesBitField(signature);
                        if (type != 0) {
                            lv1.setTypesBitField(lv1.getTypesBitField() & type);
                        }
                    }
                }
            }
            else if (lv2 != null && lv2.getSignatureIndex() == NUMBER_TYPE)
            {
                // Reduction des types de lv2
                lv2.setSignatureIndex(lv1.getSignatureIndex());
            }
        }
        else if (lv2 != null)
        {
            lv2.updateRange(instruction.getOffset());

            if (lv2.getSignatureIndex() == NUMBER_TYPE)
            {
                // Reduction des types de lv2
                String signature =
                        i1.getReturnedSignature(constants, localVariables);

                if (SignatureUtil.isIntegerSignature(signature))
                {
                    int type = SignatureUtil.createTypesBitField(signature);
                    if (type != 0) {
                        lv2.setTypesBitField(lv2.getTypesBitField() & type);
                    }
                }
            }
        }
    }

    private static void analyzeReturnInstruction(
            ConstantPool constants, LocalVariables localVariables,
            Instruction instruction, int varIndex, String returnedSignature)
    {
        ReturnInstruction ri = (ReturnInstruction)instruction;
        analyzeArgOrReturnedInstruction(
                constants, localVariables, ri.getValueref(),
                varIndex, returnedSignature);
    }

    private static void analyzeStore(
            ConstantPool constants, LocalVariables localVariables,
            Instruction instruction)
    {
        StoreInstruction store = (StoreInstruction)instruction;
        int index = store.getIndex();
        int offset = store.getOffset();

        LocalVariable lv =
                localVariables.searchLocalVariableWithIndexAndOffset(index, offset);
        String signature =
                instruction.getReturnedSignature(constants, localVariables);
        int signatureIndex =
                signature != null ? constants.addConstantUtf8(signature) : -1;

        if (lv == null || lv.getSignatureIndex() != signatureIndex)
        {
            // variable non trouvée ou type incompatible => création de variable
            localVariables.add(new LocalVariable(
                    offset, 1, -1, signatureIndex, index));
        } else {
            lv.updateRange(offset);
        }
    }

    private static void analyzeAStore(
            ConstantPool constants, LocalVariables localVariables,
            Instruction instruction)
    {
        StoreInstruction store = (StoreInstruction)instruction;
        int index = store.getIndex();
        int offset = store.getOffset();

        LocalVariable lv =
                localVariables.searchLocalVariableWithIndexAndOffset(index, offset);
        String signatureInstruction =
                instruction.getReturnedSignature(constants, localVariables);
        int signatureInstructionIndex = signatureInstruction != null ?
                constants.addConstantUtf8(signatureInstruction) : UNDEFINED_TYPE;
        boolean isExceptionOrReturnAddress =
                store.getValueref().getOpcode() == ByteCodeConstants.EXCEPTIONLOAD ||
                store.getValueref().getOpcode() == ByteCodeConstants.RETURNADDRESSLOAD;

        if (lv == null || lv.isExceptionOrReturnAddress() ||
                isExceptionOrReturnAddress && lv.getStartPc() + lv.getLength() < offset)
        {
            localVariables.add(new LocalVariable(
                    offset, 1, -1, signatureInstructionIndex, index,
                    isExceptionOrReturnAddress));
        }
        else if (!isExceptionOrReturnAddress)
        {
            // Une variable est trouvée. Le type est il compatible ?
            if (lv.getSignatureIndex() == UNDEFINED_TYPE)
            {
                // Cas particulier Jikes 1.2.2 bloc finally :
                //  Une instruction ALoad apparait avant AStore
                lv.setSignatureIndex(signatureInstructionIndex);
                lv.updateRange(offset);
            }
            else if (lv.getSignatureIndex() == NUMBER_TYPE)
            {
                // Creation de variables
                localVariables.add(new LocalVariable(
                        offset, 1, -1, signatureInstructionIndex, index));
            }
            else if (lv.getSignatureIndex() == signatureInstructionIndex ||
                    lv.getSignatureIndex() == OBJECT_TYPE)
            {
                lv.updateRange(offset);
            }
            else
            {
                // Type incompatible => 2 cas :
                // 1) si une signature est de type 'Object' et la seconde est
                //    un type primitif, creation d'une nouvelle variable.
                // 2) si les deux signatures sont de type 'Object',
                //    modification du type de la variable en 'Object' puis
                //    ajout d'instruction cast.
                String signatureLV =
                        constants.getConstantUtf8(lv.getSignatureIndex());

                if (SignatureUtil.isPrimitiveSignature(signatureLV))
                {
                    // Creation de variables
                    localVariables.add(new LocalVariable(
                            offset, 1, -1, signatureInstructionIndex, index));
                } else {
                    if (signatureInstructionIndex != UNDEFINED_TYPE)
                    {
                        // Modification du type de variable
                        lv.setSignatureIndex(OBJECT_TYPE);
                    }
                    lv.updateRange(offset);
                }
            }
        }
    }

    /**
     * Substitution des types byte par char dans les instructions
     * bipush, sipush et iconst suivants les instructions istore et invoke.
     */
    private static void setConstantTypes(
            ConstantPool constants,
            LocalVariables localVariables, List<Instruction> list,
            List<Instruction> listForAnalyze, String returnedSignature)
    {
        final int length = listForAnalyze.size();

        // Affection du type des constantes depuis les instructions mères
        for (int i=0; i<length; i++)
        {
            final Instruction instruction = listForAnalyze.get(i);

            switch (instruction.getOpcode())
            {
            case ByteCodeConstants.ARRAYSTORE:
                setConstantTypesArrayStore(
                        constants, localVariables,
                        (ArrayStoreInstruction)instruction);
                break;
            case ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi =
                        (BinaryOperatorInstruction)instruction;
                setConstantTypesBinaryOperator(
                        constants, localVariables, boi.getValue1(), boi.getValue2());
            }
            break;
            case ByteCodeConstants.IFCMP:
            {
                IfCmp ic = (IfCmp)instruction;
                setConstantTypesBinaryOperator(
                        constants, localVariables, ic.getValue1(), ic.getValue2());
            }
            break;
            case Const.INVOKEINTERFACE,
                 Const.INVOKEVIRTUAL,
                 Const.INVOKESPECIAL,
                 Const.INVOKESTATIC,
                 ByteCodeConstants.INVOKENEW:
                setConstantTypesInvokeInstruction(constants, instruction);
                break;
            case Const.ISTORE:
                setConstantTypesIStore(constants, localVariables, instruction);
                break;
            case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                setConstantTypesPutFieldAndPutStatic(
                        constants, putField.getValueref(), putField.getIndex());
            }
            break;
            case Const.PUTSTATIC:
            {
                PutStatic putStatic = (PutStatic)instruction;
                setConstantTypesPutFieldAndPutStatic(
                        constants, putStatic.getValueref(), putStatic.getIndex());
            }
            break;
            case ByteCodeConstants.XRETURN:
            {
                setConstantTypesXReturn(instruction, returnedSignature);
            }
            break;
            }
        }

        Instruction instruction;
        // Determination des types des constantes apparaissant dans les
        // instructions 'TernaryOpStore'.
        for (int i=0; i<length; i++)
        {
            instruction = listForAnalyze.get(i);

            if (instruction.getOpcode() == ByteCodeConstants.TERNARYOPSTORE)
            {
                TernaryOpStore tos = (TernaryOpStore)instruction;
                setConstantTypesTernaryOpStore(
                        constants, localVariables, list, tos);
            }
        }
    }

    private static void setConstantTypesInvokeInstruction(
            ConstantPool constants,
            Instruction instruction)
    {
        final InvokeInstruction invokeInstruction =
                (InvokeInstruction)instruction;
        final List<Instruction> args = invokeInstruction.getArgs();
        final List<String> types =
                invokeInstruction.getListOfParameterSignatures(constants);
        final int nbrOfArgs = args.size();

        Instruction arg;
        for (int j=0; j<nbrOfArgs; j++)
        {
            arg = args.get(j);

            if (arg.getOpcode() == Const.BIPUSH || arg.getOpcode() == ByteCodeConstants.ICONST
                    || arg.getOpcode() == Const.SIPUSH) {
                ((IConst)arg).setReturnedSignature(types.get(j));
            }
        }
    }

    private static void setConstantTypesPutFieldAndPutStatic(
            ConstantPool constants, Instruction valueref, int index)
    {
        if (valueref.getOpcode() == Const.BIPUSH || valueref.getOpcode() == ByteCodeConstants.ICONST
                || valueref.getOpcode() == Const.SIPUSH) {
            ConstantFieldref cfr = constants.getConstantFieldref(index);
            ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
            String signature = constants.getConstantUtf8(cnat.getSignatureIndex());
            ((IConst)valueref).setReturnedSignature(signature);
        }
    }

    private static void setConstantTypesTernaryOpStore(
            ConstantPool constants, LocalVariables localVariables,
            List<Instruction> list, TernaryOpStore tos)
    {
        if (tos.getObjectref().getOpcode() == Const.BIPUSH || tos.getObjectref().getOpcode() == ByteCodeConstants.ICONST
                || tos.getObjectref().getOpcode() == Const.SIPUSH) {
            // Recherche de la seconde valeur de l'instruction ternaire
            int index = InstructionUtil.getIndexForOffset(
                    list, tos.getTernaryOp2ndValueOffset());

            if (index != -1)
            {
                int length = list.size();

                Instruction result;
                while (index < length)
                {
                    result = SearchInstructionByOffsetVisitor.visit(
                            list.get(index), tos.getTernaryOp2ndValueOffset());

                    if (result != null)
                    {
                        String signature =
                                result.getReturnedSignature(constants, localVariables);
                        ((IConst)tos.getObjectref()).setReturnedSignature(signature);
                        break;
                    }

                    index++;
                }
            }
        }
    }

    private static void setConstantTypesArrayStore(
            ConstantPool constants,
            LocalVariables localVariables,
            ArrayStoreInstruction asi)
    {
        if (ByteCodeUtil.isLoadIntValue(asi.getValueref().getOpcode())) {
            int asiArrayRefOpCode = asi.getArrayref().getOpcode();
            if (asiArrayRefOpCode == Const.ALOAD)
            {
                ALoad aload = (ALoad)asi.getArrayref();
                LocalVariable lv = localVariables.getLocalVariableWithIndexAndOffset(
                        aload.getIndex(), aload.getOffset());

                if (lv == null)
                {
                    new Throwable("lv is null. index=" + aload.getIndex()).printStackTrace();
                    return;
                }

                String signature =
                        constants.getConstantUtf8(lv.getSignatureIndex());
                ((IConst)asi.getValueref()).setReturnedSignature(
                        SignatureUtil.cutArrayDimensionPrefix(signature));
            }
            else if (asiArrayRefOpCode == Const.GETFIELD
                  || asiArrayRefOpCode == Const.GETSTATIC)
            {
                IndexInstruction ii = (IndexInstruction)asi.getArrayref();
                ConstantFieldref cfr = constants.getConstantFieldref(ii.getIndex());
                ConstantNameAndType cnat =
                        constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
                String signature =
                        constants.getConstantUtf8(cnat.getSignatureIndex());
                ((IConst)asi.getValueref()).setReturnedSignature(
                        SignatureUtil.cutArrayDimensionPrefix(signature));
            }
        }
    }

    private static void setConstantTypesIStore(
            ConstantPool constants,
            LocalVariables localVariables,
            Instruction instruction)
    {
        StoreInstruction store = (StoreInstruction)instruction;

        if (store.getValueref().getOpcode() == Const.BIPUSH
         || store.getValueref().getOpcode() == ByteCodeConstants.ICONST
         || store.getValueref().getOpcode() == Const.SIPUSH) {
            final LocalVariable lv =
                    localVariables.getLocalVariableWithIndexAndOffset(
                            store.getIndex(), store.getOffset());
            String signature = constants.getConstantUtf8(lv.getSignatureIndex());
            ((IConst)store.getValueref()).setReturnedSignature(signature);
        }
    }

    private static void setConstantTypesBinaryOperator(
            ConstantPool constants,
            LocalVariables localVariables,
            Instruction i1, Instruction i2)
    {
        if (i1.getOpcode() == Const.BIPUSH
         || i1.getOpcode() == ByteCodeConstants.ICONST
         || i1.getOpcode() == Const.SIPUSH) {
            if (i2.getOpcode() != Const.BIPUSH
             && i2.getOpcode() != ByteCodeConstants.ICONST
             && i2.getOpcode() != Const.SIPUSH) {
                String signature = i2.getReturnedSignature(
                        constants, localVariables);
                if (signature != null) {
                    ((IConst)i1).setReturnedSignature(signature);
                }
            }
        } else if (i2.getOpcode() == Const.BIPUSH
                || i2.getOpcode() == ByteCodeConstants.ICONST
                || i2.getOpcode() == Const.SIPUSH) {
            String signature = i1.getReturnedSignature(
                    constants, localVariables);
            if (signature != null) {
                ((IConst)i2).setReturnedSignature(signature);
            }
        }
    }

    private static void setConstantTypesXReturn(
            Instruction instruction, String returnedSignature)
    {
        ReturnInstruction ri = (ReturnInstruction)instruction;

        int opcode = ri.getValueref().getOpcode();

        if (opcode != Const.SIPUSH &&
            opcode != Const.BIPUSH &&
            opcode != ByteCodeConstants.ICONST) {
            return;
        }

        ((IConst)ri.getValueref()).setSignature(returnedSignature);
    }

    private static String getReturnedSignature(
            ClassFile classFile, Method method)
    {
        AttributeSignature as = method.getAttributeSignature();
        int signatureIndex = as == null ?
                method.getDescriptorIndex() : as.getSignatureIndex();
        String signature =
                classFile.getConstantPool().getConstantUtf8(signatureIndex);

        return SignatureUtil.getMethodReturnedSignature(signature);
    }

    private static void initialyzeExceptionLoad(
            List<Instruction> listForAnalyze, LocalVariables localVariables)
    {
        int length = listForAnalyze.size();

        /*
         * Methode d'initialisation des instructions ExceptionLoad non
         * initialisées. Cela se produit lorsque les méthodes possèdent un bloc
         * de definition de variables locales.
         * Les instructions ExceptionLoad appartenant aux blocs 'finally' ne
         * sont pas initialisée.
         */
        for (int index=0; index<length; index++)
        {
            Instruction i = listForAnalyze.get(index);

            if (i.getOpcode() == Const.ASTORE)
            {
                AStore as = (AStore)i;

                if (as.getValueref().getOpcode() == ByteCodeConstants.EXCEPTIONLOAD)
                {
                    ExceptionLoad el = (ExceptionLoad)as.getValueref();
                    if (el.getIndex() == UtilConstants.INVALID_INDEX) {
                        el.setIndex(as.getIndex());
                    }
                }
            }
        }

        Instruction i;
        /*
         * Lorsque les exceptions ne sont pas utilisées dans le block 'catch',
         * aucune variable locale n'est créée. Une pseudo variable locale est
         * alors créée pour afficher correctement l'instruction
         * "catch (Exception localException)".
         * Aucun ajout d'instruction si "ExceptionLoad" correspond à  une
         * instruction "finally".
         */
        for (int index=0; index<length; index++)
        {
            i = listForAnalyze.get(index);

            if (i.getOpcode() == ByteCodeConstants.EXCEPTIONLOAD)
            {
                ExceptionLoad el = (ExceptionLoad)i;

                if (el.getIndex() == UtilConstants.INVALID_INDEX &&
                        el.getExceptionNameIndex() > 0)
                {
                    int varIndex = localVariables.size();
                    LocalVariable localVariable = new LocalVariable(
                            el.getOffset(), 1, UtilConstants.INVALID_INDEX,
                            el.getExceptionNameIndex(), varIndex, true);
                    localVariables.add(localVariable);
                    el.setIndex(varIndex);
                }
            }
        }
    }

    private static void generateLocalVariableNames(
            ConstantPool constants,
            LocalVariables localVariables,
            DefaultVariableNameGenerator variableNameGenerator)
    {
        final int length = localVariables.size();

        for (int i=localVariables.getIndexOfFirstLocalVariable(); i<length; i++)
        {
            final LocalVariable lv = localVariables.getLocalVariableAt(i);

            if (lv != null && lv.getNameIndex() <= 0)
            {
                String signature = constants.getConstantUtf8(lv.getSignatureIndex());
                boolean appearsOnce = signatureAppearsOnceInLocalVariables(
                        localVariables, length, lv.getSignatureIndex());
                String name =
                        variableNameGenerator.generateLocalVariableNameFromSignature(
                                signature, appearsOnce);
                lv.setNameIndex(constants.addConstantUtf8(name));
            }
        }
    }

    private static boolean signatureAppearsOnceInParameters(
            List<String> parameterTypes, int firstIndex,
            int length, String signature)
    {
        int counter = 0;

        for (int i=firstIndex; i<length && counter<2; i++) {
            if (signature.equals(parameterTypes.get(i))) {
                counter++;
            }
        }

        return counter <= 1;
    }

    private static boolean signatureAppearsOnceInLocalVariables(
            LocalVariables localVariables,
            int length, int signatureIndex)
    {
        int counter = 0;

        for (int i=localVariables.getIndexOfFirstLocalVariable();
                i<length && counter<2; i++)
        {
            final LocalVariable lv = localVariables.getLocalVariableAt(i);
            if (lv != null && lv.getSignatureIndex() == signatureIndex) {
                counter++;
            }
        }

        return counter == 1;
    }

    private static boolean reverseAnalyzeIStore(
            LocalVariables localVariables, StoreInstruction si)
    {
        LoadInstruction load = (LoadInstruction)si.getValueref();
        LocalVariable lvLoad =
                localVariables.getLocalVariableWithIndexAndOffset(
                        load.getIndex(), load.getOffset());

        if (lvLoad == null || lvLoad.getSignatureIndex() != NUMBER_TYPE) {
            return false;
        }

        LocalVariable lvStore =
                localVariables.getLocalVariableWithIndexAndOffset(
                        si.getIndex(), si.getOffset());

        if (lvStore == null) {
            return false;
        }

        if (lvStore.getSignatureIndex() == NUMBER_TYPE)
        {
            int old = lvLoad.getTypesBitField();
            lvLoad.setTypesBitField(lvLoad.getTypesBitField() & lvStore.getTypesBitField());
            return old != lvLoad.getTypesBitField();
        }
        if (lvStore.getSignatureIndex() >= 0 &&
                lvStore.getSignatureIndex() != lvLoad.getSignatureIndex())
        {
            lvLoad.setSignatureIndex(lvStore.getSignatureIndex());
            return true;
        }

        return false;
    }

    private static boolean reverseAnalyzePutStaticPutField(
            ConstantPool constants, LocalVariables localVariables,
            IndexInstruction ii, LoadInstruction load)
    {
        LocalVariable lvLoad =
                localVariables.getLocalVariableWithIndexAndOffset(
                        load.getIndex(), load.getOffset());

        if (lvLoad != null)
        {
            ConstantFieldref cfr = constants.getConstantFieldref(ii.getIndex());
            ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

            if (lvLoad.getSignatureIndex() == NUMBER_TYPE)
            {
                String descriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
                int typesBitField = SignatureUtil.createArgOrReturnBitFields(descriptor);
                int old = lvLoad.getTypesBitField();
                lvLoad.setTypesBitField(lvLoad.getTypesBitField() & typesBitField);
                return old != lvLoad.getTypesBitField();
            }
            if (lvLoad.getSignatureIndex() == UNDEFINED_TYPE)
            {
                lvLoad.setSignatureIndex(cnat.getSignatureIndex());
                return true;
            }
        }

        return false;
    }

    private static void addCastInstruction(
            ConstantPool constants, List<Instruction> list,
            LocalVariables localVariables, LocalVariable lv)
    {
        // Add cast instruction before all 'ALoad' instruction for local
        // variable le used type is not 'Object'.
        AddCheckCastVisitor visitor = new AddCheckCastVisitor(
                constants, localVariables, lv);

        final int length = list.size();

        for (int i=0; i<length; i++) {
            visitor.visit(list.get(i));
        }
    }
}
