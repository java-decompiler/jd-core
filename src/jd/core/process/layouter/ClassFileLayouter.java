package jd.core.process.layouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Field.ValueAndMethod;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.layout.block.BlockLayoutBlock;
import jd.core.model.layout.block.ByteCodeLayoutBlock;
import jd.core.model.layout.block.CommentDeprecatedLayoutBlock;
import jd.core.model.layout.block.CommentErrorLayoutBlock;
import jd.core.model.layout.block.ExtendsSuperInterfacesLayoutBlock;
import jd.core.model.layout.block.ExtendsSuperTypeLayoutBlock;
import jd.core.model.layout.block.FieldNameLayoutBlock;
import jd.core.model.layout.block.FragmentLayoutBlock;
import jd.core.model.layout.block.ImplementsInterfacesLayoutBlock;
import jd.core.model.layout.block.ImportsLayoutBlock;
import jd.core.model.layout.block.InnerTypeBodyBlockEndLayoutBlock;
import jd.core.model.layout.block.InnerTypeBodyBlockStartLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.model.layout.block.LayoutBlockConstants;
import jd.core.model.layout.block.MarkerLayoutBlock;
import jd.core.model.layout.block.MethodBodyBlockEndLayoutBlock;
import jd.core.model.layout.block.MethodBodyBlockStartLayoutBlock;
import jd.core.model.layout.block.MethodBodySingleLineBlockEndLayoutBlock;
import jd.core.model.layout.block.MethodNameLayoutBlock;
import jd.core.model.layout.block.MethodStaticLayoutBlock;
import jd.core.model.layout.block.PackageLayoutBlock;
import jd.core.model.layout.block.SeparatorLayoutBlock;
import jd.core.model.layout.block.SubListLayoutBlock;
import jd.core.model.layout.block.ThrowsLayoutBlock;
import jd.core.model.layout.block.TypeBodyBlockEndLayoutBlock;
import jd.core.model.layout.block.TypeBodyBlockStartLayoutBlock;
import jd.core.model.layout.block.TypeNameLayoutBlock;
import jd.core.model.layout.section.LayoutSection;
import jd.core.model.reference.Reference;
import jd.core.model.reference.ReferenceMap;
import jd.core.preferences.Preferences;
import jd.core.process.layouter.visitor.InstructionSplitterVisitor;
import jd.core.process.layouter.visitor.MaxLineNumberVisitor;
import jd.core.util.ClassFileUtil;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;
import jd.core.util.TypeNameUtil;

public class ClassFileLayouter {

	public static int Layout(
		Preferences preferences, 
		ReferenceMap referenceMap, 
		ClassFile classFile, 
		ArrayList<LayoutBlock> layoutBlockList)
	{
		int maxLineNumber = CreateBlocks(
				preferences, referenceMap, classFile, layoutBlockList);
		
		// "layoutBlockList" contient une structure lineaire classee dans 
		// l'ordre naturel sans prendre les contraintes d'alignement.
		
		if ((maxLineNumber != Instruction.UNKNOWN_LINE_NUMBER) && 
			preferences.getRealignmentLineNumber())
		{
			LayoutBlocks(layoutBlockList);
		}
		
		return maxLineNumber;
	}

	private static int CreateBlocks(
		Preferences preferences, 
		ReferenceMap referenceMap, 
		ClassFile classFile,
		ArrayList<LayoutBlock> layoutBlockList)
	{
		boolean separator = true;
		
		// Layout package statement
		String internalPackageName = classFile.getInternalPackageName();
		if ((internalPackageName != null) && (internalPackageName.length() > 0))
		{
			layoutBlockList.add(new PackageLayoutBlock(classFile));			
			layoutBlockList.add(
				new SeparatorLayoutBlock(LayoutBlockConstants.SEPARATOR, 2));
			separator = false;
		}
		
		// Layout import statements
		int importCount = GetImportCount(referenceMap, classFile);
		if (importCount > 0)
		{
			layoutBlockList.add(new ImportsLayoutBlock(
				classFile, importCount-1));
			layoutBlockList.add(new SeparatorLayoutBlock(
				LayoutBlockConstants.SEPARATOR_AFTER_IMPORTS, 2));
			separator = false;
		}
		
		if (separator)
		{
			layoutBlockList.add(new SeparatorLayoutBlock(
				LayoutBlockConstants.SEPARATOR_AT_BEGINING, 0));
		}
		
		// Layout class
		return CreateBlocksForClass(preferences, classFile, layoutBlockList);
	}	
	
	private static int GetImportCount(
		ReferenceMap referenceMap, ClassFile classFile) 
	{
		Collection<Reference> collection = referenceMap.values();
		
		if (collection.size() > 0) 
		{
			int importCount = 0;
			String internalPackageName = classFile.getInternalPackageName();
			Iterator<Reference> iterator = collection.iterator();
			
			// Filtrage
			while (iterator.hasNext())
			{
				String internalReferencePackageName = 
					TypeNameUtil.InternalTypeNameToInternalPackageName(
						iterator.next().getInternalName());
				
				// No import for same package classes
				if (internalReferencePackageName.equals(internalPackageName))
				{
					continue;
				}
				
				// No import for 'java/lang' classes
				if (internalReferencePackageName.equals(
						StringConstants.INTERNAL_JAVA_LANG_PACKAGE_NAME))
				{
					continue;
				}

				importCount++;
			}	
			
			return importCount;
		}
		else
		{
			return 0;
		}
	}
	
	private static int CreateBlocksForClass(
		Preferences preferences, 
		ClassFile classFile, 
		List<LayoutBlock> layoutBlockList)
	{
		MarkerLayoutBlock tmslb = new MarkerLayoutBlock(
			LayoutBlockConstants.TYPE_MARKER_START, classFile);
		layoutBlockList.add(tmslb);
		
		boolean displayExtendsOrImplementsFlag = 
			CreateBlocksForHeader(classFile, layoutBlockList);

		TypeBodyBlockStartLayoutBlock bbslb = new TypeBodyBlockStartLayoutBlock();
		layoutBlockList.add(bbslb);
		
		int layoutBlockListLength = layoutBlockList.size();

		int maxLineNumber = CreateBlocksForBody(
			preferences, classFile, 
			layoutBlockList, displayExtendsOrImplementsFlag);
		
		if (layoutBlockListLength == layoutBlockList.size())
		{
			// Classe vide. Transformation du bloc 'BodyBlockStartLayoutBlock'
			if (displayExtendsOrImplementsFlag)
				bbslb.transformToStartEndBlock(1);
			else
				bbslb.transformToStartEndBlock(0);
		}
		else
		{		
			TypeBodyBlockEndLayoutBlock bbelb = new TypeBodyBlockEndLayoutBlock();
			bbslb.other = bbelb;
			bbelb.other = bbslb;
			layoutBlockList.add(bbelb);
		}

		MarkerLayoutBlock tmelb = new MarkerLayoutBlock(
			LayoutBlockConstants.TYPE_MARKER_END, classFile);
		tmslb.other = tmelb;
		tmelb.other = tmslb;
		layoutBlockList.add(tmelb);
		
		return maxLineNumber;
	}
		
	private static boolean CreateBlocksForHeader(
		ClassFile classFile, List<LayoutBlock> layoutBlockList)
	{
		boolean displayExtendsOrImplementsFlag = false;
		
		if (classFile.containsAttributeDeprecated() &&
			!classFile.containsAnnotationDeprecated(classFile))
		{
			layoutBlockList.add(new CommentDeprecatedLayoutBlock());
		}
			
		// Affichage des attributs de la classe
		//LayoutAttributes(
		//	layoutBlockList, classFile, classFile.getAttributes());

		// Affichage des annotations de la classe
		AnnotationLayouter.CreateBlocksForAnnotations(
			classFile, classFile.getAttributes(), layoutBlockList);

		// Affichage de la classe, de l'interface, de l'enum ou de l'annotation		
		 // Check annotation
		AttributeSignature as = classFile.getAttributeSignature();
		if (as == null)
		{
			layoutBlockList.add(new TypeNameLayoutBlock(classFile));
			
			if ((classFile.access_flags & ClassFileConstants.ACC_ANNOTATION) != 0)
			{
				// Annotation
			}
			else if ((classFile.access_flags & ClassFileConstants.ACC_ENUM) != 0)
			{
				// Enum
				 // Interfaces
				displayExtendsOrImplementsFlag =
					CreateBlocksForInterfacesImplements(
						classFile, layoutBlockList);				
			}
			else if ((classFile.access_flags & ClassFileConstants.ACC_INTERFACE) != 0)
			{
				// Interface
				 // Super interface
				int[] interfaceIndexes = classFile.getInterfaces();
				if ((interfaceIndexes != null) && (interfaceIndexes.length > 0))
				{
					displayExtendsOrImplementsFlag = true;
					layoutBlockList.add(
						new ExtendsSuperInterfacesLayoutBlock(classFile));
				}
			}
			else
			{
				// Class
				 // Super class
				String internalSuperClassName = classFile.getSuperClassName();
				if ((internalSuperClassName != null) && 
					!StringConstants.INTERNAL_OBJECT_CLASS_NAME.equals(internalSuperClassName))
				{
					displayExtendsOrImplementsFlag = true;
					layoutBlockList.add(
						new ExtendsSuperTypeLayoutBlock(classFile));
				}
				
				// Interfaces
				displayExtendsOrImplementsFlag |=
					CreateBlocksForInterfacesImplements(
						classFile, layoutBlockList);			
			}		
		}
		else
		{
			// Signature contenant des notations generiques
			ConstantPool constants = classFile.getConstantPool();	
			String signature = constants.getConstantUtf8(as.signature_index);
			displayExtendsOrImplementsFlag = 
				SignatureLayouter.CreateLayoutBlocksForClassSignature(
					classFile, signature, layoutBlockList);
		}
		
		return displayExtendsOrImplementsFlag;
	}
	
	private static boolean CreateBlocksForInterfacesImplements(
		ClassFile classFile, List<LayoutBlock> layoutBlockList)
	{
		int[] interfaceIndexes = classFile.getInterfaces();
			
		if ((interfaceIndexes != null) && (interfaceIndexes.length > 0))
		{
			layoutBlockList.add(
				new ImplementsInterfacesLayoutBlock(classFile));
			
			return true;
		} 	
		else
		{
			return false;
		}
	}

	public static int CreateBlocksForBodyOfAnonymousClass(
		Preferences preferences, 
		ClassFile classFile, 
		List<LayoutBlock> layoutBlockList)
	{
		InnerTypeBodyBlockStartLayoutBlock ibbslb = 
			new InnerTypeBodyBlockStartLayoutBlock();
		layoutBlockList.add(ibbslb);
		
		int layoutBlockListLength = layoutBlockList.size();

		int maxLineNumber = CreateBlocksForBody(
				preferences, classFile, layoutBlockList, false);
		
		if (layoutBlockListLength == layoutBlockList.size())
		{
			// Classe vide. Transformation du bloc 'BodyBlockStartLayoutBlock'
			ibbslb.transformToStartEndBlock();
		}
		else
		{
			InnerTypeBodyBlockEndLayoutBlock ibbelb = 
				new InnerTypeBodyBlockEndLayoutBlock();
			ibbslb.other = ibbelb;
			ibbelb.other = ibbslb;
			layoutBlockList.add(ibbelb);
		}	
		
		return maxLineNumber;
	}
	
	private static int CreateBlocksForBody(
		Preferences preferences, 
		ClassFile classFile, 
		List<LayoutBlock> layoutBlockList,
		boolean displayExtendsOrImplementsFlag)
	{
		CreateBlockForEnumValues(preferences, classFile, layoutBlockList);
		
		List<SubListLayoutBlock> sortedFieldBlockList = 
			CreateSortedBlocksForFields(preferences, classFile);
		List<SubListLayoutBlock> sortedMethodBlockList = 
			CreateSortedBlocksForMethods(preferences, classFile);
		List<SubListLayoutBlock> sortedInnerClassBlockList = 
			CreateSortedBlocksForInnerClasses(preferences, classFile);
	
		return MergeBlocks(
			layoutBlockList, sortedFieldBlockList, 
			sortedMethodBlockList, sortedInnerClassBlockList);
	}
	
