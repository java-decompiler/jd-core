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

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantNameAndType;
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
import jd.core.model.instruction.fast.FastConstants;
import jd.core.process.analyzer.classfile.visitor.AddCheckCastVisitor;
import jd.core.process.analyzer.classfile.visitor.SearchInstructionByOffsetVisitor;
import jd.core.process.analyzer.instruction.bytecode.util.ByteCodeUtil;
import jd.core.process.analyzer.util.InstructionUtil;
import jd.core.process.analyzer.variable.DefaultVariableNameGenerator;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;
import jd.core.util.UtilConstants;


public class LocalVariableAnalyzer 
{	
	/**
	 * Indexe de signature pour les variables locales de type inconnu. Si le 
	 * type de la variable n'a pu etre determin�, la variable sera type 
	 * 'Object'.
	 */
	private static final int UNDEFINED_TYPE = -1;
	/**
	 * Indexe de signature pour les variables locales de type num�rique inconnu.
	 * Si le type de la variable n'a pu etre determin�, la variable sera type 
	 * 'int'.
	 */
	private static final int NUMBER_TYPE = -2;	
	/**
	 * Indexe de signature pour les variables locales de type 'Object' et 
	 * n�c�ssitant l'insertion d'instructions 'cast'. 
	 */
	private static final int OBJECT_TYPE = -3;	

	public static void Analyze(
			ClassFile classFile, Method method, 
			DefaultVariableNameGenerator variableNameGenerator,
			List<Instruction> list, List<Instruction> listForAnalyze)
	{
		ConstantPool constants = classFile.getConstantPool();
		variableNameGenerator.clearLocalNames();
				
		// DEBUG String debugClassName = classFile.getInternalClassName();
		// DEBUG String debugMethodName = constants.getConstantUtf8(method.name_index);
		
		// Reconstruction de la Liste des variables locales
		byte[] code = method.getCode();
		int codeLength = (code == null) ? 0 : code.length;			
		LocalVariables localVariables = method.getLocalVariables();
				
		if (localVariables == null)
		{
			// Ajout d'entr�es dans le tableau pour les parametres
			localVariables = new LocalVariables();			
			method.setLocalVariables(localVariables);

			// Add this
			if ((method.access_flags & ClassFileConstants.ACC_STATIC) == 0)
			{
				int nameIndex = constants.addConstantUtf8(
					StringConstants.THIS_LOCAL_VARIABLE_NAME);
				int signatureIndex = 
					constants.addConstantUtf8(classFile.getInternalClassName());
				LocalVariable lv = 
					new LocalVariable(0, codeLength, nameIndex, signatureIndex, 0);
				localVariables.add(lv);
			}
			
			if ((method.name_index == constants.instanceConstructorIndex) && 
				classFile.isAInnerClass() && 
				((classFile.access_flags & ClassFileConstants.ACC_STATIC) == 0))
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
			AnalyzeMethodParameter(
				classFile, constants, method, localVariables, 
				variableNameGenerator, codeLength);
			
			localVariables.setIndexOfFirstLocalVariable(localVariables.size());
			
			if (code != null)
			{
				GenerateMissingMonitorLocalVariables(
					constants, localVariables, listForAnalyze);
			}
		}
		else
		{
			// Traitement des entr�es correspondant aux parametres
			AttributeSignature as = method.getAttributeSignature();
			String methodSignature = constants.getConstantUtf8(
					(as==null) ? method.descriptor_index : as.signature_index);
			
			int indexOfFirstLocalVariable = 
				(((method.access_flags & ClassFileConstants.ACC_STATIC) == 0) ? 1 : 0) +				
				SignatureUtil.GetParameterSignatureCount(methodSignature);		

			if (indexOfFirstLocalVariable > localVariables.size())
			{
				// Dans le cas des m�thodes g�n�r�e automatiquement par le 
				// compilateur (comme par exemple les m�thode des enums), le 
				// tableau des variables locales est incomplet.
				// Add Parameters
				AnalyzeMethodParameter(
					classFile, constants, method, localVariables, 
					variableNameGenerator, codeLength);
			}
			
			localVariables.setIndexOfFirstLocalVariable(
					indexOfFirstLocalVariable);
			
			if (code != null)
			{
				GenerateMissingMonitorLocalVariables(
					constants, localVariables, listForAnalyze);
				CheckLocalVariableRanges(
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
			String returnedSignature = GetReturnedSignature(classFile, method);			

			AnalyzeMethodCode(
				constants, localVariables, list, listForAnalyze, 
				returnedSignature);
			
			  // Upgrade byte type to char type
			  // Substitution des types byte par char dans les instructions 
			  // bipush et sipush
			SetConstantTypes(
				classFile, constants, method, localVariables, 
				list, listForAnalyze, returnedSignature);
			
			InitialyzeExceptionLoad(listForAnalyze, localVariables);
		}
		
		GenerateLocalVariableNames(
				constants, localVariables, variableNameGenerator);
	}
	
	private static void AnalyzeMethodParameter(
			ClassFile classFile, ConstantPool constants, 
			Method method, LocalVariables localVariables, 
			DefaultVariableNameGenerator variableNameGenerator, int codeLength)
	{
		// Le descripteur et la signature sont differentes pour les 
		// constructeurs des Enums !
		AttributeSignature as = method.getAttributeSignature();
		boolean descriptorFlag = (as == null);
		String methodSignature = constants.getConstantUtf8(
			descriptorFlag ? method.descriptor_index : as.signature_index);
		List<String> parameterTypes = 
			SignatureUtil.GetParameterSignatures(methodSignature);
		
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
			// Le premier parametre des m�thodes non statiques est 'this'
			boolean staticMethodFlag = 
				((method.access_flags & ClassFileConstants.ACC_STATIC) != 0);
			int variableIndex = staticMethodFlag ? 0 : 1;

			int firstVisibleParameterCounter = 0;	

			if (method.name_index == constants.instanceConstructorIndex)
			{
				if ((classFile.access_flags & ClassFileConstants.ACC_ENUM) != 0)
				{
					if (descriptorFlag)
						firstVisibleParameterCounter = 2;
					else
						variableIndex = 3;
				}
				else if (classFile.isAInnerClass())
				{
					if ((classFile.access_flags & ClassFileConstants.ACC_STATIC) == 0)
						firstVisibleParameterCounter = 1;
				}
			}
			
			//
			int anonymousClassDepth = 0;
			ClassFile anonymousClassFile = classFile;
			
			while ((anonymousClassFile != null) &&
				   (anonymousClassFile.getInternalAnonymousClassName() != null))
			{
				anonymousClassDepth++;
				anonymousClassFile = anonymousClassFile.getOuterClass();
			}
				
			//
			final int length = parameterTypes.size();
			
			//
			final int varargsParameterIndex;
			
			if ((method.access_flags & ClassFileConstants.ACC_VARARGS) == 0)
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

				if (/*(parameterIndex >= firstVisibleParameterCounter) &&*/
					(localVariables.getLocalVariableWithIndexAndOffset(variableIndex, 0) == null))
				{
					boolean appearsOnceFlag = SignatureAppearsOnceInParameters(
						parameterTypes, firstVisibleParameterCounter, 
						length, signature);
					final String name = 
						variableNameGenerator.generateParameterNameFromSignature(
								signature, appearsOnceFlag, 
								(parameterIndex==varargsParameterIndex),
								anonymousClassDepth);
					
					int nameIndex = constants.addConstantUtf8(name);
					int signatureIndex = constants.addConstantUtf8(signature);
					LocalVariable lv = new LocalVariable(
						0, codeLength, nameIndex, signatureIndex, variableIndex);
					localVariables.add(lv);
				}
				
				final char firstChar = signature.charAt(0);
				variableIndex += 
					((firstChar == 'D') || (firstChar == 'J')) ? 2 : 1;	
			}
		}
	}
	