	private static void CreateBlockForEnumValues(
		Preferences preferences, 
		ClassFile classFile, 
		List<LayoutBlock> layoutBlockList)
	{
		List<Instruction> values = classFile.getEnumValues();
		
		if (values != null)
		{
			int valuesLength = values.size();
			
			if (valuesLength > 0)
			{
				ConstantPool constants = classFile.getConstantPool();
				Field[] fields = classFile.getFields();
				int fieldsLength = fields.length;
				ArrayList<InvokeNew> enumValues = 
					new ArrayList<InvokeNew>(fieldsLength);
				
		    	InstructionSplitterVisitor visitor = 
			   		new InstructionSplitterVisitor();
					
				// Pour chaque valeur, recherche du l'attribut d'instance, puis 
		    	// du constructeur
		    	for (int i=0; i<valuesLength; i++)
		    	{
		    		GetStatic getStatic = (GetStatic)values.get(i);
		    		ConstantFieldref cfr = 
		    			constants.getConstantFieldref(getStatic.index);
		    		ConstantNameAndType cnat = constants.getConstantNameAndType(
		    			cfr.name_and_type_index);
		    		
		    		int j = fields.length;
		    		
		    		while (j-- > 0)
		    		{
		    			Field field = fields[j];
		    			
		    			if ((field.name_index != cnat.name_index) || 
		    				(field.descriptor_index != cnat.descriptor_index))
		    				continue;
		    			
	    				ValueAndMethod vam = field.getValueAndMethod();
		    			InvokeNew invokeNew = (InvokeNew)vam.getValue();
		    			
		    			invokeNew.transformToEnumValue(getStatic);

	    				enumValues.add(invokeNew);
	    				break;
		    		}
		    	}
		    	
		    	int length = enumValues.size();
		    			
		    	if (length > 0)
		    	{
		    		// Affichage des valeurs
		    		InvokeNew enumValue = enumValues.get(0);
		    		
		    		visitor.start(
		    			preferences, layoutBlockList, classFile, 
		    			classFile.getStaticMethod(), enumValue);   	
			    	visitor.visit(enumValue); 	
			    	visitor.end();
		    		
		    		for (int i=1; i<length; i++)
		    		{
		    			layoutBlockList.add(new FragmentLayoutBlock(
							LayoutBlockConstants.FRAGMENT_COMA_SPACE));
		    			layoutBlockList.add(new SeparatorLayoutBlock(
		    				LayoutBlockConstants.SEPARATOR, 0));
		    			
		    			enumValue = enumValues.get(i);
			    		
			    		visitor.start(
			    			preferences, layoutBlockList, classFile, 
			    			classFile.getStaticMethod(), enumValue);   	
				    	visitor.visit(enumValue); 	
				    	visitor.end();
		    		}
		    		
	    			layoutBlockList.add(new FragmentLayoutBlock(
						LayoutBlockConstants.FRAGMENT_SEMICOLON));
				}
			}
		}
	}
	
	/**
	 * @return liste de sequences de 'LayoutBlock'
	 * Sequence produite pour chaque champ:
	 *  - FieldBlockStartLayoutBlock
	 *  -  CommentDeprecatedLayoutBlock ?
	 *  -  AnnotationsLayoutBlock ?
	 *  -  FieldLayoutBlock
	 *  -  InstructionsLayoutBlock ?
	 *  - FieldBlockEndLayoutBlock
	 */
	private static List<SubListLayoutBlock> CreateSortedBlocksForFields(
		Preferences preferences, ClassFile classFile) 
	{	
		Field[] fields = classFile.getFields();
		
		if (fields == null)
		{
			return Collections.emptyList();
		}
		else
		{
			// Creation des 'FieldLayoutBlock'
			int length = fields.length;
			ArrayList<SubListLayoutBlock> sortedFieldBlockList = 
				new ArrayList<SubListLayoutBlock>(length);
					
	    	InstructionSplitterVisitor visitor = 
	    		new InstructionSplitterVisitor();
		    	
			for (int i=0; i<length; i++)
			{
				Field field = fields[i];
				
		    	if ((field.access_flags & (ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_ENUM)) != 0)
		    		continue;
		    	
		    	ArrayList<LayoutBlock> subLayoutBlockList = 
		    		new ArrayList<LayoutBlock>(6);	
		    	
		    	MarkerLayoutBlock fmslb = new MarkerLayoutBlock(
		    		LayoutBlockConstants.FIELD_MARKER_START, classFile);
		    	subLayoutBlockList.add(fmslb);
		    	
//	    		WriteAttributes(
//	    			spw, referenceMap, classFile, field.getAttributes());
				
				if (field.containsAttributeDeprecated() &&
					!field.containsAnnotationDeprecated(classFile))
				{	    		
					subLayoutBlockList.add(new CommentDeprecatedLayoutBlock());
				}
					
				AnnotationLayouter.CreateBlocksForAnnotations(
					classFile, field.getAttributes(), subLayoutBlockList);    	

				subLayoutBlockList.add(new FieldNameLayoutBlock(classFile, field));
				
				int firstLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		    	int lastLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		    	int preferedLineNumber = LayoutBlockConstants.UNLIMITED_LINE_COUNT;
		    	
			    if (field.getValueAndMethod() != null)
			    {
			    	ValueAndMethod valueAndMethod = field.getValueAndMethod();
			    	Instruction value = valueAndMethod.getValue();
			    	Method method = valueAndMethod.getMethod();

			    	firstLineNumber = value.lineNumber;
			    	lastLineNumber = MaxLineNumberVisitor.visit(value);
			    	preferedLineNumber = lastLineNumber - firstLineNumber;
			    	
			    	// Affichage des instructions d'initialisation des valeurs
			    	visitor.start(
			    		preferences, subLayoutBlockList, classFile, method, value);   	
			    	visitor.visit(value); 	
			    	visitor.end();

			    	subLayoutBlockList.add(new FragmentLayoutBlock(
						LayoutBlockConstants.FRAGMENT_SEMICOLON));
			    }
			    
			    MarkerLayoutBlock fmelb = new MarkerLayoutBlock(
			    	LayoutBlockConstants.FIELD_MARKER_END, classFile);
			    fmslb.other = fmelb;
			    fmelb.other = fmslb;
		    	subLayoutBlockList.add(fmelb);

			    sortedFieldBlockList.add(new SubListLayoutBlock(
			    	LayoutBlockConstants.SUBLIST_FIELD, 
			   		subLayoutBlockList, firstLineNumber,
			   		lastLineNumber, preferedLineNumber));
			}

			return SortBlocks(sortedFieldBlockList);	
		}
	}
	
	/**
	 * @return liste de sequences de 'LayoutBlock'
	 * Sequence produite pour chaque methode:
	 *  - MethodBlockStartLayoutBlock
	 *  -  CommentDeprecatedLayoutBlock ?
	 *  -  AnnotationsLayoutBlock ?
	 *  -  MethodLayoutBlock
	 *  -  ThrowsLayoutBlock ?
	 *  -  StatementsBlockStartLayoutBlock ?
	 *  -   StatementsLayoutBlock *
	 *  -  StatementsBlockEndLayoutBlock ?
	 *  - MethodBlockEndLayoutBlock
	 */
	private static List<SubListLayoutBlock> CreateSortedBlocksForMethods(
		Preferences preferences, ClassFile classFile) 
	{	
		Method[] methods = classFile.getMethods();
		
		if (methods == null)
		{
			return Collections.emptyList();
		}
		else
		{
			// Creation des 'MethodLayoutBlock'
			ConstantPool constants = classFile.getConstantPool();
			boolean multipleConstructorFlag = 
				ClassFileUtil.ContainsMultipleConstructor(classFile);
			int length = methods.length;
			ArrayList<SubListLayoutBlock> sortedMethodBlockList = 
				new ArrayList<SubListLayoutBlock>(length);		
			boolean showDefaultConstructor = 
				preferences.getShowDefaultConstructor();
					
			JavaSourceLayouter javaSourceLayouter = new JavaSourceLayouter();
			
		    for (int i=0; i<length; i++)
			{
		    	Method method = methods[i];
		    	
		    	if ((method.access_flags & 
		    			(ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_BRIDGE)) != 0)
		    		continue;
		    	
	    		AttributeSignature as = method.getAttributeSignature();
	    		
	    		// Le descripteur et la signature sont differentes pour les 
	    		// constructeurs des Enums ! Cette information est passée à 
	    		// "SignatureWriter.WriteMethodSignature(...)".
			    boolean descriptorFlag = (as == null);
	    		int signatureIndex = descriptorFlag ? 
			    		method.descriptor_index : as.signature_index;
			    String signature = constants.getConstantUtf8(signatureIndex);
			    	
		    	if (((classFile.access_flags & ClassFileConstants.ACC_ENUM) != 0) && 
		    		ClassFileUtil.IsAMethodOfEnum(classFile, method, signature))
		    		continue;
		    	
	    		if (method.name_index == constants.instanceConstructorIndex)
		    	{
	    			if (classFile.getInternalAnonymousClassName() != null)
	    				// Ne pas afficher les constructeurs des classes anonymes.
	    				continue;
	    			    			
		    		if ((multipleConstructorFlag == false) &&
		    			((method.getFastNodes() == null) || 
		    			 (method.getFastNodes().size() == 0)))
		    		{
						int[] exceptionIndexes = method.getExceptionIndexes();
						
						if ((exceptionIndexes == null) || 
							(exceptionIndexes.length == 0))
						{
	    					if ((classFile.access_flags & ClassFileConstants.ACC_ENUM) != 0)
	    					{
	    						if (SignatureUtil.GetParameterSignatureCount(signature) == 2)
	    						{
	    							// Ne pas afficher le constructeur par defaut 
	    							// des Enum si il est vide et si c'est le seul 
	    							// constructeur.
	    							continue;
	    						}
	    					}
	    					else if (showDefaultConstructor == false)
	    					{
	    						if (signature.equals("()V"))
					    		{
					    			// Ne pas afficher le constructeur par defaut si 
	    							// il est vide et si c'est le seul constructeur.
					    			continue;
					    		}
	    					}
			    		}
			    	}
		    	}
		    	
	    		if (method.name_index == constants.classConstructorIndex)
		    	{
		    		if ((method.getFastNodes() == null) || 
		    			(method.getFastNodes().size() == 0))
		    			// Ne pas afficher les blocs statiques vides.
		    			continue;
		    	}
	    		
		    	ArrayList<LayoutBlock> subLayoutBlockList = 
		    		new ArrayList<LayoutBlock>(30);		    	

		    	MarkerLayoutBlock mmslb = new MarkerLayoutBlock(
		    		LayoutBlockConstants.METHOD_MARKER_START, classFile);
		    	subLayoutBlockList.add(mmslb);
		    	
//					WriteAttributes(
//						spw, referenceMap, classFile, method.getAttributes());
				
				if (method.containsError())
				{
					subLayoutBlockList.add(new CommentErrorLayoutBlock());
				}
				
				if (method.containsAttributeDeprecated() &&
					!method.containsAnnotationDeprecated(classFile))
				{
					subLayoutBlockList.add(new CommentDeprecatedLayoutBlock());
				}
				
				AnnotationLayouter.CreateBlocksForAnnotations(
					classFile, method.getAttributes(), subLayoutBlockList);
				
				// Information utilisee par 'PrintWriter' pour afficher un ';'
				// apres les methodes sans code. Evite d'instancier un object
				// 'EmptyCodeLayoutBlock'.
				boolean nullCodeFlag = (method.getCode() == null);
				boolean displayThrowsFlag = false;

				if (method.name_index == constants.classConstructorIndex)
				{
					subLayoutBlockList.add(new MethodStaticLayoutBlock(classFile));
				}
				else
				{					
					if (method.getExceptionIndexes() == null)
					{
						subLayoutBlockList.add(new MethodNameLayoutBlock(
							classFile, method, signature, 
							descriptorFlag, nullCodeFlag));							
					}
					else
					{
						subLayoutBlockList.add(new MethodNameLayoutBlock(
							classFile, method, signature, 
							descriptorFlag, false));
							
						subLayoutBlockList.add(new ThrowsLayoutBlock(
							classFile, method, nullCodeFlag));
						
						displayThrowsFlag = true;
					}
				}

		    	int firstLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		    	int lastLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		    	int preferedLineNumber = LayoutBlockConstants.UNLIMITED_LINE_COUNT;
				
				if (nullCodeFlag == false)
				{
					// DEBUG // 					
					if (method.containsError())
					{
						MethodBodyBlockStartLayoutBlock mbbslb = 
							new MethodBodyBlockStartLayoutBlock();
						subLayoutBlockList.add(mbbslb);							
						subLayoutBlockList.add(
							new ByteCodeLayoutBlock(classFile, method));
						MethodBodyBlockEndLayoutBlock mbbelb = 
							new MethodBodyBlockEndLayoutBlock();		
						subLayoutBlockList.add(mbbelb);
						mbbslb.other = mbbelb;
						mbbelb.other = mbbslb;
					}
					// DEBUG // 						
					else
					{
						List<Instruction> list = method.getFastNodes();
						
						MethodBodyBlockStartLayoutBlock mbbslb = 
							new MethodBodyBlockStartLayoutBlock();
						subLayoutBlockList.add(mbbslb);	
						
						int subLayoutBlockListLength = subLayoutBlockList.size();
						boolean singleLine = false;
						
						if (list.size() > 0)
						{
							try
							{
								int beforeIndex = subLayoutBlockList.size();
								singleLine = javaSourceLayouter.createBlocks(
									preferences, subLayoutBlockList, 
									classFile, method, list);
								int afterIndex = subLayoutBlockList.size();
								
								firstLineNumber = SearchFirstLineNumber(
									subLayoutBlockList, beforeIndex, afterIndex);
								lastLineNumber = SearchLastLineNumber(
									subLayoutBlockList, beforeIndex, afterIndex);
							}
							catch (Exception e)
							{
								// DEBUG e.printStackTrace();
								// Erreur durant l'affichage => Retrait de tous 
								// les blocs
								int currentLength = subLayoutBlockList.size();
								while (currentLength > subLayoutBlockListLength)
									subLayoutBlockList.remove(--currentLength);
								
								subLayoutBlockList.add(
									new ByteCodeLayoutBlock(classFile, method));
							}
						}
						
						if (subLayoutBlockListLength == subLayoutBlockList.size())
						{
							// Bloc vide d'instructions. Transformation du bloc 
							// 'StatementBlockStartLayoutBlock'
							if (displayThrowsFlag)
								mbbslb.transformToStartEndBlock(1);
							else
								mbbslb.transformToStartEndBlock(0);
						}
						else if (singleLine)
						{
							mbbslb.transformToSingleLineBlock();
							MethodBodySingleLineBlockEndLayoutBlock mbssbelb = 
								new MethodBodySingleLineBlockEndLayoutBlock();		
							mbbslb.other   = mbssbelb;
							mbssbelb.other = mbbslb;
							subLayoutBlockList.add(mbssbelb);							
						}
						else
						{
							MethodBodyBlockEndLayoutBlock mbbelb = 
								new MethodBodyBlockEndLayoutBlock();		
							mbbslb.other = mbbelb;
							mbbelb.other = mbbslb;
							subLayoutBlockList.add(mbbelb);
						}
					} // if (method.containsError()) else
				} // if (nullCodeFlag == false)
				
				MarkerLayoutBlock mmelb = new MarkerLayoutBlock(
					LayoutBlockConstants.METHOD_MARKER_END, classFile);
				mmslb.other = mmelb;
				mmelb.other = mmslb;
		    	subLayoutBlockList.add(mmelb);

		    	sortedMethodBlockList.add(new SubListLayoutBlock(
		    		LayoutBlockConstants.SUBLIST_METHOD, 
		   			subLayoutBlockList, firstLineNumber,
		    		lastLineNumber, preferedLineNumber));
			}
		    
		    return SortBlocks(sortedMethodBlockList);	
		}
	}
	
	private static List<SubListLayoutBlock> CreateSortedBlocksForInnerClasses(
		Preferences preferences, ClassFile classFile) 
	{	
		ArrayList<ClassFile> innerClassFiles = classFile.getInnerClassFiles();
		
		if (innerClassFiles == null)
		{
			return Collections.emptyList();
		}
		else
		{
			int length = innerClassFiles.size();
	    	ArrayList<SubListLayoutBlock> sortedInnerClassBlockList = 
				new ArrayList<SubListLayoutBlock>(length);
			
			for (int i=0; i<length; i++)
			{
				ClassFile innerClassFile = innerClassFiles.get(i);
				
		    	if (((innerClassFile.access_flags & ClassFileConstants.ACC_SYNTHETIC) != 0) ||
		    		(innerClassFile.getInternalAnonymousClassName() != null))
		    		continue;
		    	
		    	ArrayList<LayoutBlock> innerClassLayoutBlockList = 
					new ArrayList<LayoutBlock>(100);
		    	
		    	CreateBlocksForClass(
		    		preferences, innerClassFile, innerClassLayoutBlockList);
		    	
		    	int afterIndex = innerClassLayoutBlockList.size();
		    	
		    	int firstLineNumber = SearchFirstLineNumber(
		    		innerClassLayoutBlockList, 0, afterIndex);
		    	int lastLineNumber = SearchLastLineNumber(
		    		innerClassLayoutBlockList, 0, afterIndex);
		    	
		    	int preferedLineCount = LayoutBlockConstants.UNLIMITED_LINE_COUNT;
				if ((firstLineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
					(lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER))
				{
					preferedLineCount = lastLineNumber-firstLineNumber;
				}

		    	sortedInnerClassBlockList.add(new SubListLayoutBlock(
		    		LayoutBlockConstants.SUBLIST_INNER_CLASS,
		    		innerClassLayoutBlockList, firstLineNumber,
		    		lastLineNumber, preferedLineCount));
			}
		    
		    return SortBlocks(sortedInnerClassBlockList);
		}
	}
	
	private static int SearchFirstLineNumber(
		List<LayoutBlock> layoutBlockList, int firstIndex, int afterIndex)
	{
		for (int index=firstIndex; index<afterIndex; index++)
		{
			int firstLineNumber = layoutBlockList.get(index).firstLineNumber;
			if (firstLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
				return firstLineNumber;
		}
		
		return Instruction.UNKNOWN_LINE_NUMBER;
	}

	private static int SearchLastLineNumber(
		List<LayoutBlock> layoutBlockList, int firstIndex, int afterIndex)
	{
		while (afterIndex-- > firstIndex)
		{
			int lastLineNumber = layoutBlockList.get(afterIndex).lastLineNumber;
			if (lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
				return lastLineNumber;
		}
		
		return Instruction.UNKNOWN_LINE_NUMBER;
	}
	
	private static List<SubListLayoutBlock> SortBlocks(
		List<SubListLayoutBlock> blockList) 
	{	
		// Detection de l'ordre de generation des champs par le compilateur:
		// ascendant (1), descendant (2) ou aleatoire (3)
		int length = blockList.size();
		int lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		int order = 0;

		for (int i=0; i<length; i++)
		{
			SubListLayoutBlock layoutBlock = blockList.get(i);
			int newLineNumber = layoutBlock.lastLineNumber;
	    
	    	if (newLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
	    	{
	    		if (lineNumber != Instruction.UNKNOWN_LINE_NUMBER)
	    		{
			    	if (order == 0) // Unknown
			    	{
			    		order = (lineNumber < newLineNumber) ? 1 : 2;
			    	}
			    	else if (order == 1) // Asc
			    	{
			    		if (lineNumber > newLineNumber) 
			    		{
			    			order = 3; // Aleatoire
			    			break;
			    		}
			    	}
			    	else if (order == 2) // Desc
			    	{
			    		if (lineNumber < newLineNumber) 
			    		{
			    			order = 3; // Aleatoire
			    			break;
			    		}
			    	}
	    		}

	    		lineNumber = newLineNumber;
	    	}
		}
		
		// Trie
		switch (order)
    	{
    	case 2: // Desc
    		Collections.reverse(blockList);
    		break;
    	case 3: // Aleatoire
    		for (int i=0; i<length; i++)
    		{
    			blockList.get(i).index = i;
    		}
    		// Tri par ordre croissant, les blocs sans numero de ligne 
    		// sont places a la fin.
    		Collections.sort(blockList, new LayoutBlockComparator());
    		break;
    	}
		
		return blockList;	
	}
	/* POURQUOI AVOIR UTILISE UNE SIGNATURE SI COMPLEXE A CONVERTIR EN C++ ?
	 * private static <T extends LayoutBlock> List<T> SortBlocks(List<T> blockList) 
	{	
		// Detection de l'ordre de generation des champs par le compilateur:
		// ascendant (1), descendant (2) ou aleatoire (3)
		int length = blockList.size();
		int lineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		int order = 0;

		for (int i=0; i<length; i++)
		{
			T layoutBlock = blockList.get(i);
			int newLineNumber = layoutBlock.lastLineNumber;
	    
	    	if (newLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
	    	{
	    		if (lineNumber != Instruction.UNKNOWN_LINE_NUMBER)
	    		{
			    	if (order == 0) // Unknown
			    	{
			    		order = (lineNumber < newLineNumber) ? 1 : 2;
			    	}
			    	else if (order == 1) // Asc
			    	{
			    		if (lineNumber > newLineNumber) 
			    		{
			    			order = 3; // Aleatoire
			    			break;
			    		}
			    	}
			    	else if (order == 2) // Desc
			    	{
			    		if (lineNumber < newLineNumber) 
			    		{
			    			order = 3; // Aleatoire
			    			break;
			    		}
			    	}
	    		}

	    		lineNumber = newLineNumber;
	    	}
		}
		
		// Trie
		switch (order)
    	{
    	case 2: // Desc
    		Collections.reverse(blockList);
    		break;
    	case 3: // Aleatoire
    		// Tri par ordre croissant, les blocs sans numero de ligne 
    		// sont places a la fin.
    		Collections.sort(blockList, new LayoutBlockComparator());
    		break;
    	}
		
		return blockList;	
	} */
	
	/* Premiere phase du realignement,
	 * 3 jeux de cartes,
	 * Conserver l'ordre naturel jusqu'a une impossibilite:
	 * Copie des blocs sans numero de ligne des champs au plus tot
	 * Copie des blocs sans numero de ligne des methodes et des classes internes au plus tard
	 */
	private static int MergeBlocks(
		List<LayoutBlock> layoutBlockList, 
		List<SubListLayoutBlock> sortedFieldBlockList, 
		List<SubListLayoutBlock> sortedMethodBlockList, 
		List<SubListLayoutBlock> sortedInnerClassBlockList)
	{
		int maxLineNumber = Instruction.UNKNOWN_LINE_NUMBER;
		
		Collections.reverse(sortedFieldBlockList);
		Collections.reverse(sortedMethodBlockList);
		Collections.reverse(sortedInnerClassBlockList);
		
		// Recherche du bloc ayant un numero de ligne defini
		int minLineNumberMethod = 
			SearchMinimalLineNumber(sortedMethodBlockList);
		int minLineNumberInnerClass = 
			SearchMinimalLineNumber(sortedInnerClassBlockList);

		// Fusion des jeux de cartes
		// 1) Champs
		while (sortedFieldBlockList.size() > 0)
		{
			if (minLineNumberMethod == Instruction.UNKNOWN_LINE_NUMBER)
			{
				if (minLineNumberInnerClass == Instruction.UNKNOWN_LINE_NUMBER)
				{
					// Copie de tout dans l'ordre naturel
					maxLineNumber = MergeFieldBlockList(
						layoutBlockList, sortedFieldBlockList, maxLineNumber);
					break;
				}
				else
				{
					// Copie des champs avec et sans numero de ligne
					maxLineNumber = ExclusiveMergeFieldBlockList(
						layoutBlockList, sortedFieldBlockList, 
						minLineNumberInnerClass, maxLineNumber);
					// Copie de toutes les methodes sans numero de ligne
					maxLineNumber = MergeBlockList(
						layoutBlockList, sortedMethodBlockList, maxLineNumber);
					// Copie des classes internes jusqu'a l'inner classe ayant 
					// le plus petit numero de ligne
					maxLineNumber = InclusiveMergeBlockList(
						layoutBlockList, sortedInnerClassBlockList, 
						minLineNumberInnerClass, maxLineNumber);
					minLineNumberInnerClass = 
						SearchMinimalLineNumber(sortedInnerClassBlockList);
				}
			}
			else
			{
				if ((minLineNumberInnerClass == Instruction.UNKNOWN_LINE_NUMBER) ||
					(minLineNumberMethod < minLineNumberInnerClass))
				{
					// Copie des champs avec et sans numero de ligne
					maxLineNumber = ExclusiveMergeFieldBlockList(
						layoutBlockList, sortedFieldBlockList, 
						minLineNumberMethod, maxLineNumber);
					// Copie des methodes jusqu'a la methode ayant le plus 
					// petit numero de ligne
					maxLineNumber = InclusiveMergeBlockList(
						layoutBlockList, sortedMethodBlockList,
						minLineNumberMethod, maxLineNumber);
					minLineNumberMethod = 
						SearchMinimalLineNumber(sortedMethodBlockList);
				}
				else
				{
					// Copie des champs avec et sans numero de ligne
					maxLineNumber = ExclusiveMergeFieldBlockList(
						layoutBlockList, sortedFieldBlockList, 
						minLineNumberInnerClass, maxLineNumber);
					// Copie des methodes avec et sans numero de ligne
					maxLineNumber = ExclusiveMergeMethodOrInnerClassBlockList(
						layoutBlockList, sortedMethodBlockList, 
						minLineNumberInnerClass, maxLineNumber);
					// Copie des classes internes jusqu'a l'inner classe ayant 
					// le plus petit numero de ligne
					maxLineNumber = InclusiveMergeBlockList(
						layoutBlockList, sortedInnerClassBlockList, 
						minLineNumberInnerClass, maxLineNumber);
					minLineNumberInnerClass = 
						SearchMinimalLineNumber(sortedInnerClassBlockList);
				}
			}
		}
		
		// 2) Methodes
		while (sortedMethodBlockList.size() > 0)
		{
			if (minLineNumberInnerClass == Instruction.UNKNOWN_LINE_NUMBER)
			{
				maxLineNumber = MergeBlockList(
					layoutBlockList, sortedMethodBlockList, maxLineNumber);
				break;
			}
			else
			{
				// Copie des methodes avec et sans numero de ligne
				maxLineNumber = ExclusiveMergeMethodOrInnerClassBlockList(
					layoutBlockList, sortedMethodBlockList, 
					minLineNumberInnerClass, maxLineNumber);
				// Copie des classes internes jusqu'a l'inner classe ayant le 
				// plus petit numero de ligne
				maxLineNumber = InclusiveMergeBlockList(
					layoutBlockList, sortedInnerClassBlockList, 
					minLineNumberInnerClass, maxLineNumber);
				minLineNumberInnerClass = 
					SearchMinimalLineNumber(sortedInnerClassBlockList);
			}
		}
		
		// 3) Classes internes
		maxLineNumber = MergeBlockList(
			layoutBlockList, sortedInnerClassBlockList, maxLineNumber);
		
		return maxLineNumber;
	}
	
	private static int ExclusiveMergeMethodOrInnerClassBlockList(
		List<LayoutBlock> destination, 
		List<SubListLayoutBlock> source,
		int minLineNumber, int maxLineNumber)
	{
		byte lastTag = destination.get(destination.size()-1).tag;
		int index = source.size();

		while (index > 0)
		{
			SubListLayoutBlock sllb = source.get(index-1);
			int lineNumber = sllb.lastLineNumber;
			
			if ((lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
				(lineNumber >= minLineNumber))
				break;
			
			// Add separator
			switch (lastTag)
			{
			case LayoutBlockConstants.FIELD_MARKER_END:
				destination.add(
					new SeparatorLayoutBlock(LayoutBlockConstants.SEPARATOR, 1));
				break;
//			case LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK:
			case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
			case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:				
			case LayoutBlockConstants.STATEMENTS_BLOCK_START:
			case LayoutBlockConstants.SWITCH_BLOCK_START:
			case LayoutBlockConstants.SEPARATOR:
				break;
			default:
				destination.add(
					new SeparatorLayoutBlock(LayoutBlockConstants.SEPARATOR, 2));
				break;
			}

			// Move item
			destination.addAll(sllb.subList);
			
			// Store last line number
			int lastLineNumber = sllb.lastLineNumber;			
			if (lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				if ((maxLineNumber == Instruction.UNKNOWN_LINE_NUMBER) ||
					(maxLineNumber < lastLineNumber))
				{
					maxLineNumber = lastLineNumber;
				}
			}

			source.remove(--index);

			// Store last tag
			lastTag = LayoutBlockConstants.UNDEFINED;
		}
		
		return maxLineNumber;
	}
	
	private static int ExclusiveMergeFieldBlockList(
		List<LayoutBlock> destination, 
		List<SubListLayoutBlock> source,
		int minLineNumber, int maxLineNumber)
	{
		byte lastTag = destination.get(destination.size()-1).tag;
		int index = source.size();

		while (index > 0)
		{
			SubListLayoutBlock sllb = source.get(index-1);
			int lineNumber = sllb.lastLineNumber;
			
			if ((lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
				(lineNumber >= minLineNumber))
				break;

			// Add separator
			switch (lastTag)
			{
			case LayoutBlockConstants.FIELD_MARKER_END:
				destination.add(
					new SeparatorLayoutBlock(LayoutBlockConstants.SEPARATOR, 1));
				break;
//			case LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK:
			case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
			case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:				
			case LayoutBlockConstants.STATEMENTS_BLOCK_START:
			case LayoutBlockConstants.SWITCH_BLOCK_START:
			case LayoutBlockConstants.SEPARATOR:
				break;
			default:
				destination.add(
					new SeparatorLayoutBlock(LayoutBlockConstants.SEPARATOR, 2));
				break;
			}

			// Move item
			source.remove(--index);
			destination.addAll(sllb.subList);
			
			// Store last line number
			int lastLineNumber = sllb.lastLineNumber;			
			if (lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				if ((maxLineNumber == Instruction.UNKNOWN_LINE_NUMBER) ||
					(maxLineNumber < lastLineNumber))
				{
					maxLineNumber = lastLineNumber;
				}
			}
			
			// Store last tag
			lastTag = LayoutBlockConstants.FIELD_MARKER_END;
		}
		
		return maxLineNumber;
	}
		
	private static int InclusiveMergeBlockList(
		List<LayoutBlock> destination, 
		List<SubListLayoutBlock> source,
		int minLineNumber, int maxLineNumber)
	{
		byte lastTag = destination.get(destination.size()-1).tag;
		int index = source.size();
		
		// Deplacement
		while (index > 0)
		{
			SubListLayoutBlock sllb = source.get(index-1);
			int lineNumber = sllb.lastLineNumber;
			
			if ((lineNumber != Instruction.UNKNOWN_LINE_NUMBER) &&
				(lineNumber > minLineNumber))
				break;

			// Add separator
			switch (lastTag)
			{
//			case LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK:
			case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
			case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_BLOCK_START:
			case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:
			case LayoutBlockConstants.STATEMENTS_BLOCK_START:
			case LayoutBlockConstants.SWITCH_BLOCK_START:
			case LayoutBlockConstants.SEPARATOR:
				break;
			default:
				destination.add(
					new SeparatorLayoutBlock(LayoutBlockConstants.SEPARATOR, 2));
				break;
			}

			// Move item
			destination.addAll(sllb.subList);
			
			// Store last line number
			int lastLineNumber = sllb.lastLineNumber;			
			if (lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				if ((maxLineNumber == Instruction.UNKNOWN_LINE_NUMBER) ||
					(maxLineNumber < lastLineNumber))
				{
					maxLineNumber = lastLineNumber;
				}
			}

			source.remove(--index);

			if (lineNumber == minLineNumber)
				break;
			
			// Store last tag
			lastTag = LayoutBlockConstants.UNDEFINED;
		}
		
		return maxLineNumber;
	}

	private static int MergeBlockList(
		List<LayoutBlock> destination, 
		List<SubListLayoutBlock> source, 
		int maxLineNumber)
	{
		byte lastTag = destination.get(destination.size()-1).tag;
		int index = source.size();
		
		while (index-- > 0)
		{
			// Add separator
			switch (lastTag)
			{
//			case LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK:
			case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
			case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_BLOCK_START:
			case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:
			case LayoutBlockConstants.STATEMENTS_BLOCK_START:
			case LayoutBlockConstants.SWITCH_BLOCK_START:
			case LayoutBlockConstants.SEPARATOR:
				break;
			default:
				destination.add(new SeparatorLayoutBlock(
					LayoutBlockConstants.SEPARATOR, 2));
				break;
			}

			// Move item
			SubListLayoutBlock sllb = source.remove(index);
			destination.addAll(sllb.subList);
			
			// Store last line number
			int lastLineNumber = sllb.lastLineNumber;			
			if (lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				if ((maxLineNumber == Instruction.UNKNOWN_LINE_NUMBER) ||
					(maxLineNumber < lastLineNumber))
				{
					maxLineNumber = lastLineNumber;
				}
			}

			// Store last tag
			lastTag = LayoutBlockConstants.UNDEFINED;
		}
		
		return maxLineNumber;
	}
	
	private static int MergeFieldBlockList(
		List<LayoutBlock> destination, 
		List<SubListLayoutBlock> source,
		int maxLineNumber)
	{
		byte lastTag = destination.get(destination.size()-1).tag;
		int index = source.size();
		
		while (index-- > 0)
		{
			// Add separator
			switch (lastTag)
			{
			case LayoutBlockConstants.FIELD_MARKER_END:
				destination.add(
					new SeparatorLayoutBlock(LayoutBlockConstants.SEPARATOR, 1));
				break;
//			case LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK:
			case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
			case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_BLOCK_START:				
			case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:				
			case LayoutBlockConstants.STATEMENTS_BLOCK_START:
			case LayoutBlockConstants.SWITCH_BLOCK_START:
			case LayoutBlockConstants.SEPARATOR:
				break;
			default:
				destination.add(new SeparatorLayoutBlock(
					LayoutBlockConstants.SEPARATOR, 2));
				break;
			}

			// Move item
			SubListLayoutBlock sllb = source.remove(index);
			destination.addAll(sllb.subList);	

			// Store last line number
			int lastLineNumber = sllb.lastLineNumber;			
			if (lastLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				if ((maxLineNumber == Instruction.UNKNOWN_LINE_NUMBER) ||
					(maxLineNumber < lastLineNumber))
				{
					maxLineNumber = lastLineNumber;
				}
			}
			
			// Store last tag
			lastTag = LayoutBlockConstants.FIELD_MARKER_END;
		}
		
		return maxLineNumber;
	}

	/*
	 * La liste est classee en ordre inverse
	 */
	private static int SearchMinimalLineNumber(List<? extends LayoutBlock> list)
	{
		int index = list.size();

		while (index-- > 0)
		{
			int lineNumber = list.get(index).lastLineNumber;
			if (lineNumber != Instruction.UNKNOWN_LINE_NUMBER)
				return lineNumber;
		}
		
		return Instruction.UNKNOWN_LINE_NUMBER;
	}
	
	private static void LayoutBlocks(ArrayList<LayoutBlock> layoutBlockList)
	{
		// DEBUG // long time0 = System.currentTimeMillis();
		
		// Initialize
		ArrayList<LayoutSection> layoutSectionList = 
			new ArrayList<LayoutSection>();
		
		CreateSections(layoutBlockList, layoutSectionList);			
		InitializeBlocks(layoutBlockList, layoutSectionList);
				
		int layoutCount = 20;
		
		do
		{
			// Layout
			LayoutSections(layoutBlockList, layoutSectionList);
			
			// Score
			ScoreSections(layoutBlockList, layoutSectionList);
			
			// Slice
			if (SliceDownBlocks(layoutBlockList, layoutSectionList) == false)
				break;
			
			ResetLineCounts(layoutBlockList, layoutSectionList);
		}
		while (layoutCount-- > 0);
		
		// DEBUG // System.err.println("LayoutBlocks: Nbr de boucles: " + (20-layoutCount));

		layoutCount = 20;
		
		do
		{
			// Layout
			LayoutSections(layoutBlockList, layoutSectionList);
			
			// Score
			ScoreSections(layoutBlockList, layoutSectionList);
			
			// Slice
			if (SliceUpBlocks(layoutBlockList, layoutSectionList) == false) 
				break;
			
			ResetLineCounts(layoutBlockList, layoutSectionList);
		}
		while (layoutCount-- > 0);
		
		// DEBUG // System.err.println("LayoutBlocks: Nbr de boucles: " + (20-layoutCount));

		// DEBUG // long time1 = System.currentTimeMillis();
		// DEBUG // System.err.println("LayoutBlocks: Temps: " + (time1-time0) + "ms");
	}
	
	private static void CreateSections(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList)
	{
		int blockLength = layoutBlockList.size();
		
		// Layout
		int layoutSectionListSize = 0;
		int firstBlockIndex = 0;
		int firstLineNumber = 1;
		boolean containsError = false;
		
		for (int blockIndex=1; blockIndex<blockLength; blockIndex++)
		{
			LayoutBlock lb = layoutBlockList.get(blockIndex);
			
			if (lb.tag == LayoutBlockConstants.BYTE_CODE)
			{
				containsError = true;
			}
			
			if (lb.firstLineNumber != Instruction.UNKNOWN_LINE_NUMBER)
			{
				if (firstLineNumber > lb.firstLineNumber)
					containsError = true;
				layoutSectionList.add(new LayoutSection(
					layoutSectionListSize++,
					firstBlockIndex, blockIndex-1, 
					firstLineNumber, lb.firstLineNumber, 
					containsError));
				firstBlockIndex = blockIndex+1;
				firstLineNumber = lb.lastLineNumber;
				containsError = false;
			}
		}
		
		if (firstBlockIndex < blockLength-1)
		{
			layoutSectionList.add(new LayoutSection(
				layoutSectionListSize++,
				firstBlockIndex, blockLength-1, 
				firstLineNumber, Instruction.UNKNOWN_LINE_NUMBER, 
				containsError));
		}
	}

	private static void InitializeBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList)
	{
		// Initialize indexes & sections
		int blockIndex = 0;
		int sectionLength = layoutSectionList.size();

		for (int sectionIndex=0; sectionIndex<sectionLength; sectionIndex++)
		{
			LayoutSection section = layoutSectionList.get(sectionIndex);			
			int lastBlockIndex = section.lastBlockIndex;
			
			for (blockIndex = section.firstBlockIndex; 
			     blockIndex <= lastBlockIndex; 
			     blockIndex++)
			{
				LayoutBlock lb = layoutBlockList.get(blockIndex);
				lb.index = blockIndex;
				lb.section = section;
			}
		}
	}
	
	private static void ResetLineCounts(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList)
	{
		// Initialize indexes & sections
		int sectionLength = layoutSectionList.size();
		
		for (int sectionIndex=0; sectionIndex<sectionLength; sectionIndex++)
		{
			LayoutSection section = layoutSectionList.get(sectionIndex);	
			
			if (section.relayout)
			{
				int lastBlockIndex = section.lastBlockIndex;
				
				for (int blockIndex = section.firstBlockIndex; 
				         blockIndex <= lastBlockIndex; 
				         blockIndex++)
				{
					LayoutBlock lb = layoutBlockList.get(blockIndex);
					lb.lineCount = lb.preferedLineCount;
				}
			}
		}
	}

	private static void LayoutSections(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList)
	{
		// Layout sections
		int sectionLength = layoutSectionList.size();
		
		if (sectionLength > 0)
		{
			sectionLength--;
			
			int layoutCount = 5;
			boolean redo;
	
			do
			{
				redo = false;
				
				// Mise en page avec heuristiques
				for (int sectionIndex=0; sectionIndex<sectionLength; sectionIndex++)
				{
					LayoutSection section = layoutSectionList.get(sectionIndex);

					if (section.relayout && !section.containsError)
					{
						section.relayout = false;
						
						int originalLineCount = section.originalLineCount;
						int currentLineCount = GetLineCount(
							layoutBlockList, section.firstBlockIndex, section.lastBlockIndex);
						
						if (originalLineCount > currentLineCount)
						{
							ExpandBlocksWithHeuristics(
								layoutBlockList, section.firstBlockIndex, section.lastBlockIndex, 
								originalLineCount-currentLineCount);
							redo = true;
						}
						else if (currentLineCount > originalLineCount)
						{
							CompactBlocksWithHeuristics(
								layoutBlockList, section.firstBlockIndex, section.lastBlockIndex, 
								currentLineCount-originalLineCount);
							redo = true;
						}
					}
				}
				
				// Pas de mise en page de la derniere section
				layoutSectionList.get(sectionLength).relayout = false;
			}
			while (redo && (layoutCount-- > 0));
			
			// Derniere mise en page si les precedentes tentatives ont echouees
			if (redo)
			{
				for (int sectionIndex=0; sectionIndex<sectionLength; sectionIndex++)
				{
					LayoutSection section = layoutSectionList.get(sectionIndex);
					
					if (section.relayout && !section.containsError)
					{
						section.relayout = false;
						
						int originalLineCount = section.originalLineCount;
						int currentLineCount = GetLineCount(
							layoutBlockList, section.firstBlockIndex, section.lastBlockIndex);
						
						if (originalLineCount > currentLineCount)
						{
							ExpandBlocks(
								layoutBlockList, section.firstBlockIndex, section.lastBlockIndex, 
								originalLineCount-currentLineCount);
						}
						else if (currentLineCount > originalLineCount)
						{
							CompactBlocks(
								layoutBlockList, section.firstBlockIndex, section.lastBlockIndex, 
								currentLineCount-originalLineCount);
						}
					}
				}
				
				// Pas de mise en page de la derniere section
				layoutSectionList.get(sectionLength).relayout = false;
			}
		}
	}
	
	private static int GetLineCount(
		ArrayList<LayoutBlock> layoutBlockList, int firstIndex, int lastIndex)
	{
		int sum = 0;
		
		for (int index=firstIndex; index<=lastIndex; index++)
		{
			int lineCount = layoutBlockList.get(index).lineCount;
			if (lineCount != LayoutBlockConstants.UNLIMITED_LINE_COUNT)
				sum += lineCount;
		}
		
		return sum;
	}
			
	private static void CompactBlocksWithHeuristics(
		ArrayList<LayoutBlock> layoutBlockList, 
		int firstIndex, int lastIndex, int delta)
	{
		int oldDelta;

		do
		{
			oldDelta = delta;

			// Compact separator
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.SEPARATOR:
				case LayoutBlockConstants.SEPARATOR_OF_STATEMENTS:
					if (lb.lineCount > 2)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}			
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;

			// Compact implements & throws
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.IMPLEMENTS_INTERFACES:
				case LayoutBlockConstants.GENERIC_IMPLEMENTS_INTERFACES:
				case LayoutBlockConstants.THROWS:
					if (lb.lineCount > 0)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}

			// Compact extends
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.EXTENDS_SUPER_TYPE:
				case LayoutBlockConstants.EXTENDS_SUPER_INTERFACES:
				case LayoutBlockConstants.GENERIC_EXTENDS_SUPER_TYPE:
				case LayoutBlockConstants.GENERIC_EXTENDS_SUPER_INTERFACES:
					if (lb.lineCount > 0)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
		
		// Compact imports
		for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
		{
			LayoutBlock lb = layoutBlockList.get(i);
			
			switch (lb.tag)
			{
			case LayoutBlockConstants.IMPORTS:
				if (lb.lineCount > 0)
				{
					if (lb.lineCount >= delta)
					{
						lb.lineCount -= delta;
						delta = 0;
					}
					else
					{
						delta -= lb.lineCount;
						lb.lineCount = 0;
					}
				}
				break;
			}
		}
		
		do
		{
			oldDelta = delta;
			
			// Compact debut de bloc des methodes
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.STATEMENTS_BLOCK_START:
				case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_START:
				case LayoutBlockConstants.SWITCH_BLOCK_START:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_START:
				case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
					if ((lb.lineCount > 1) && 
						(lb.lineCount > lb.minimalLineCount))
					{
						lb.lineCount--;
						delta--;
					}
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
			
		do
		{
			oldDelta = delta;

			// Compact separator
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				/* TENTATIVE case LayoutBlockConstants.SEPARATOR: */
				case LayoutBlockConstants.SEPARATOR_OF_STATEMENTS:
					if (lb.lineCount > 1)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}			
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;
			
			// Compact fin de bloc des methodes
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.STATEMENTS_BLOCK_END:
				case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END:
				case LayoutBlockConstants.SWITCH_BLOCK_END:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_END:
				case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_END:
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_END:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_END:
					if ((lb.lineCount > 1) && 
						(lb.lineCount > lb.minimalLineCount))
					{
						// Compact end block 
						lb.lineCount--;
						delta--;
					}
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
		
		do
		{
			oldDelta = delta;

			// Compact separator
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.FRAGMENT_CASE:
				case LayoutBlockConstants.FRAGMENT_CASE_ENUM:
				case LayoutBlockConstants.FRAGMENT_CASE_STRING:
					if (lb.lineCount > 0)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
					
		do
		{
			oldDelta = delta;
			
			// Compact fin de bloc des methodes
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END:
					if (lb.lineCount > lb.minimalLineCount)
					{
						// Compact end block 
						lb.lineCount--;
						delta--;
					}
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
		
		do
		{
			oldDelta = delta;

			// Compact separator
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.CASE_BLOCK_START:
				case LayoutBlockConstants.CASE_BLOCK_END:
					if (lb.lineCount > 0)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
		
		do
		{
			oldDelta = delta;
			
			// Compact fin de bloc des methodes
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:
				case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_END:
					if (lb.lineCount > lb.minimalLineCount)
					{
						BlockLayoutBlock blb = (BlockLayoutBlock)lb;
						
						// Compact end block 
						lb.lineCount--;
						delta--;
	
						if (lb.lineCount <= 1)
						{
							// Compact start block 
							if (blb.section == blb.other.section)
							{
								if (blb.other.lineCount > delta)
								{
									blb.other.lineCount -= delta;
									delta = 0;
								}
								else
								{
									delta -= blb.other.lineCount;
									blb.other.lineCount = 0;
								}
							}
							else
							{
								blb.other.section.relayout = true;
								blb.other.lineCount = 0;
							}
						}
					}
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;
			
			// Compact debut de bloc des methodes
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.STATEMENTS_BLOCK_START:
				case LayoutBlockConstants.SWITCH_BLOCK_START:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_START:
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
					if (lb.lineCount > lb.minimalLineCount)
					{
						// Compact start block 
						lb.lineCount--;
						delta--;
					}
					break;
				case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_START:
					if (lb.lineCount > lb.minimalLineCount)
					{
						// Compact start block 
						lb.lineCount--;
						delta--;
						
						if (lb.lineCount == 0)
						{
							BlockLayoutBlock blb = (BlockLayoutBlock)lb;
							
							// Compact end block 
							if (blb.section == blb.other.section)
							{
								if (blb.other.lineCount > delta)
								{
									blb.other.lineCount -= delta;
									delta = 0;
								}
								else
								{
									delta -= blb.other.lineCount;
									blb.other.lineCount = 0;
								}
							}
							else
							{
								blb.other.section.relayout = true;
								blb.other.lineCount = 0;
							}
						}
					}
					break;
				}
			}
			
			// Compact fin de bloc des methodes
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.STATEMENTS_BLOCK_END:
				case LayoutBlockConstants.SWITCH_BLOCK_END:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_END:
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_END:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_END:
					if (lb.lineCount > lb.minimalLineCount)
					{
//						BlockLayoutBlock blb = (BlockLayoutBlock)lb;
						
						// Compact end block 
						lb.lineCount--;
						delta--;
	
//						if (lb.lineCount <= 1)
//						{
//							// Compact start block 
//							if (blb.section == blb.other.section)
//							{
//								if (blb.other.lineCount > delta)
//								{
//									blb.other.lineCount -= delta;
//									delta = 0;
//								}
//								else
//								{
//									delta -= blb.other.lineCount;
//									blb.other.lineCount = 0;
//								}
//							}
//							else
//							{
//								blb.other.section.relayout = true;
//								blb.other.lineCount = 0;
//							}
//						}
					}
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
		
		do
		{
			oldDelta = delta;

			// Compact separator
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.SEPARATOR:
				case LayoutBlockConstants.SEPARATOR_OF_STATEMENTS:
					if (lb.lineCount > 0)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}			
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;

			// Compact separator
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.COMMENT_ERROR:
					if (lb.lineCount > 0)
					{
						lb.lineCount--;
						delta--;
					}
					break;
				}
			}			
		}
		while ((delta>0) && (oldDelta>delta));

//		// Si les heuristiques n'ont pas ete suffisantes...
//		do
//		{
//			oldDelta = delta;
//
//			// Compact block
//			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
//			{
//				LayoutBlock lb = layoutBlockList.get(i);
//
//				if (lb.lineCount > lb.minimalLineCount)
//				{
//					lb.lineCount--;
//					delta--;
//				}
//			}
//		}
//		while ((delta>0) && (oldDelta>delta));
	}
		
	private static void ExpandBlocksWithHeuristics(
		ArrayList<LayoutBlock> layoutBlockList, 
		int firstIndex, int lastIndex, int delta)
	{
		int oldDelta;

		do
		{
			oldDelta = delta;
			
			// Expand "implements types"
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.IMPLEMENTS_INTERFACES:
				case LayoutBlockConstants.EXTENDS_SUPER_INTERFACES:
				case LayoutBlockConstants.GENERIC_IMPLEMENTS_INTERFACES:
				case LayoutBlockConstants.GENERIC_EXTENDS_SUPER_INTERFACES:
					if (lb.lineCount < lb.maximalLineCount)
					{
						lb.lineCount++;
						delta--;
					}
					break;
				}
			}

			// Expand "extends super type"
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.EXTENDS_SUPER_TYPE:
				case LayoutBlockConstants.GENERIC_EXTENDS_SUPER_TYPE:
					if (lb.lineCount < lb.maximalLineCount)
					{
						lb.lineCount++;
						delta--;
					}
					break;
				}
			}
			
			// Expand separator after imports
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.SEPARATOR_AT_BEGINING:
				case LayoutBlockConstants.SEPARATOR_AFTER_IMPORTS:
					lb.lineCount += delta;
					delta = 0;
					break;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
			
		do
		{
			oldDelta = delta;
			
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.FOR_BLOCK_START:
					if (lb.lineCount < lb.maximalLineCount)
					{
						lb.lineCount++;
						delta--;
					}
					break;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;
			
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.CASE_BLOCK_END:
					if (lb.lineCount == 0)
					{
						lb.lineCount++;
						delta--;
					}
					break;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;
			
			// Expand fin de bloc des methodes
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{					
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_END:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_END:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_END:
				case LayoutBlockConstants.STATEMENTS_BLOCK_END: // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
				case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END:
				case LayoutBlockConstants.SWITCH_BLOCK_END:
					if (lb.lineCount == 0)
					{
						BlockLayoutBlock blb = (BlockLayoutBlock)lb;
						
						// Expand end block 
						lb.lineCount++;
						delta--;
						
						// Expand start block 
						if (blb.other.lineCount == 0)
						{
							if (blb.section == blb.other.section)
							{
								if (delta > 0)
								{
									blb.other.lineCount++;
									delta--;
								}
							}
							else
							{
								blb.other.section.relayout = true;
								blb.other.lineCount = 1;
							}
						}
					}
				}
			}
				
			// Expand debut de bloc du corps des classes internes
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_START:
				case LayoutBlockConstants.STATEMENTS_BLOCK_START:
				case LayoutBlockConstants.SWITCH_BLOCK_START:
					if (lb.lineCount == 0)
					{
						BlockLayoutBlock blb = (BlockLayoutBlock)lb;
						
						// Expand start block 
						lb.lineCount++;
						delta--;						
						// Expand end block 
						if (blb.section == blb.other.section)
						{
							int d = 2 - blb.other.lineCount;
							
							if (d > delta)
							{
								blb.other.lineCount += delta;
								delta = 0;
							}
							else
							{
								delta -= d;
								blb.other.lineCount = 2;
							}
						}
						else
						{
							blb.other.section.relayout = true;
							blb.other.lineCount = 2;
						}
					}
				}
			}	
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;
				