	private static void GenerateMissingMonitorLocalVariables(
			ConstantPool constants, LocalVariables localVariables, 
			List<Instruction> listForAnalyze)
	{
		int length = listForAnalyze.size();
		
		for (int i=1; i<length; i++)
		{
			Instruction instruction = listForAnalyze.get(i);
		
			if (instruction.opcode != ByteCodeConstants.MONITORENTER)
				continue;
			
			MonitorEnter mEnter = (MonitorEnter)instruction;
			int monitorLocalVariableIndex = 0;
			int monitorLocalVariableOffset = 0;
			int monitorLocalVariableLenght = 1;
			
			if (mEnter.objectref.opcode == ByteCodeConstants.DUPLOAD)
			{
				/* DupStore( ? )
				 * AStore( DupLoad )
				 * MonitorEnter( DupLoad )
				 */
				instruction = listForAnalyze.get(i-1);
				if (instruction.opcode != ByteCodeConstants.ASTORE)
					continue;
				AStore astore = (AStore)instruction;
				if (astore.valueref.opcode != ByteCodeConstants.DUPLOAD)
					continue;
				DupLoad dupload1 = (DupLoad)mEnter.objectref;
				DupLoad dupload2 = (DupLoad)astore.valueref;
				if (dupload1.dupStore != dupload2.dupStore)
					continue;
				monitorLocalVariableIndex = astore.index;
				monitorLocalVariableOffset = astore.offset;
			}
			else if (mEnter.objectref.opcode == ByteCodeConstants.ALOAD)
			{
				/* AStore( ? )
				 * MonitorEnter( ALoad )
				 */
				ALoad aload = (ALoad)mEnter.objectref;
				instruction = listForAnalyze.get(i-1);
				if (instruction.opcode != ByteCodeConstants.ASTORE)
					continue;
				AStore astore = (AStore)instruction;
				if (astore.index != aload.index)
					continue;
				monitorLocalVariableIndex = astore.index;
				monitorLocalVariableOffset = astore.offset;
			}
			else
			{
				continue;
			}
			
			// Recherche des intructions MonitorExit correspondantes
			int monitorExitCount = 0;
				// Recherche en avant
			int j = i; 
			while (++j < length)
			{
				instruction = listForAnalyze.get(j);
				if (instruction.opcode != ByteCodeConstants.MONITOREXIT)
					continue;
				if (((MonitorExit)instruction).objectref.opcode != ByteCodeConstants.ALOAD)
					continue;
				ALoad al = (ALoad)((MonitorExit)instruction).objectref;
				if (al.index == monitorLocalVariableIndex)
				{
					monitorLocalVariableLenght = 
						al.offset - monitorLocalVariableOffset;
					monitorExitCount++;
				}
			}

			if (monitorExitCount == 1)
			{
				// Recherche en arriere (Jikes 1.22)
				j = i; 
				while (j-- > 0)
				{
					instruction = listForAnalyze.get(j);
					if (instruction.opcode != ByteCodeConstants.MONITOREXIT)
						continue;
					if (((MonitorExit)instruction).objectref.opcode != ByteCodeConstants.ALOAD)
						continue;
					ALoad al = (ALoad)((MonitorExit)instruction).objectref;
					if (al.index == monitorLocalVariableIndex)
					{
						monitorLocalVariableLenght += 
							monitorLocalVariableOffset - al.offset;
						monitorLocalVariableOffset = al.offset;
						
						monitorExitCount++;
						break;
					}
				}
			}

			if (monitorExitCount < 2)
				continue;
			
			// Verification de l'existance d'une variable locale
			LocalVariable lv = 
				localVariables.getLocalVariableWithIndexAndOffset(
					monitorLocalVariableIndex, monitorLocalVariableOffset);
			
			// Creation d'une variable locale
			if ((lv == null) || 
				(lv.start_pc+lv.length < monitorLocalVariableOffset+monitorLocalVariableLenght))
			{
				int signatureIndex = 
					constants.addConstantUtf8(StringConstants.INTERNAL_OBJECT_SIGNATURE);
				localVariables.add(new LocalVariable(
					monitorLocalVariableOffset, monitorLocalVariableLenght, 
					signatureIndex, signatureIndex, monitorLocalVariableIndex));
			}
		}
	}
	