			// Expand separator 1
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.SEPARATOR:
				case LayoutBlockConstants.SEPARATOR_OF_STATEMENTS:
					{
						lb.lineCount++;
						delta--;
					}
					break;
				}
			}			
		}
		while ((delta>0) && (oldDelta>delta));
	
		do
		{
			oldDelta = delta;
			
			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.CASE_BLOCK_END:
					if (lb.lineCount < lb.maximalLineCount)
					{
						lb.lineCount++;
						delta--;
					}
					break;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));

		do
		{
			oldDelta = delta;
			
			// Compact fin de bloc des methodes
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{					
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_END:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_END:
				case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_END:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_END:
				case LayoutBlockConstants.STATEMENTS_BLOCK_END:
				case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END:
				case LayoutBlockConstants.SWITCH_BLOCK_END:
					if (lb.lineCount < lb.maximalLineCount)
					{
	//					BlockLayoutBlock blb = (BlockLayoutBlock)lb;
						
						// Expand end block 
						lb.lineCount++;
						delta--;
//						if (delta < 2)
//						{
//							lb.lineCount += delta;
//							delta = 0;
//						}
//						else
//						{
//							delta -= 2 - lb.lineCount;
//							lb.lineCount = 2;
//						}
						
	//					// Expand start block 
	//					if (blb.other.lineCount == 0)
	//					{
	//						if (blb.section == blb.other.section)
	//						{
	//							if (delta > 0)
	//							{
	//								blb.other.lineCount++;
	//								delta--;
	//							}
	//						}
	//						else
	//						{
	//							blb.other.section.relayout = true;
	//							blb.other.lineCount = 1;
	//						}
	//					}
					}
				}
			}
				
			// Expand debut de bloc du corps des classes internes
			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				switch (lb.tag)
				{
				case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:
				case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
				case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:
				case LayoutBlockConstants.METHOD_BODY_BLOCK_START:
				case LayoutBlockConstants.STATEMENTS_BLOCK_START:
				case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_START:
				case LayoutBlockConstants.SWITCH_BLOCK_START:
					if (lb.lineCount < lb.maximalLineCount)
					{
						BlockLayoutBlock blb = (BlockLayoutBlock)lb;
						
						// Expand start block 
						lb.lineCount++;
						delta--;
						
						if ((lb.lineCount > 1) && (blb.other.lineCount == 0))
						{
							// Expand end block 
							if (blb.section == blb.other.section) // yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy
							{
								if (delta > 0)
								{
									blb.other.lineCount = 1;
									delta--;
								}
							}
							else
							{
								blb.other.section.relayout = true;
								blb.other.lineCount = 1;
							}
						}
					}
				}
			}	
		}
		while ((delta>0) && (oldDelta>delta));
			
//		do
//		{
//			oldDelta = delta;
//
//			// Expand 
//			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
//			{
//				LayoutBlock lb = layoutBlockList.get(i);
//				
//				if (lb.lineCount < lb.maximalLineCount)
//				{
//					lb.lineCount++;
//					delta--;
//				}
//			}
//		}
//		while ((delta>0) && (oldDelta>delta));
	}
	
	private static void CompactBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		int firstIndex, int lastIndex, int delta)
	{
		int oldDelta;
		
		do
		{
        	oldDelta = delta;

			for (int i=lastIndex; (i>=firstIndex) && (delta>0); i--)
			{
				LayoutBlock lb = layoutBlockList.get(i);

				if (lb.lineCount > lb.minimalLineCount)
				{
					lb.lineCount--;
					delta--;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
	}
	
	private static void ExpandBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		int firstIndex, int lastIndex, int delta)
	{
		int oldDelta;
		
		do
		{
			oldDelta = delta;

			for (int i=firstIndex; (i<=lastIndex) && (delta>0); i++)
			{
				LayoutBlock lb = layoutBlockList.get(i);
				
				if (lb.lineCount < lb.maximalLineCount)
				{
					lb.lineCount++;
					delta--;
				}
			}
		}
		while ((delta>0) && (oldDelta>delta));
	}

	/*
	 * Score = sum(
	 * - (separator.lineCount)^2
	 * + (sum(block.lineCount==0))
	 * )
	 */
	private static void ScoreSections(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList)
	{
		int sectionLength = layoutSectionList.size();
		
		if (sectionLength > 0)
		{
			sectionLength--;
		
			for (int sectionIndex=0; sectionIndex<sectionLength; sectionIndex++)
			{
				LayoutSection section = layoutSectionList.get(sectionIndex);			
				int lastBlockIndex = section.lastBlockIndex;
				int score = 0;
				int sumScore = 0;
				
				for (int blockIndex = section.firstBlockIndex; 
				     blockIndex <= lastBlockIndex; 
				     blockIndex++)
				{
					LayoutBlock lb = layoutBlockList.get(blockIndex);
				
					switch (lb.tag)
					{
					case LayoutBlockConstants.SEPARATOR:
						if (lb.lineCount < lb.preferedLineCount)
						{
							sumScore += lb.preferedLineCount-lb.lineCount;	
							
							if (lb.lineCount > 0)
							{
								score += sumScore*sumScore;
								sumScore = 0;
							}
						}
						else if (lb.lineCount > lb.preferedLineCount)
						{
							int delta = lb.lineCount - lb.preferedLineCount;
							score -= delta*delta;
						}
					}
				}
	
				score += sumScore*sumScore;
				
				// DEBUG // System.err.println("score = " + score);
				section.score = score;
			}
		}
		
		// DEBUG // System.err.println();
	}
	
	/**
	 * @param layoutBlockList
	 * @param layoutSectionList
	 * @return true si des bloques ont ete deplaces
	 */
	private static boolean SliceDownBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList)
	{
		// Identifier la section avec le plus haut score c.a.d. la section
		// sur laquelle il faut relacher des contraintes.	
		int sectionLenght = layoutSectionList.size();

		ArrayList<LayoutSection> sortedLayoutSectionList = 
			new ArrayList<LayoutSection>(sectionLenght);
		sortedLayoutSectionList.addAll(layoutSectionList);
		
		Collections.sort(sortedLayoutSectionList);
		
		for (int sectionSourceIndex = 0; 
		     sectionSourceIndex < sectionLenght; 
		     sectionSourceIndex++)
		{
			// Section source
			LayoutSection lsSource = 
				sortedLayoutSectionList.get(sectionSourceIndex);
			
			if (lsSource.score <= 0)
				break;
			
			if (SliceDownBlocks(
					layoutBlockList, layoutSectionList, 
					sectionSourceIndex, lsSource))
				return true;
		}
	
		return false;
	}

	private static boolean SliceUpBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList)
	{
		// Identifier la section avec le plus haut score c.a.d. la section
		// sur laquelle il faut relacher des contraintes.	
		int sectionLenght = layoutSectionList.size();

		ArrayList<LayoutSection> sortedLayoutSectionList = 
			new ArrayList<LayoutSection>(sectionLenght);
		sortedLayoutSectionList.addAll(layoutSectionList);
		
		Collections.sort(sortedLayoutSectionList);
		
		for (int sectionSourceIndex = 0; 
		     sectionSourceIndex < sectionLenght; 
		     sectionSourceIndex++)
		{
			// Section source
			LayoutSection lsSource = 
				sortedLayoutSectionList.get(sectionSourceIndex);
			
			if (lsSource.score <= 0)
				break;
			
			if (SliceUpBlocks(
					layoutBlockList, layoutSectionList, 
					sectionSourceIndex, lsSource))
				return true;
		}
	
		return false;
	}	
	
	/**
	 * @param layoutBlockList
	 * @param layoutSectionList
	 * @param lsSource
	 * @return true si des bloques ont ete deplaces
	 */
	private static boolean SliceDownBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList,
		int sectionSourceIndex, LayoutSection lsSource)
	{
		// Slice down. Detect type of last block
		int firstBlockIndex = lsSource.firstBlockIndex;
		int blockIndex;
		
		for (blockIndex = lsSource.lastBlockIndex; 
		     blockIndex >= firstBlockIndex; 
		     blockIndex--)
		{
			LayoutBlock lb = layoutBlockList.get(blockIndex);
			
			switch (lb.tag)
			{
			case LayoutBlockConstants.TYPE_MARKER_START:
				// Found				
				// Slice last method block
				if (SliceDownBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.METHOD_MARKER_START, 
						LayoutBlockConstants.METHOD_MARKER_END))
					return true;	
				// Slice last field block
				if (SliceDownBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.FIELD_MARKER_START, 
						LayoutBlockConstants.FIELD_MARKER_END))
					return true;

				break;
			case LayoutBlockConstants.FIELD_MARKER_START:
				// Found
				// Slice last inner class block
				if (SliceDownBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.TYPE_MARKER_START, 
						LayoutBlockConstants.TYPE_MARKER_END))
					return true;
				// Slice last method block
				if (SliceDownBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.METHOD_MARKER_START,  
						LayoutBlockConstants.METHOD_MARKER_END))
					return true;

				break;
			case LayoutBlockConstants.METHOD_MARKER_START:
				// Found
				// Slice last inner class block
				if (SliceDownBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.TYPE_MARKER_START, 
						LayoutBlockConstants.TYPE_MARKER_END))
					return true;
				// Slice last field block
				if (SliceDownBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.FIELD_MARKER_START,  
						LayoutBlockConstants.FIELD_MARKER_END))
					return true;

				break;
			}
		}
		
		return false;
	}
	
	/**
	 * @param layoutBlockList
	 * @param layoutSectionList
	 * @param sectionSourceIndex
	 * @param firstBlockIndex
	 * @param blockIndex
	 * @param markerStartTag
	 * @param markerEndTag
	 * @return true si des bloques ont ete deplaces
	 */
	private static boolean SliceDownBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList,
		int sectionSourceIndex, int blockIndex, 
		LayoutSection lsSource, int markerStartTag, int markerEndTag)
	{
		// Rechercher le dernier block de type 'tag'
		int firstBlockIndex = lsSource.firstBlockIndex;
		
		while (firstBlockIndex < blockIndex)
		{
			LayoutBlock lb = layoutBlockList.get(--blockIndex);
			
			if (lb.tag == markerEndTag)
			{
				// Tag de marqueur de fin trouvé.
				MarkerLayoutBlock mlb = (MarkerLayoutBlock)lb;
				
				if (mlb.section != mlb.other.section)
				{
					// Le marqueur de début et le marqueur de fin ne font pas
					// parti de la même section -> l'un des blocs entre eux deux 
					// a un numero de ligne connu -> blocs indeplacables.
					return false;
				}
				if (mlb.other.index <= firstBlockIndex)
				{
					// Le marqueur de debut est avant la limite.
					return false;
				}
							
				// Trouvé. 
				
				// -- 1 ----------------------------------------------------- //
				int lastEndTagBlockIndex = blockIndex;
				
				// Rechercher du nombre de blocs a deplacer => 
				//  Trouver le premier block de type 'tag' sans numero de ligne
				int counter = 1;
				
				blockIndex = mlb.other.index;

				while (firstBlockIndex < blockIndex)
				{
					lb = layoutBlockList.get(--blockIndex);
					
					if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_START) /* ||
						(lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK) */)
					{
						break;
					}
					else if (lb.tag == markerEndTag)
					{
						// Tag de marqueur de fin trouvé.
						mlb = (MarkerLayoutBlock)lb;
						
						if (mlb.section != mlb.other.section)
						{
							// Le marqueur de début et le marqueur de fin ne font pas
							// parti de la même section -> l'un des blocs entre eux deux 
							// a un numero de ligne connu -> blocs indeplacables.
							break;
						}					
						if (mlb.other.index <= firstBlockIndex)
						{
							// Le marqueur de debut est avant la limite.
							break;
						}

						counter++;
						blockIndex = mlb.other.index;
					}
					else if ((lb.tag == LayoutBlockConstants.FIELD_MARKER_END) ||
							 (lb.tag == LayoutBlockConstants.METHOD_MARKER_END) ||
							 (lb.tag == LayoutBlockConstants.TYPE_MARKER_END))
					{
						break;
					}
				}
				
				// Un ou plusieurs blocs a deplacer trouvés.
				
				// Rechercher de l'index d'insertion =>
				//  Trouver la section ayant le score le plus bas jusqu'a la 
				//  section contenant un block de type 'tag' ayant un numero 
				//  de ligne defini
				int blockLenght = layoutBlockList.size();
				blockIndex = lastEndTagBlockIndex;
					
				int lowerScore = lsSource.score;
				int lowerScoreBlockIndex = blockIndex;
				
				while (++blockIndex < blockLenght)
				{
					lb = layoutBlockList.get(blockIndex);
				
					if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_END) ||
//						(lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK) ||
						(lb.tag == markerStartTag))
					{
						// Fin de corps ou debut d'un bloc
						if (lowerScore > lb.section.score)
						{
							lowerScore = lb.section.score;							
							lowerScoreBlockIndex = blockIndex;	
						}
						
						// Impossible de deplacer un bloc au dessus
						// - d'une fin de corps
						// - d'un autre du meme type
						// => On s'arrete.
						break;
					}
					else if ((lb.tag == LayoutBlockConstants.FIELD_MARKER_START) ||
							 (lb.tag == LayoutBlockConstants.METHOD_MARKER_START) ||
							 (lb.tag == LayoutBlockConstants.TYPE_MARKER_START))
					{
						// Debut d'un bloc d'un type different
						if ((lb.section != null) && 
							(lowerScore > lb.section.score))
						{
							lowerScore = lb.section.score;							
							lowerScoreBlockIndex = blockIndex;	
						}								
						
						blockIndex = ((MarkerLayoutBlock)lb).other.index;
					}
				}
					
				if (lowerScore != lsSource.score)
				{
					// Trouvé. 
					
					// -- 2 ------------------------------------------------- //

					// Rechercher de l'index de debut du bloc de type 'tag'
					// counter/2 en partant de 'lastEndTagBlockIndex'
					counter = (counter + 1) / 2;
					int firstStartTagBlockIndex = 
							blockIndex = lastEndTagBlockIndex;
					
					while (firstBlockIndex < blockIndex)
					{
						lb = layoutBlockList.get(blockIndex);
						
						if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_START) /* ||
							(lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
						{
							break;
						}
						else if (lb.tag == markerEndTag)
						{							
							firstStartTagBlockIndex = blockIndex =
								((MarkerLayoutBlock)lb).other.index;
						
							if (--counter == 0)
								break;
						}	
						
						blockIndex--;
					}
					
					// Trouvé.
					
					// -- 3 ------------------------------------------------- //

					// Deplacer la moitier des blocks de type 'tag' de 
					// 'firstStartTagBlockIndex' a 'lastEndTagBlockIndex'
					// vers 'lowerScoreBlockIndex'
					
					LayoutBlock insertionLayoutBlock = 
						layoutBlockList.get(lowerScoreBlockIndex);
					LayoutSection lsTarget = insertionLayoutBlock.section;
					
					// Remove blocks
					int sourceDeltaIndex = 
						lastEndTagBlockIndex - firstStartTagBlockIndex + 1;
					ArrayList<LayoutBlock> layoutBlockListToMove = 
						new ArrayList<LayoutBlock>(sourceDeltaIndex);
					
					for (blockIndex=lastEndTagBlockIndex; 
					     blockIndex>=firstStartTagBlockIndex; 
					     blockIndex--)
					{
						lb = layoutBlockList.remove(blockIndex);
						
						// Update section attribute
						lb.section = lsTarget;
						
						layoutBlockListToMove.add(lb);
					}
					
					Collections.reverse(layoutBlockListToMove);
 
					// Remove separator after blocks if exists
					if (layoutBlockList.get(blockIndex+1).tag == 
						LayoutBlockConstants.SEPARATOR)
					{
						layoutBlockList.remove(blockIndex+1);
						sourceDeltaIndex++;					}
					
					// Modify separator brefore blocks if exists
					if (layoutBlockList.get(blockIndex).tag == 
						LayoutBlockConstants.SEPARATOR)
					{
						layoutBlockList.get(blockIndex).preferedLineCount = 2;
					}
					
					// Blocs pas encore inserés.
					lowerScoreBlockIndex -= sourceDeltaIndex;
					
					int targetDeltaIndex = 0;
					
					if ((insertionLayoutBlock.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_END) /*||
						(insertionLayoutBlock.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
					{
						// Insert new separator before blocks
						int preferedLineCount = 2;
						
						if ((markerEndTag == LayoutBlockConstants.FIELD_MARKER_END) &&
							(layoutBlockList.get(lowerScoreBlockIndex-1).tag == 
								LayoutBlockConstants.FIELD_MARKER_END))
						{
							preferedLineCount = 1;
						}
						
						layoutBlockList.add(
							lowerScoreBlockIndex, 
							new SeparatorLayoutBlock(
								LayoutBlockConstants.SEPARATOR, 
								preferedLineCount));
						
						targetDeltaIndex++;
					}
					else
					{
						// Update separator before blocks
						LayoutBlock beforeLayoutBlock = 
							layoutBlockList.get(lowerScoreBlockIndex-1);
						
						int preferedLineCount = 2;
						
						if ((markerEndTag == LayoutBlockConstants.FIELD_MARKER_END) &&
							(layoutBlockList.get(lowerScoreBlockIndex-2).tag == 
								LayoutBlockConstants.FIELD_MARKER_END))
						{
							preferedLineCount = 1;
						}
						
						beforeLayoutBlock.preferedLineCount = preferedLineCount;
					}
					
					// Insert blocks			
					int layoutBlockListToMoveSize = layoutBlockListToMove.size();
					
					layoutBlockList.addAll(
						lowerScoreBlockIndex+targetDeltaIndex, 
						layoutBlockListToMove);
					
					targetDeltaIndex += layoutBlockListToMoveSize;

					// Add separator after blocks
					if ((insertionLayoutBlock.tag != LayoutBlockConstants.TYPE_BODY_BLOCK_END) /*&&
						(insertionLayoutBlock.tag != LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
					{
						int preferedLineCount = 2;
						
						if (markerStartTag == LayoutBlockConstants.FIELD_MARKER_START)
						{
							preferedLineCount = 1;
						}
						
						layoutBlockList.add(
							lowerScoreBlockIndex+targetDeltaIndex, 
							new SeparatorLayoutBlock(
								LayoutBlockConstants.SEPARATOR, 
								preferedLineCount));
						
						targetDeltaIndex++;
					}
			
					// -- 4 ------------------------------------------------- //

					// Update indexes of sections
					lsSource.lastBlockIndex -= sourceDeltaIndex;
					
					for (int sectionIndex=lsSource.index+1; 
					         sectionIndex<=lsTarget.index-1; 
					         sectionIndex++)
					{
						LayoutSection ls = layoutSectionList.get(sectionIndex);
						ls.firstBlockIndex -= sourceDeltaIndex;
						ls.lastBlockIndex  -= sourceDeltaIndex;
					}
					
					lsTarget.firstBlockIndex -= sourceDeltaIndex;
									
					int delta = sourceDeltaIndex - targetDeltaIndex;
					
					if (delta != 0)
					{
						lsTarget.lastBlockIndex -= delta;

						// Update indexes of last sections
						for (int sectionIndex=layoutSectionList.size()-1; 
						         sectionIndex>lsTarget.index; 
						         sectionIndex--)
						{
							LayoutSection ls = layoutSectionList.get(sectionIndex);
							ls.firstBlockIndex -= delta;
							ls.lastBlockIndex  -= delta;
						}
					}
					
					// Update index of blocks
					blockLenght = layoutBlockList.size();
					
					for (blockIndex=firstStartTagBlockIndex; 
					     blockIndex<blockLenght; 
					     blockIndex++)
					{
						layoutBlockList.get(blockIndex).index = blockIndex;
					}
					
					// Update relayout flag of sections
					UpdateRelayoutFlag(layoutBlockList, lsSource);
					UpdateRelayoutFlag(layoutBlockList, lsTarget);

					return true;
				}
				
				break;
			}
			else if ((lb.tag == LayoutBlockConstants.FIELD_MARKER_END) ||
					 (lb.tag == LayoutBlockConstants.METHOD_MARKER_END) ||
					 (lb.tag == LayoutBlockConstants.TYPE_MARKER_END))
			{
				blockIndex = ((MarkerLayoutBlock)lb).other.index;
			}
			else if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_START) /*||
					 (lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
			{
				break;
			}	
		}
		
		return false;
	}

	private static boolean SliceUpBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList,
		int sectionSourceIndex, LayoutSection lsSource)
	{
		// Slice up. Detect type of last block
		int lastBlockIndex = lsSource.lastBlockIndex;
		int blockIndex;

		for (blockIndex = lsSource.firstBlockIndex; 
		     blockIndex <= lastBlockIndex; 
		     blockIndex++)
		{
			LayoutBlock lb = layoutBlockList.get(blockIndex);
			
			switch (lb.tag)
			{
			case LayoutBlockConstants.TYPE_MARKER_END:
				// Found				
				// Slice last method block
				// Slice last field block
				if (SliceUpBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.FIELD_MARKER_START, 
						LayoutBlockConstants.FIELD_MARKER_END))
					return true;
				if (SliceUpBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.METHOD_MARKER_START, 
						LayoutBlockConstants.METHOD_MARKER_END))
					return true;	

				return false;
			case LayoutBlockConstants.FIELD_MARKER_END:
				// Found
				// Slice last method block
				if (SliceUpBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.METHOD_MARKER_START,  
						LayoutBlockConstants.METHOD_MARKER_END))
					return true;
				// Slice last inner class block
				if (SliceUpBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.TYPE_MARKER_START, 
						LayoutBlockConstants.TYPE_MARKER_END))
					return true;

				return false;
			case LayoutBlockConstants.METHOD_MARKER_END:
				// Found
				// Slice last field block
				if (SliceUpBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.FIELD_MARKER_START,  
						LayoutBlockConstants.FIELD_MARKER_END))
					return true;
				// Slice last inner class block
				if (SliceUpBlocks(
						layoutBlockList, layoutSectionList, 
						sectionSourceIndex, blockIndex, lsSource,
						LayoutBlockConstants.TYPE_MARKER_START, 
						LayoutBlockConstants.TYPE_MARKER_END))
					return true;

				return false;			
			}
		}
		
		return false;
	}

	/**
	 * @param layoutBlockList
	 * @param layoutSectionList
	 * @param sectionSourceIndex
	 * @param firstBlockIndex
	 * @param blockIndex
	 * @param markerStartTag
	 * @param markerEndTag
	 * @return true si des bloques ont ete deplaces
	 */
	private static boolean SliceUpBlocks(
		ArrayList<LayoutBlock> layoutBlockList, 
		ArrayList<LayoutSection> layoutSectionList,
		int sectionSourceIndex, int blockIndex, 
		LayoutSection lsSource, int markerStartTag, int markerEndTag)
	{
		// Rechercher le premier block de type 'tag'
		int lastBlockIndex = lsSource.lastBlockIndex;
		
		while (blockIndex < lastBlockIndex)
		{
			LayoutBlock lb = layoutBlockList.get(++blockIndex);
			
			if (lb.tag == markerStartTag)
			{
				// Tag de marqueur de debut trouvé.
				MarkerLayoutBlock mlb = (MarkerLayoutBlock)lb;
				
				if (mlb.section != mlb.other.section)
				{
					// Le marqueur de début et le marqueur de fin ne font pas
					// parti de la même section -> l'un des blocs entre eux deux 
					// a un numero de ligne connu -> blocs indeplacables.
					return false;
				}
				if (mlb.other.index >= lastBlockIndex)
				{
					// Le marqueur de fin est apres la limite.
					return false;
				}
							
				// Trouvé. 
				
				// -- 1 ----------------------------------------------------- //
				int firstStartTagBlockIndex = blockIndex;

				// Rechercher du nombre de blocs a deplacer => 
				//  Trouver le dernier block de type 'tag' sans numero de ligne
				int counter = 1;
				
				blockIndex = mlb.other.index;

				while (blockIndex < lastBlockIndex)
				{
					lb = layoutBlockList.get(++blockIndex);
					
					if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_END) /*||
						(lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
					{
						break;
					}
					else if (lb.tag == markerStartTag)
					{
						// Tag de marqueur de fin trouvé.
						mlb = (MarkerLayoutBlock)lb;
						
						if (mlb.section != mlb.other.section)
						{
							// Le marqueur de début et le marqueur de fin ne font pas
							// parti de la même section -> l'un des blocs entre eux deux 
							// a un numero de ligne connu -> blocs indeplacables.
							break;
						}					
						if (mlb.other.index >= lastBlockIndex)
						{
							// Le marqueur de debut est avant la limite.
							break;
						}

						counter++;
						blockIndex = mlb.other.index;
					}
					else if ((lb.tag == LayoutBlockConstants.FIELD_MARKER_START) ||
							 (lb.tag == LayoutBlockConstants.METHOD_MARKER_START) ||
							 (lb.tag == LayoutBlockConstants.TYPE_MARKER_START))
					{
						break;
					}
				}
				
				// Un ou plusieurs blocs a deplacer trouvés.
				
				// Rechercher de l'index d'insertion =>
				//  Trouver la section ayant le score le plus bas jusqu'a la 
				//  section contenant un block de type 'tag' ayant un numero 
				//  de ligne defini
				blockIndex = firstStartTagBlockIndex;
					
				int lowerScore = lsSource.score;
				int lowerScoreBlockIndex = blockIndex;
				
				while (blockIndex-- > 0)
				{
					lb = layoutBlockList.get(blockIndex);
				
					if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_START) ||
//						(lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK) ||
						(lb.tag == markerEndTag))
					{
						// debut de corps ou fin d'un bloc
						if (lowerScore > lb.section.score)
						{
							lowerScore = lb.section.score;							
							lowerScoreBlockIndex = blockIndex;	
						}
						
						// Impossible de deplacer un bloc au dessus
						// - d'un debut de corps
						// - d'un autre du meme type
						// => On s'arrete.
						break;
					}
					else if ((lb.tag == LayoutBlockConstants.FIELD_MARKER_END) ||
							 (lb.tag == LayoutBlockConstants.METHOD_MARKER_END) ||
							 (lb.tag == LayoutBlockConstants.TYPE_MARKER_END))
					{
						// Fin d'un bloc d'un type different
						if ((lb.section != null) && 
							(lowerScore > lb.section.score))
						{
							lowerScore = lb.section.score;							
							lowerScoreBlockIndex = blockIndex;	
						}								
						
						blockIndex = ((MarkerLayoutBlock)lb).other.index;
					}
				}
					
				if (lowerScore != lsSource.score)
				{
					// Trouvé. 
					
					// -- 2 ------------------------------------------------- //
					
					// Rechercher de l'index de debut du bloc de type 'tag'
					// counter/2 en partant de 'lastEndTagBlockIndex'
					counter = (counter + 1) / 2;
					int lastEndTagBlockIndex = 
							blockIndex = firstStartTagBlockIndex;
					
					while (blockIndex > 0)
					{
						lb = layoutBlockList.get(blockIndex);
						
						if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_END) /*||
							(lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
						{
							break;
						}
						else if (lb.tag == markerStartTag)
						{							
							lastEndTagBlockIndex = blockIndex =
								((MarkerLayoutBlock)lb).other.index;
						
							if (--counter == 0)
								break;
						}	
						
						blockIndex++;
					}
					
					// Trouvé.
					
					// -- 3 ------------------------------------------------- //
					
					// Deplacer la moitier des blocks de type 'tag' de 
					// 'firstStartTagBlockIndex' a 'lastEndTagBlockIndex'
					// vers 'lowerScoreBlockIndex'
					
					LayoutBlock insertionLayoutBlock = 
						layoutBlockList.get(lowerScoreBlockIndex);
					LayoutSection lsTarget = insertionLayoutBlock.section;
					
					// Remove blocks
					int sourceDeltaIndex = 
						lastEndTagBlockIndex - firstStartTagBlockIndex + 1;
					ArrayList<LayoutBlock> layoutBlockListToMove = 
						new ArrayList<LayoutBlock>(sourceDeltaIndex);
					
					for (blockIndex=lastEndTagBlockIndex; 
					     blockIndex>=firstStartTagBlockIndex; 
					     blockIndex--)
					{
						lb = layoutBlockList.remove(blockIndex);
						
						// Update section attribute
						lb.section = lsTarget;
						
						layoutBlockListToMove.add(lb);
					}
					
					Collections.reverse(layoutBlockListToMove);
 
					// Remove separator after blocks if exists
					if (layoutBlockList.get(blockIndex+1).tag == 
						LayoutBlockConstants.SEPARATOR)
					{
						layoutBlockList.remove(blockIndex+1);
						sourceDeltaIndex++;
					}
					
					// Modify separator brefore blocks if exists
					if (layoutBlockList.get(blockIndex).tag == 
						LayoutBlockConstants.SEPARATOR)
					{
						layoutBlockList.get(blockIndex).preferedLineCount = 2;
					}
					
					// Blocs pas encore inserés.
					lowerScoreBlockIndex++;

					int targetDeltaIndex = 0;
											
					if ((insertionLayoutBlock.tag != LayoutBlockConstants.TYPE_BODY_BLOCK_START) /*&& 
						(insertionLayoutBlock.tag != LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
					{
						// Insert new separator before blocks
						int preferedLineCount = 2;
						
						if (markerEndTag == LayoutBlockConstants.FIELD_MARKER_END)
						{
							preferedLineCount = 1;
						}
						
						layoutBlockList.add(
							lowerScoreBlockIndex, 
							new SeparatorLayoutBlock(
								LayoutBlockConstants.SEPARATOR, 
								preferedLineCount));
						
						targetDeltaIndex++;
					}
					
					// Insert blocks			
					int layoutBlockListToMoveSize = layoutBlockListToMove.size();
					
					layoutBlockList.addAll(
						lowerScoreBlockIndex+targetDeltaIndex, 
						layoutBlockListToMove);

					targetDeltaIndex += layoutBlockListToMoveSize;
					
					// Update separator after blocks
					if ((insertionLayoutBlock.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_START) /*|| 
						(insertionLayoutBlock.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
					{
						// Insert new separator after blocks
						int preferedLineCount = 2;
						
						if ((markerEndTag == LayoutBlockConstants.FIELD_MARKER_END) &&
							(layoutBlockList.get(lowerScoreBlockIndex+targetDeltaIndex).tag == 
								LayoutBlockConstants.FIELD_MARKER_END))
						{
							preferedLineCount = 1;
						}
						
						layoutBlockList.add(
							lowerScoreBlockIndex+targetDeltaIndex, 
							new SeparatorLayoutBlock(
								LayoutBlockConstants.SEPARATOR, 
								preferedLineCount));
						
						targetDeltaIndex++;
					}
					else
					{
						// Update separator after blocks
						LayoutBlock afterLayoutBlock = 
							layoutBlockList.get(lowerScoreBlockIndex+targetDeltaIndex);
						
						int preferedLineCount = 2;
						
						if ((markerStartTag == LayoutBlockConstants.FIELD_MARKER_START) &&
							(layoutBlockList.get(lowerScoreBlockIndex+targetDeltaIndex+1).tag == 
								LayoutBlockConstants.FIELD_MARKER_START))
						{
							preferedLineCount = 1;
						}
						
						afterLayoutBlock.preferedLineCount = preferedLineCount;
					}
					
					// -- 4 ------------------------------------------------- //

					// Update indexes of sections
					lsTarget.lastBlockIndex += targetDeltaIndex;
					
					for (int sectionIndex=lsTarget.index+1; 
					         sectionIndex<=lsSource.index-1; 
					         sectionIndex++)
					{
						LayoutSection ls = layoutSectionList.get(sectionIndex);
						ls.firstBlockIndex += targetDeltaIndex;
						ls.lastBlockIndex  += targetDeltaIndex;
					}
					
					lsSource.firstBlockIndex += targetDeltaIndex;

					int delta = sourceDeltaIndex - targetDeltaIndex;
					
					if (delta != 0)
					{
						lsSource.lastBlockIndex -= delta;
						
						// Update indexes of last sections
						for (int sectionIndex=layoutSectionList.size()-1; 
						         sectionIndex>lsSource.index; 
						         sectionIndex--)
						{
							LayoutSection ls = layoutSectionList.get(sectionIndex);
							ls.firstBlockIndex -= delta;
							ls.lastBlockIndex  -= delta;
						}	
					}
					
					// Update index of blocks
					int blockLenght = layoutBlockList.size();
					
					for (blockIndex=lowerScoreBlockIndex; 
					     blockIndex<blockLenght; 
					     blockIndex++)
					{
						layoutBlockList.get(blockIndex).index = blockIndex;
					}
					
					// Update relayout flag of sections
					UpdateRelayoutFlag(layoutBlockList, lsSource);
					UpdateRelayoutFlag(layoutBlockList, lsTarget);

					return true;
				}
				
				break;
			}
			else if ((lb.tag == LayoutBlockConstants.FIELD_MARKER_START) ||
					 (lb.tag == LayoutBlockConstants.METHOD_MARKER_START) ||
					 (lb.tag == LayoutBlockConstants.TYPE_MARKER_START))
			{
				blockIndex = ((MarkerLayoutBlock)lb).other.index;
			}
			else if ((lb.tag == LayoutBlockConstants.TYPE_BODY_BLOCK_END) /*||
					 (lb.tag == LayoutBlockConstants.SEPARATOR_BEFORE_OR_AFTER_BLOCK)*/)
			{
				break;
			}	
		}
		
		return false;
	}
	
	private static void UpdateRelayoutFlag(
		ArrayList<LayoutBlock> layoutBlockList, LayoutSection section)
	{
		section.relayout = true;
		
		int lastBlockIndex = section.lastBlockIndex;
		
		for (int blockIndex=section.firstBlockIndex; 
		         blockIndex<lastBlockIndex; 
		         blockIndex++)
		{
			LayoutBlock block = layoutBlockList.get(blockIndex);
			
			switch (block.tag) 
			{
			case LayoutBlockConstants.TYPE_BODY_BLOCK_START:
			case LayoutBlockConstants.TYPE_BODY_BLOCK_END:
			case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START:
			case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_END:
			case LayoutBlockConstants.METHOD_BODY_BLOCK_START:
			case LayoutBlockConstants.METHOD_BODY_BLOCK_END:
			case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START:
			case LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_END:
			case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_START:
			case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END:
			case LayoutBlockConstants.STATEMENTS_BLOCK_START:
			case LayoutBlockConstants.STATEMENTS_BLOCK_END:
			case LayoutBlockConstants.SWITCH_BLOCK_START:
			case LayoutBlockConstants.SWITCH_BLOCK_END:
				BlockLayoutBlock blb = (BlockLayoutBlock)block;
				LayoutSection otherSection = blb.other.section;
				if (otherSection.relayout == false)
					UpdateRelayoutFlag(layoutBlockList, otherSection);
				break;
			}
		}
	}	
}