	/*
	 * Verification de la portee de chaque variable : la portee generee par les 
	 * compilateurs est incorrecte : elle commence une instruction trop tard!
	 * De plus, la longueur de la portee est tres importante. Elle est 
	 * recalcul�e.
	 */
	private static void CheckLocalVariableRanges(
			ConstantPool constants, byte[] code, LocalVariables localVariables, 
			DefaultVariableNameGenerator variableNameGenerator, 
			List<Instruction> listForAnalyze)
	{
		// Reset length
		int length = localVariables.size();
		
		  // Remise � 1 de la longueur des port�es
		for (int i=localVariables.getIndexOfFirstLocalVariable(); i<length; i++)
			localVariables.getLocalVariableAt(i).length = 1;
		
		// Update range
		length = listForAnalyze.size();
		
		for (int i=0; i<length; i++)
		{
			Instruction instruction = listForAnalyze.get(i);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.PREINC:				
			case ByteCodeConstants.POSTINC:
				{
					instruction = ((IncInstruction)instruction).value;
					if ((instruction.opcode == ByteCodeConstants.ILOAD) ||
						(instruction.opcode == ByteCodeConstants.LOAD))
						CheckLocalVariableRangesForIndexInstruction(
							code, localVariables, (IndexInstruction)instruction);
				}
				break;
			case ByteCodeConstants.ASTORE:
				{
					AStore astore = (AStore)instruction;					
					// ExceptionLoad ?
					if (astore.valueref.opcode == ByteCodeConstants.EXCEPTIONLOAD)
					{
						ExceptionLoad el = 
							(ExceptionLoad)astore.valueref;
						
						if (el.exceptionNameIndex != 0)
						{
							LocalVariable lv = 
								localVariables.getLocalVariableWithIndexAndOffset(
									astore.index, astore.offset);
							
							if (lv == null)
							{				
								// Variable non trouv�e. Recherche de la variable avec 
								// l'offset suivant car les compilateurs place 'start_pc'
								// une instruction plus apres.
								int nextOffset = 
									ByteCodeUtil.NextInstructionOffset(code, astore.offset);					
								lv = localVariables.getLocalVariableWithIndexAndOffset(
									astore.index, nextOffset);		
								if (lv == null)
								{
									// Create a new local variable for exception
									lv = new LocalVariable(
										astore.offset, 1, -1, 
										el.exceptionNameIndex, astore.index, true);
									localVariables.add(lv);
									String signature = 
										constants.getConstantUtf8(el.exceptionNameIndex);									
									boolean appearsOnce = SignatureAppearsOnceInLocalVariables(
										localVariables, localVariables.size(), 
										el.exceptionNameIndex);
									String name = 
										variableNameGenerator.generateLocalVariableNameFromSignature(
											signature, appearsOnce);
									lv.name_index = constants.addConstantUtf8(name);
								}
								else
								{
									// Variable trouv�e. Mise � jour de 'start_pc' de la 
									// port�e.
									lv.updateRange(astore.offset);
								}
							}
						}
					}
					else if ((i+1 < length) && 
							 (astore.valueref.opcode == ByteCodeConstants.DUPLOAD) &&
							 (listForAnalyze.get(i+1).opcode == ByteCodeConstants.MONITORENTER))
					{
						// Monitor ?
						LocalVariable lv = 
							localVariables.getLocalVariableWithIndexAndOffset(
								astore.index, astore.offset);
						if (lv == null)
						{					
							MonitorEnter me = (MonitorEnter)listForAnalyze.get(i+1);
							if ((me.objectref.opcode == ByteCodeConstants.DUPLOAD) &&
								(((DupLoad)astore.valueref).dupStore == 
										((DupLoad)me.objectref).dupStore))
							{
								// Create a new local variable for monitor
								int signatureIndex = constants.addConstantUtf8(
									StringConstants.INTERNAL_OBJECT_SIGNATURE);
								localVariables.add(new LocalVariable(
									astore.offset, 1, signatureIndex, 
									signatureIndex, astore.index));
							}
							else
							{
								// Default case
								CheckLocalVariableRangesForIndexInstruction(
									code, localVariables, astore);
							}
						}
					}					
					else
					{
						// Default case
						CheckLocalVariableRangesForIndexInstruction(
							code, localVariables, astore);
					}					
				}
				break;
			case ByteCodeConstants.ISTORE:
			case ByteCodeConstants.ILOAD:
			case ByteCodeConstants.STORE:
			case ByteCodeConstants.LOAD:
			case ByteCodeConstants.ALOAD:
			case ByteCodeConstants.IINC:
				CheckLocalVariableRangesForIndexInstruction(
					code, localVariables, (IndexInstruction)instruction);
				break;
			}
		}		
	}
	
	private static void CheckLocalVariableRangesForIndexInstruction(
		byte[] code, LocalVariables localVariables, IndexInstruction ii)
	{
		LocalVariable lv = 
			localVariables.getLocalVariableWithIndexAndOffset(ii.index, ii.offset);
		
		if (lv == null)
		{
			// Variable non trouv�e. Recherche de la variable avec 
			// l'offset suivant car les compilateurs place 'start_pc'
			// une instruction plus apres.
			int nextOffset = ByteCodeUtil.NextInstructionOffset(code, ii.offset);					
			lv = localVariables.getLocalVariableWithIndexAndOffset(ii.index, nextOffset);					
			if (lv != null)
			{
				// Variable trouv�e. Mise � jour de 'start_pc' de la 
				// port�e.
				lv.updateRange(ii.offset);
			}
			else
			{
				// Mise � jour de la longueur de la port�es de la 
				// variable possedant le meme index et precedement 
				// definie.
				lv = localVariables.searchLocalVariableWithIndexAndOffset(ii.index, ii.offset);
				if (lv != null)
						lv.updateRange(ii.offset);
			}
		}
		else
		{
			// Mise � jour de la longeur de la port�e
			lv.updateRange(ii.offset);
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
	 * Fusion des entrees du tableau poss�dants les memes numero de slot, 
	 * le meme nom et le meme type. Le tableau genere pour le code suivant 
	 * contient deux entrees pour la variable 'a' !
		int a;
		if (e == null)
			a = 1;
		else
			a = 2;
		System.out.println(a);
	 */
//	private static void MergeLocalVariables(LocalVariables localVariables)
//	{		
//		for (int i=localVariables.size()-1; i>0; --i)
//		{
//			LocalVariable lv1 = localVariables.getLocalVariableAt(i);
//			
//			for (int j=i-1; j>=0; --j)
//			{
//				LocalVariable lv2 = localVariables.getLocalVariableAt(j);
//				
//				if ((lv1.index == lv2.index) && 
//					(lv1.signature_index == lv2.signature_index) && 
//					(lv1.name_index == lv2.name_index))
//				{
//					localVariables.remove(i);	
//					lv2.updateRange(lv1.start_pc);
//					lv2.updateRange(lv1.start_pc+lv1.length-1);
//					break;
//				}
//			}			
//		}
//	}

	// Create new local variables, set range and type, update attribute 
	// 'exception'
	/*
	 * Strategie :
	 * 	- Recherche de tous les instructions '?store' et '?load'
	 *  - Determiner le type de la viariable
	 *  - Si la variable n'est pas encore definie, ajouter une entr�e dans la 
	 *    Liste
	 *  - Sinon, si le type est compatible
	 */
	private static void AnalyzeMethodCode(
			ConstantPool constants, 
			LocalVariables localVariables, List<Instruction> list, 
			List<Instruction> listForAnalyze, String returnedSignature)
	{		
		// Recherche des instructions d'ecriture des variables locales.
		int length = listForAnalyze.size();

		for (int i=0; i<length; i++)
		{
			Instruction instruction = listForAnalyze.get(i);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.ISTORE:
			case ByteCodeConstants.STORE:
			case ByteCodeConstants.ASTORE:
			case ByteCodeConstants.ILOAD:
			case ByteCodeConstants.LOAD:
			case ByteCodeConstants.ALOAD:
			case ByteCodeConstants.IINC:
				SubAnalyzeMethodCode(
					constants, localVariables, list, listForAnalyze, 
					((IndexInstruction)instruction).index, i, 
					returnedSignature);
				break;
			}
		}
		
		// Analyse inverse
		boolean change;
		
		do
		{
			change = false;
			
			for (int i=0; i<length; i++)
			{
				Instruction instruction = listForAnalyze.get(i);	
				switch (instruction.opcode)
				{
				case ByteCodeConstants.ISTORE:
					{
						StoreInstruction si = (StoreInstruction)instruction;
						if (si.valueref.opcode == ByteCodeConstants.ILOAD)
						{
							// Contrainte du type de la variable li�e � ILoad par
							// le type de la variable li�e � IStore.
							change |= ReverseAnalyzeIStore(localVariables, si);
						}
					}
					break;
				case ByteCodeConstants.PUTSTATIC:
					{
						PutStatic ps = (PutStatic)instruction;
						switch (ps.valueref.opcode)
						{
						case ByteCodeConstants.ILOAD:
						case ByteCodeConstants.ALOAD:
							// Contrainte du type de la variable li�e � ILoad par
							// le type de la variable li�e � PutStatic.
							LoadInstruction load = (LoadInstruction)ps.valueref;
							change |= ReverseAnalyzePutStaticPutField(
								constants, localVariables, ps, load);
						}
					}
					break;
				case ByteCodeConstants.PUTFIELD:
					{
						PutField pf = (PutField)instruction;
						switch (pf.valueref.opcode)
						{
						case ByteCodeConstants.ILOAD:
						case ByteCodeConstants.ALOAD:
							// Contrainte du type de la variable li�e � ILoad
							// par le type de la variable li�e � PutField.
							LoadInstruction load = (LoadInstruction)pf.valueref;
							change |= ReverseAnalyzePutStaticPutField(
								constants, localVariables, pf, load);
						}
					}
					break;
				}
			}
		}
		while (change);
		
		// Selection d'un type pour les variables non encore typ�es.
		int internalObjectSignatureIndex = 
			constants.addConstantUtf8(StringConstants.INTERNAL_OBJECT_SIGNATURE);

		length = localVariables.size();
		
		for (int i=0; i<length; i++)
		{
			LocalVariable lv = localVariables.getLocalVariableAt(i);
	
			switch (lv.signature_index)
			{
			case UNDEFINED_TYPE:
				lv.signature_index = constants.addConstantUtf8(
						StringConstants.INTERNAL_OBJECT_SIGNATURE);
				break;
			case NUMBER_TYPE:
				lv.signature_index = constants.addConstantUtf8(
					SignatureUtil.GetSignatureFromTypesBitField(lv.typesBitField));
				break;
			case OBJECT_TYPE:
				// Plusieurs types sont affect�s � la m�me variable. Le 
				// decompilateur ne connait pas le graphe d'heritage des
				// classes decompil�es. Le type de la variable est valu� � 
				// 'Object'. Des instructions 'cast' suppl�mentaires doivent
				// etre ajout�s. Voir la limitation de JAD sur ce point.
				lv.signature_index = internalObjectSignatureIndex;
				break;
			}
		}
		
		// Ajout d'instructions "cast"
		for (int i=0; i<length; i++)
		{
			LocalVariable lv = localVariables.getLocalVariableAt(i);
			if (lv.signature_index == internalObjectSignatureIndex)
				AddCastInstruction(constants, list, localVariables, lv);		
		}
	}
	
	/*
	 * Analyse du type de la variable locale No varIndex
	 */
	private static void SubAnalyzeMethodCode(
			ConstantPool constants, LocalVariables localVariables, 
			List<Instruction> list, List<Instruction> listForAnalyze, 
			int varIndex, int startIndex, String returnedSignature)
	{
		IndexInstruction firstInstruction = 
			(IndexInstruction)listForAnalyze.get(startIndex);
		
		LocalVariable lv = 
			localVariables.getLocalVariableWithIndexAndOffset(
					firstInstruction.index, firstInstruction.offset);

		if (lv != null)
		{
			// Variable locale deja traitee	
			
			  // Verification que l'attribut 'exception' est correctement
			  // positionn�.
			if (firstInstruction.opcode == ByteCodeConstants.ASTORE)
			{
				AStore astore = (AStore)firstInstruction;
				if (astore.valueref.opcode == ByteCodeConstants.EXCEPTIONLOAD)
					lv.exceptionOrReturnAddress = true;
			}
				
			return;
		}
		
		final int length = listForAnalyze.size();
		
		// Recherche des instructions de lecture, d'ecriture et de comparaison
		// des variables locales.
		for (int i=startIndex; i<length; i++)
		{
			Instruction instruction = listForAnalyze.get(i);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.ISTORE:
				if (((IndexInstruction)instruction).index == varIndex)
					AnalyzeIStore(constants, localVariables, instruction);
				break;
			case ByteCodeConstants.STORE:
				if (((IndexInstruction)instruction).index == varIndex)
					AnalyzeStore(constants, localVariables, instruction);
				break;
			case ByteCodeConstants.ASTORE:
				if (((IndexInstruction)instruction).index == varIndex)
					AnalyzeAStore(constants, localVariables, instruction);
				break;
			case ByteCodeConstants.PREINC:
			case ByteCodeConstants.POSTINC:		
				instruction = ((IncInstruction)instruction).value;				
				if ((instruction.opcode != ByteCodeConstants.ILOAD) && 
					(instruction.opcode != ByteCodeConstants.LOAD))
					break;
			case ByteCodeConstants.ILOAD:
			case ByteCodeConstants.IINC:
				if (((IndexInstruction)instruction).index == varIndex)
					AnalyzeILoad(localVariables, instruction);
				break;
			case ByteCodeConstants.LOAD:
			case ByteCodeConstants.EXCEPTIONLOAD:
				if (((IndexInstruction)instruction).index == varIndex)
					AnalyzeLoad(localVariables, instruction);
				break;
			case ByteCodeConstants.ALOAD:
				if (((IndexInstruction)instruction).index == varIndex)
					AnalyzeALoad(localVariables, instruction);
				break;
			case ByteCodeConstants.INVOKEINTERFACE:
			case ByteCodeConstants.INVOKEVIRTUAL:
			case ByteCodeConstants.INVOKESPECIAL:
			case ByteCodeConstants.INVOKESTATIC:
				AnalyzeInvokeInstruction(
					constants, localVariables, instruction, varIndex);	
				break;
			case ByteCodeConstants.BINARYOP:
				BinaryOperatorInstruction boi = 
					(BinaryOperatorInstruction)instruction;
				AnalyzeBinaryOperator(
					constants, localVariables, instruction, 
					boi.value1, boi.value2, varIndex);	
				break;
			case ByteCodeConstants.IFCMP:
				IfCmp ic = (IfCmp)instruction;
				AnalyzeBinaryOperator(
					constants, localVariables, instruction, 
					ic.value1, ic.value2, varIndex);
				break;
			case ByteCodeConstants.XRETURN:
				AnalyzeReturnInstruction(
					constants, localVariables, instruction, 
					varIndex, returnedSignature);				
				break;
			}
		}
	}
	
	private static void AnalyzeIStore(
			ConstantPool constants, LocalVariables localVariables, 
			Instruction instruction)
	{
		StoreInstruction store = (StoreInstruction)instruction;
		int index = store.index;
		int offset = store.offset;
		
		LocalVariable lv = 
			localVariables.searchLocalVariableWithIndexAndOffset(index, offset);
		String signature = 
			store.getReturnedSignature(constants, localVariables);
				
		if (lv == null)
		{
			int typesBitField;
			
			if (signature == null)
			{
				if (store.valueref.opcode == ByteCodeConstants.ILOAD)
				{
					ILoad iload = (ILoad)store.valueref;				
					lv = localVariables.getLocalVariableWithIndexAndOffset(
						iload.index, iload.offset);		
					typesBitField = (lv == null) ? 
						ByteCodeConstants.TBF_INT_INT|ByteCodeConstants.TBF_INT_SHORT| 
						ByteCodeConstants.TBF_INT_BYTE|ByteCodeConstants.TBF_INT_CHAR|
						ByteCodeConstants.TBF_INT_BOOLEAN:
						lv.typesBitField;
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
				typesBitField = SignatureUtil.CreateTypesBitField(signature);				
			}
			
			localVariables.add(new LocalVariable(
				offset, 1, -1, NUMBER_TYPE, index, typesBitField));
		}
		else
		{
			if (signature == null)
			{
				lv.updateRange(offset);
			}
			else
			{
				// Une variable est trouv�e. Le type est il compatible ?
				int typesBitField = 
						SignatureUtil.CreateTypesBitField(signature);

				switch (lv.signature_index) 
				{
				case NUMBER_TYPE:
					if ((typesBitField & lv.typesBitField) != 0)
					{
						// Reduction de champ de bits
						lv.typesBitField &= typesBitField;
						lv.updateRange(offset);
					}
					else
					{
						// Type incompatible => creation de variables
						localVariables.add(new LocalVariable(
							offset, 1, -1, NUMBER_TYPE, index, typesBitField));				
					}
					break;
				case UNDEFINED_TYPE:
				case OBJECT_TYPE:
					// Type incompatible => creation de variables
					localVariables.add(new LocalVariable(
						offset, 1, -1, NUMBER_TYPE, index, typesBitField));
					break;
				default:
					String signatureLV = 
						constants.getConstantUtf8(lv.signature_index);
					int typesBitFieldLV = 
							SignatureUtil.CreateTypesBitField(signatureLV);
					
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
	}
	
	private static void AnalyzeILoad(
			LocalVariables localVariables, Instruction instruction)
	{
		IndexInstruction load = (IndexInstruction)instruction;
		int index = load.index;
		int offset = load.offset;

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
	
	private static void AnalyzeLoad(
			LocalVariables localVariables, Instruction instruction)
	{
		IndexInstruction load = (IndexInstruction)instruction;
		int index = load.index;
		int offset = load.offset;

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
	
	private static void AnalyzeALoad(
			LocalVariables localVariables, Instruction instruction)
	{
		IndexInstruction load = (IndexInstruction)instruction;
		int index = load.index;
		int offset = load.offset;

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

	private static void AnalyzeInvokeInstruction(
			ConstantPool constants, LocalVariables localVariables, 
			Instruction instruction, int varIndex)
	{
		final InvokeInstruction invokeInstruction = 
									(InvokeInstruction)instruction;
		final List<Instruction> args = invokeInstruction.args;
		final List<String> argSignatures = 
			invokeInstruction.getListOfParameterSignatures(constants);
		final int nbrOfArgs = args.size();
		
		for (int j=0; j<nbrOfArgs; j++)
		{
			AnalyzeArgOrReturnedInstruction(
				constants, localVariables, args.get(j), 
				varIndex, argSignatures.get(j));
		}		
	}
	
	private static void AnalyzeArgOrReturnedInstruction(
		ConstantPool constants, LocalVariables localVariables, 
		Instruction instruction, int varIndex, String signature)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ILOAD:
			LoadInstruction li = (LoadInstruction)instruction;
			if (li.index == varIndex)
			{
				LocalVariable lv = 
					localVariables.searchLocalVariableWithIndexAndOffset(li.index, li.offset);				
				if (lv != null)
					lv.typesBitField &= 
						SignatureUtil.CreateArgOrReturnBitFields(signature);
			}
			break;
		case ByteCodeConstants.ALOAD:
			li = (LoadInstruction)instruction;
			if (li.index == varIndex)
			{
				LocalVariable lv = 
					localVariables.searchLocalVariableWithIndexAndOffset(
						li.index, li.offset);				
				if (lv != null)
				{
					switch (lv.signature_index)
					{
					case UNDEFINED_TYPE:
						lv.signature_index = 
							constants.addConstantUtf8(signature);
						break;
					case NUMBER_TYPE:
						new Throwable("type inattendu").printStackTrace();
						break;
					// NE PAS GENERER DE CONFLIT DE TYPE LORSQUE LE TYPE
					// D'UNE VARIABLE EST DIFFERENT DU TYPE D'UN PARAMETRE.
					/* case OBJECT_TYPE:
						break;
					default:
						String signature = 
							constants.getConstantUtf8(lv.signature_index);		
						String argSignature = argSignatures.get(j);
						
						if (!argSignature.equals(signature) &&
							!argSignature.equals(
								Constants.INTERNAL_OBJECT_SIGNATURE))
						{
							// La signature du parametre ne correspond pas
							// a la signature de l'objet pass� en parametre
							lv.signature_index = OBJECT_TYPE;
						}*/
					}
				}
			}
			break;				
		}		
	}
	
	/*
	 * Reduction de l'ensemble des types entiers.
	 */
	private static void AnalyzeBinaryOperator(
			ConstantPool constants, LocalVariables localVariables, 
			Instruction instruction, Instruction i1, Instruction i2, 
			int varIndex)
	{
		if ( 
			((i1.opcode != ByteCodeConstants.ILOAD) || (((ILoad)i1).index != varIndex)) &&
			((i2.opcode != ByteCodeConstants.ILOAD) || (((ILoad)i2).index != varIndex))
		   )
			return;		
		
		LocalVariable lv1 = (i1.opcode == ByteCodeConstants.ILOAD) ?
			localVariables.searchLocalVariableWithIndexAndOffset(
				((ILoad)i1).index, i1.offset) : null;
		
		LocalVariable lv2 = (i2.opcode == ByteCodeConstants.ILOAD) ?
			localVariables.searchLocalVariableWithIndexAndOffset(
				((ILoad)i2).index, i2.offset) : null;
		
		if (lv1 != null)
		{
			lv1.updateRange(instruction.offset);
			if (lv2 != null)
				lv2.updateRange(instruction.offset);

			if (lv1.signature_index == NUMBER_TYPE)
			{
				// Reduction des types de lv1
				if (lv2 != null)
				{
					if (lv2.signature_index == NUMBER_TYPE)
					{
						// Reduction des types de lv1 & lv2
						lv1.typesBitField &= lv2.typesBitField;
						lv2.typesBitField &= lv1.typesBitField;
					}			
					else
					{
						lv1.signature_index = lv2.signature_index;
					}
				}
				else
				{
					String signature = 
						i2.getReturnedSignature(constants, localVariables);
					
					if (SignatureUtil.IsIntegerSignature(signature))
					{
						int type = SignatureUtil.CreateTypesBitField(signature);					
						if (type != 0)
							lv1.typesBitField &= type;
					}
				}
			}
			else if ((lv2 != null) && (lv2.signature_index == NUMBER_TYPE))
			{
				// Reduction des types de lv2
				lv2.signature_index = lv1.signature_index;		
			}
		}
		else if (lv2 != null)
		{
			lv2.updateRange(instruction.offset);
			
			if (lv2.signature_index == NUMBER_TYPE)
			{
				// Reduction des types de lv2
				String signature = 
					i1.getReturnedSignature(constants, localVariables);
				
				if (SignatureUtil.IsIntegerSignature(signature))
				{
					int type = SignatureUtil.CreateTypesBitField(signature);					
					if (type != 0)
						lv2.typesBitField &= type;
				}
			}
		}
	}
	
	private static void AnalyzeReturnInstruction(
		ConstantPool constants, LocalVariables localVariables, 
		Instruction instruction, int varIndex, String returnedSignature)
	{
		ReturnInstruction ri = (ReturnInstruction)instruction;		
		AnalyzeArgOrReturnedInstruction(
			constants, localVariables, ri.valueref, 
			varIndex, returnedSignature);
	}
	
	private static void AnalyzeStore(
			ConstantPool constants, LocalVariables localVariables, 
			Instruction instruction)
	{
		StoreInstruction store = (StoreInstruction)instruction;
		int index = store.index;
		int offset = store.offset;
		
		LocalVariable lv = 
			localVariables.searchLocalVariableWithIndexAndOffset(index, offset);
		String signature = 
			instruction.getReturnedSignature(constants, localVariables);
		int signatureIndex = 
			(signature != null) ? constants.addConstantUtf8(signature) : -1;
				
		if (lv == null)
		{
			localVariables.add(new LocalVariable(
				offset, 1, -1, signatureIndex, index));
		}
		else
		{
			// Une variable est trouv�e. Le type est il compatible ?
			if (lv.signature_index == signatureIndex)
			{
				lv.updateRange(offset);
			}
			else
			{
				// Type incompatible => creation de variables
				localVariables.add(new LocalVariable(
						offset, 1, -1, signatureIndex, index));
			}
		}
	}
	
	private static void AnalyzeAStore(
			ConstantPool constants, LocalVariables localVariables, 
			Instruction instruction)
	{
		StoreInstruction store = (StoreInstruction)instruction;
		int index = store.index;
		int offset = store.offset;
		
		LocalVariable lv = 
			localVariables.searchLocalVariableWithIndexAndOffset(index, offset);
		String signatureInstruction = 
			instruction.getReturnedSignature(constants, localVariables);
		int signatureInstructionIndex = (signatureInstruction != null) ? 
			constants.addConstantUtf8(signatureInstruction) : UNDEFINED_TYPE;
		boolean isExceptionOrReturnAddress = 
			(store.valueref.opcode == FastConstants.EXCEPTIONLOAD) ||
			(store.valueref.opcode == FastConstants.RETURNADDRESSLOAD);
		
		if ((lv == null) || lv.exceptionOrReturnAddress ||
			(isExceptionOrReturnAddress && (lv.start_pc + lv.length < offset)))
		{
			localVariables.add(new LocalVariable(
				offset, 1, -1, signatureInstructionIndex, index, 
				isExceptionOrReturnAddress));
		}
		else if (isExceptionOrReturnAddress == false)
		{
			// Une variable est trouv�e. Le type est il compatible ?
			if (lv.signature_index == UNDEFINED_TYPE)
			{
				// Cas particulier Jikes 1.2.2 bloc finally :
				//  Une instruction ALoad apparait avant AStore
				lv.signature_index = signatureInstructionIndex;
				lv.updateRange(offset);				
			}
			else if (lv.signature_index == NUMBER_TYPE)
			{
				// Creation de variables
				localVariables.add(new LocalVariable(
						offset, 1, -1, signatureInstructionIndex, index));
			}
			else if ((lv.signature_index == signatureInstructionIndex) || 
					 (lv.signature_index == OBJECT_TYPE))
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
					constants.getConstantUtf8(lv.signature_index);
				
				if (SignatureUtil.IsPrimitiveSignature(signatureLV))
				{
					// Creation de variables
					localVariables.add(new LocalVariable(
							offset, 1, -1, signatureInstructionIndex, index));
				}
				else if (signatureInstructionIndex != UNDEFINED_TYPE)
				{
					// Modification du type de variable
					lv.signature_index = OBJECT_TYPE;
					lv.updateRange(offset);	
				}
				else
				{
					// Affectation de NULL a une variable de type connu et non 
					// primitif
					lv.updateRange(offset);	
				}
			}
		}
	}
	
	// Substitution des types byte par char dans les instructions 
	// bipush, sipush et iconst suivants les instructions istore et invoke.
	private static void SetConstantTypes(
			ClassFile classFile, ConstantPool constants, Method method,
			LocalVariables localVariables, List<Instruction> list, 
			List<Instruction> listForAnalyze, String returnedSignature)
	{
		final int length = listForAnalyze.size();
		
		// Affection du type des constantes depuis les instructions m�res
		for (int i=0; i<length; i++)
		{
			final Instruction instruction = listForAnalyze.get(i);
			
			switch (instruction.opcode)
			{
			case ByteCodeConstants.ARRAYSTORE:
				SetConstantTypesArrayStore(
					constants, localVariables, 
					(ArrayStoreInstruction)instruction);
				break;
			case ByteCodeConstants.BINARYOP:
				{
					BinaryOperatorInstruction boi = 
						(BinaryOperatorInstruction)instruction;
					SetConstantTypesBinaryOperator(
						constants, localVariables, boi.value1, boi.value2);
				}
				break;
			case ByteCodeConstants.IFCMP:
				{
					IfCmp ic = (IfCmp)instruction;
					SetConstantTypesBinaryOperator(
						constants, localVariables, ic.value1, ic.value2);
				}
				break;
			case ByteCodeConstants.INVOKEINTERFACE:
			case ByteCodeConstants.INVOKEVIRTUAL:
			case ByteCodeConstants.INVOKESPECIAL:
			case ByteCodeConstants.INVOKESTATIC:
			case ByteCodeConstants.INVOKENEW:
				SetConstantTypesInvokeInstruction(constants, instruction);
				break;
			case ByteCodeConstants.ISTORE:
				SetConstantTypesIStore(constants, localVariables, instruction);	
				break;
			case ByteCodeConstants.PUTFIELD:
				{
					PutField putField = (PutField)instruction;
					SetConstantTypesPutFieldAndPutStatic(
						constants, localVariables, 
						putField.valueref, putField.index);
				}
				break;
			case ByteCodeConstants.PUTSTATIC:
				{
					PutStatic putStatic = (PutStatic)instruction;
					SetConstantTypesPutFieldAndPutStatic(
							constants, localVariables, 
							putStatic.valueref, putStatic.index);
				}
				break;
			case ByteCodeConstants.XRETURN:
				{
					SetConstantTypesXReturn(instruction, returnedSignature);
				}
				break;
			}
		}
		
		// Determination des types des constantes apparaissant dans les 
		// instructions 'TernaryOpStore'.
		for (int i=0; i<length; i++)
		{
			Instruction instruction = listForAnalyze.get(i);
			
			if (instruction.opcode == ByteCodeConstants.TERNARYOPSTORE)
			{
				TernaryOpStore tos = (TernaryOpStore)instruction;
				SetConstantTypesTernaryOpStore(
					constants, localVariables, list, tos);		
			}
		}			
	}
	
	private static void SetConstantTypesInvokeInstruction(
			ConstantPool constants,
			Instruction instruction)
	{
		final InvokeInstruction invokeInstruction = 
			(InvokeInstruction)instruction;
		final List<Instruction> args = invokeInstruction.args;
		final List<String> types = 
			invokeInstruction.getListOfParameterSignatures(constants);
		final int nbrOfArgs = args.size();
		
		for (int j=0; j<nbrOfArgs; j++)
		{
			Instruction arg = args.get(j);
				
			switch (arg.opcode)
			{
			case ByteCodeConstants.BIPUSH:
			case ByteCodeConstants.ICONST:
			case ByteCodeConstants.SIPUSH:
				((IConst)arg).setReturnedSignature(types.get(j));
				break;
			}
		}
	}
	
	private static void SetConstantTypesPutFieldAndPutStatic(
			ConstantPool constants,
			LocalVariables localVariables, 
			Instruction valueref, int index)
	{
		switch (valueref.opcode)
		{
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.SIPUSH:
			ConstantFieldref cfr = constants.getConstantFieldref(index);
			ConstantNameAndType cnat = 
				constants.getConstantNameAndType(cfr.name_and_type_index);
			String signature = constants.getConstantUtf8(cnat.descriptor_index);			
			((IConst)valueref).setReturnedSignature(signature);
			break;
		}				
	}

	private static void SetConstantTypesTernaryOpStore(
			ConstantPool constants, LocalVariables localVariables, 
			List<Instruction> list, TernaryOpStore tos)
	{
		switch (tos.objectref.opcode)
		{
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.SIPUSH:
			// Recherche de la seconde valeur de l'instruction ternaire
			int index = InstructionUtil.getIndexForOffset(
				list, tos.ternaryOp2ndValueOffset);
			
			if (index != -1)
			{
				int length = list.size();

				while (index < length)
				{
					Instruction result = 
						SearchInstructionByOffsetVisitor.visit(
							list.get(index), tos.ternaryOp2ndValueOffset);
					
					if (result != null)
					{
						String signature = 
							result.getReturnedSignature(constants, localVariables);
						((IConst)tos.objectref).setReturnedSignature(signature);
						break;
					}
					
					index++;
				}
			}			
			break;
		}
	}
	
	private static void SetConstantTypesArrayStore(
			ConstantPool constants,
			LocalVariables localVariables, 
			ArrayStoreInstruction asi)
	{
		switch (asi.valueref.opcode)
		{
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.SIPUSH:			
			switch (asi.arrayref.opcode)
			{
			case ByteCodeConstants.ALOAD:
				{
					ALoad aload = (ALoad)asi.arrayref;
					LocalVariable lv = localVariables.getLocalVariableWithIndexAndOffset(
							aload.index, aload.offset);
					
					if (lv == null)
					{
						new Throwable("lv is null. index=" + aload.index).printStackTrace();
						return;
					}
					
					String signature = 
						constants.getConstantUtf8(lv.signature_index);
					((IConst)asi.valueref).setReturnedSignature(
							SignatureUtil.CutArrayDimensionPrefix(signature));
				}
				break;
			case ByteCodeConstants.GETFIELD:
			case ByteCodeConstants.GETSTATIC:
				{
					IndexInstruction ii = (IndexInstruction)asi.arrayref;
					ConstantFieldref cfr = 
						constants.getConstantFieldref(ii.index);
					ConstantNameAndType cnat = 
						constants.getConstantNameAndType(cfr.name_and_type_index);
					String signature = 
						constants.getConstantUtf8(cnat.descriptor_index);	
					((IConst)asi.valueref).setReturnedSignature(
							SignatureUtil.CutArrayDimensionPrefix(signature));
				}
				break;
			}
			break;
		}				
	}
	
	private static void SetConstantTypesIStore(
			ConstantPool constants,
			LocalVariables localVariables, 
			Instruction instruction)
	{
		StoreInstruction store = (StoreInstruction)instruction;
		
		switch (store.valueref.opcode)
		{
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.SIPUSH:
			final LocalVariable lv = 
				localVariables.getLocalVariableWithIndexAndOffset(
						store.index, store.offset);
			String signature = constants.getConstantUtf8(lv.signature_index);			
			((IConst)store.valueref).setReturnedSignature(signature);
			break;
		}		
	}
	
	private static void SetConstantTypesBinaryOperator(
			ConstantPool constants,
			LocalVariables localVariables, 
			Instruction i1, Instruction i2)
	{
		switch (i1.opcode)
		{
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.SIPUSH:
			switch (i2.opcode)
			{
			case ByteCodeConstants.BIPUSH:
			case ByteCodeConstants.ICONST:
			case ByteCodeConstants.SIPUSH:
				break;
			default:
				String signature = i2.getReturnedSignature(
					constants, localVariables);
				if (signature != null)
					((IConst)i1).setReturnedSignature(signature);
			}	
			break;
		default:
			switch (i2.opcode)
			{
			case ByteCodeConstants.BIPUSH:
			case ByteCodeConstants.ICONST:
			case ByteCodeConstants.SIPUSH:
				String signature = i1.getReturnedSignature(
						constants, localVariables);
				if (signature != null)
					((IConst)i2).setReturnedSignature(signature);
				break;
			}
		}		
	}
	
	private static void SetConstantTypesXReturn(
			Instruction instruction, String returnedSignature)
	{
		ReturnInstruction ri = (ReturnInstruction)instruction;
		
		int opcode = ri.valueref.opcode;

		if ((opcode != ByteCodeConstants.SIPUSH) && 
			(opcode != ByteCodeConstants.BIPUSH) && 
			(opcode != ByteCodeConstants.ICONST))
			return;
		
		((IConst)ri.valueref).signature = returnedSignature;
	}
	
	
	private static String GetReturnedSignature(
			ClassFile classFile, Method method)
	{
		AttributeSignature as = method.getAttributeSignature();
	    int signatureIndex = (as == null) ? 
	    	method.descriptor_index : as.signature_index;
	    String signature = 
	    	classFile.getConstantPool().getConstantUtf8(signatureIndex);
	    
	    return SignatureUtil.GetMethodReturnedSignature(signature);
	}
	
	private static void InitialyzeExceptionLoad(
		List<Instruction> listForAnalyze, LocalVariables localVariables)
	{
		int length = listForAnalyze.size();
		
		/*
		 * Methode d'initialisation des instructions ExceptionLoad non 
		 * initialis�es. Cela se produit lorsque les methodes poss�dent un bloc 
		 * de definition de variables locales. 
		 * Les instructions ExceptionLoad appartenant aux blocs 'finally' ne 
		 * sont pas initialis�e.
		 */
		for (int index=0; index<length; index++)
		{
			Instruction i = listForAnalyze.get(index);

			if (i.opcode == ByteCodeConstants.ASTORE)
			{
				AStore as = (AStore)i;
				
				if (as.valueref.opcode == ByteCodeConstants.EXCEPTIONLOAD)
				{
					ExceptionLoad el = (ExceptionLoad)as.valueref;
					if (el.index == UtilConstants.INVALID_INDEX)
						el.index = as.index;
				}
			}
		}
		
		/*
		 * Lorsque les exceptions ne sont pas utilis�es dans le block 'catch', 
		 * aucune variable locale n'est cr��e. Une pseudo variable locale est 
		 * alors cr��e pour afficher correctement l'instruction 
		 * "catch (Exception localException)".
		 * Aucun ajout d'instruction si "ExceptionLoad" correspond � une 
		 * instruction "finally".
		 */
		for (int index=0; index<length; index++)
		{
			Instruction i = listForAnalyze.get(index);

			if (i.opcode == ByteCodeConstants.EXCEPTIONLOAD)
			{
				ExceptionLoad el = (ExceptionLoad)i;
				
				if ((el.index == UtilConstants.INVALID_INDEX) && 
					(el.exceptionNameIndex > 0))
				{
					int varIndex = localVariables.size();
					LocalVariable localVariable = new LocalVariable(
						el.offset, 1, UtilConstants.INVALID_INDEX, 
						el.exceptionNameIndex, varIndex, true);										
					localVariables.add(localVariable);
					el.index = varIndex;
				}
			}
		}	
	}

	private static void GenerateLocalVariableNames(
			ConstantPool constants,
			LocalVariables localVariables, 
			DefaultVariableNameGenerator variableNameGenerator)
	{
		final int length = localVariables.size();
			
		for (int i=localVariables.getIndexOfFirstLocalVariable(); i<length; i++)
		{
			final LocalVariable lv = localVariables.getLocalVariableAt(i);
			
			if ((lv != null) && (lv.name_index <= 0))
			{
				String signature = constants.getConstantUtf8(lv.signature_index);
				boolean appearsOnce = SignatureAppearsOnceInLocalVariables(
						localVariables, length, lv.signature_index);
				String name = 
					variableNameGenerator.generateLocalVariableNameFromSignature(
							signature, appearsOnce);
				lv.name_index = constants.addConstantUtf8(name);
			}
		}
	}
	
	private static boolean SignatureAppearsOnceInParameters(
		List<String> parameterTypes, int firstIndex, 
		int length, String signature)
	{
		int counter = 0;
		
		for (int i=firstIndex; (i<length) && (counter<2); i++)
			if (signature.equals(parameterTypes.get(i)))
				counter++;
		
		return (counter <= 1);
	}
	
	private static boolean SignatureAppearsOnceInLocalVariables(
			LocalVariables localVariables, 
			int length, int signature_index)
	{
		int counter = 0;
		
		for (int i=localVariables.getIndexOfFirstLocalVariable(); 
					(i<length) && (counter<2); i++)
		{
			final LocalVariable lv = localVariables.getLocalVariableAt(i);
			if ((lv != null) && (lv.signature_index == signature_index))
				counter++;
		}
		
		return (counter == 1);
	}

	private static boolean ReverseAnalyzeIStore(
		LocalVariables localVariables, StoreInstruction si)
	{
		LoadInstruction load = (LoadInstruction)si.valueref;
		LocalVariable lvLoad = 
			localVariables.getLocalVariableWithIndexAndOffset(
				load.index, load.offset);
		
		if ((lvLoad == null) || (lvLoad.signature_index != NUMBER_TYPE))
			return false;
		
		LocalVariable lvStore = 
			localVariables.getLocalVariableWithIndexAndOffset(
				si.index, si.offset);
		
		if (lvStore == null)
			return false;
		
		if (lvStore.signature_index == NUMBER_TYPE)
		{
			int old = lvLoad.typesBitField;
			lvLoad.typesBitField &= lvStore.typesBitField;
			return (old != lvLoad.typesBitField);
		}
		else if ((lvStore.signature_index >= 0) &&
				 (lvStore.signature_index != lvLoad.signature_index))
		{
			lvLoad.signature_index = lvStore.signature_index;
			return true;
		}	
		
		return false;
	}
		
	private static boolean ReverseAnalyzePutStaticPutField(
		ConstantPool constants, LocalVariables localVariables, 
		IndexInstruction ii, LoadInstruction load)
	{
		LocalVariable lvLoad = 
			localVariables.getLocalVariableWithIndexAndOffset(
				load.index, load.offset);
		
		if (lvLoad != null)
		{
			ConstantFieldref cfr = constants.getConstantFieldref(ii.index);
			ConstantNameAndType cnat = 
				constants.getConstantNameAndType(cfr.name_and_type_index);
			
			if (lvLoad.signature_index == NUMBER_TYPE)
			{
				String descriptor = constants.getConstantUtf8(cnat.descriptor_index);
				int typesBitField = SignatureUtil.CreateArgOrReturnBitFields(descriptor);			
				int old = lvLoad.typesBitField;
				lvLoad.typesBitField &= typesBitField;
				return (old != lvLoad.typesBitField);
			}
			else if (lvLoad.signature_index == UNDEFINED_TYPE)
			{
				lvLoad.signature_index = cnat.descriptor_index;
				return true;
			}
		}
		
		return false;
	}
				
	private static void AddCastInstruction(
			ConstantPool constants, List<Instruction> list, 
			LocalVariables localVariables, LocalVariable lv)
	{
		// Add cast instruction before all 'ALoad' instruction for local 
		// variable le used type is not 'Object'.		
		AddCheckCastVisitor visitor = new AddCheckCastVisitor(
			constants, localVariables, lv);
		
		final int length = list.size();
		
		for (int i=0; i<length; i++)
			visitor.visit(list.get(i));
	}
}
