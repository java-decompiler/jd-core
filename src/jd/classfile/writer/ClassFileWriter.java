package jd.classfile.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import jd.Constants;
import jd.Preferences;
import jd.classfile.ClassFile;
import jd.classfile.ConstantPool;
import jd.classfile.Field;
import jd.classfile.Method;
import jd.classfile.Field.ValueAndLocalVariables;
import jd.classfile.analyzer.SignatureAnalyzer;
import jd.classfile.attribute.Attribute;
import jd.classfile.attribute.AttributeCode;
import jd.classfile.attribute.AttributeEnclosingMethod;
import jd.classfile.attribute.AttributeSignature;
import jd.classfile.attribute.AttributeSourceFile;
import jd.classfile.attribute.UnknowAttribute;
import jd.classfile.constant.ConstantFieldref;
import jd.classfile.constant.ConstantNameAndType;
import jd.classfile.constant.ConstantValue;
import jd.instruction.bytecode.instruction.GetStatic;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.InvokeNew;
import jd.instruction.bytecode.writer.ByteCodeWriter;
import jd.instruction.bytecode.writer.JavaCodeWriter;
import jd.instruction.fast.visitor.FastSourceWriterVisitor;
import jd.instruction.fast.writer.FastWriter;
import jd.printer.Printer;
import jd.util.Reference;
import jd.util.ReferenceMap;



public class ClassFileWriter 
{
	private static HashSet<String> keywords;
	
	public static void Write(
		Preferences preferences, Printer spw, 
		ReferenceMap referenceMap, ClassFile classFile)
	{
		spw.startComment();
		
		// Affichage du nom du fichier		
		spw.print(Printer.UNKNOWN_LINE_NUMBER, "// '");
		spw.print(Printer.UNKNOWN_LINE_NUMBER, classFile.getThisClassName());
		spw.print(Printer.UNKNOWN_LINE_NUMBER, ".class'");

		// Affichage de la version du compilateur
		spw.print(Printer.UNKNOWN_LINE_NUMBER, ", version ");
		spw.print(Printer.UNKNOWN_LINE_NUMBER, classFile.getMajorVersion());
		spw.print(Printer.UNKNOWN_LINE_NUMBER, '.');
		spw.print(Printer.UNKNOWN_LINE_NUMBER, classFile.getMinorVersion());
		
		spw.endComment();

		// Affichage du package
		String internalPackageName = classFile.getInternalPackageName();
		if ((internalPackageName != null) && (internalPackageName.length() > 0))
		{
			spw.startPackageStatement();
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "package ");
			spw.print(Printer.UNKNOWN_LINE_NUMBER, internalPackageName
				           .replace(Constants.INTERNAL_PACKAGE_SEPARATOR, 
						            Constants.PACKAGE_SEPARATOR));
			spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
			spw.endPackageStatement();
		}
		
		// Affichage des imports
		WriteImports(spw, referenceMap, classFile);

		// Affichage de la classe
		WriteClass(preferences, spw, referenceMap, classFile);
	}

	// --------------------------------------------------------------------- //

	private static void WriteClass(
		Preferences preferences, Printer spw, 
		ReferenceMap referenceMap, ClassFile classFile)
	{
		spw.startClassDeclaration(classFile.getThisClassName());
		
		WriteHeader(spw, referenceMap, classFile);
		WriteBody(preferences, spw, referenceMap, classFile);
		
		spw.endClassDeclaration();
	}
	
	/*
	 * Call by "SourceWriterVisitor.writeInvokeNewInstruction(InvokeNew in)"
	 */
	public static void WriteBody(
		Preferences preferences, Printer spw, 
		ReferenceMap referenceMap, ClassFile classFile)
	{
		spw.startStatementBlock();
		
		WriteEnumValues(preferences, spw, referenceMap, classFile);
		WriteFields(preferences, spw, referenceMap, classFile);
		WriteMethods(preferences, spw, referenceMap, classFile);
		WriteInnerClasses(preferences, spw, referenceMap, classFile);
		
		spw.endStatementBlock();
	}
		
	// --------------------------------------------------------------------- //
	
	private static void WriteImports(
		Printer spw, ReferenceMap referenceMap, ClassFile classFile)
	{
		String internalPackageName = classFile.getInternalPackageName();
		
		Collection<Reference> collection = referenceMap.values();
		String[] internalReferenceNames = new String[collection.size()];	
		Iterator<Reference> iterator = collection.iterator();
		
		int length = internalReferenceNames.length;
		
		if (length > 0)
		{
			boolean showImports = false;
			
			for (int i=0; i<internalReferenceNames.length; i++)
				internalReferenceNames[i] = iterator.next().getInternalName();
			Arrays.sort(internalReferenceNames);
	
			for (int i=0; i<internalReferenceNames.length; i++)
			{
				String internalReferencePackageName = 
					InternalClassNameToInternalPackageName(internalReferenceNames[i]);
				
				// No import for same package classes
				if (internalReferencePackageName.equals(internalPackageName))
				{
					if (showImports == false)
					{
						spw.startImportStatements();
						showImports = true;
					}

					spw.startImportStatement();
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "////// ");
					spw.print(Printer.UNKNOWN_LINE_NUMBER, 
						InternalClassNameToQualifiedClassName(internalReferenceNames[i]));
					spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
					spw.endImportStatement();
					continue;
				}
				
				// No import for 'java/lang' classes
				if (internalReferencePackageName.equals(
						Constants.INTERNAL_JAVA_LANG_PACKAGE_NAME))
				{
					if (showImports == false)
					{
						spw.startImportStatements();
						showImports = true;
					}

					spw.startImportStatement();
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "////// ");
					spw.print(Printer.UNKNOWN_LINE_NUMBER, 
						InternalClassNameToQualifiedClassName(internalReferenceNames[i]));
					spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
					spw.endImportStatement();
					continue;
				}
	
				if (showImports == false)
				{
					spw.startImportStatements();
					showImports = true;
				}

				spw.startImportStatement();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "import ");
				spw.print(Printer.UNKNOWN_LINE_NUMBER, 
					InternalClassNameToQualifiedClassName(internalReferenceNames[i]));
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
				spw.endImportStatement();
			}
			
			if (showImports)
				spw.endImportStatements();			
		}
	}

	private static void WriteInterfaces(
			Printer spw, ReferenceMap referenceMap, ClassFile classFile)
	{
		int[] interfaceIndexes = classFile.getInterfaces();
		
		if (interfaceIndexes != null) 
		{
			ConstantPool constants = classFile.getConstantPool();
			spw.startClassDeclarationImplements();

			spw.print(Printer.UNKNOWN_LINE_NUMBER, " implements ");
			String signature = 
				'L' + constants.getConstantClassName(interfaceIndexes[0]) + ';';
			SignatureWriter.WriteSimpleSignature(
				spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, 
				classFile, signature);
			
			for(int i=1; i<interfaceIndexes.length; i++) 
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ", ");
				signature = 
					'L' + constants.getConstantClassName(interfaceIndexes[i]) + ';';
				SignatureWriter.WriteSimpleSignature(
					spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, 
					classFile, signature);
			}
			
			spw.endClassDeclarationImplements();
		} 						
	}
	
	private static void WriteHeader(
		Printer spw, ReferenceMap referenceMap, ClassFile classFile)
	{
		if (classFile.containsAttributeDeprecated() &&
			!classFile.containsAnnotationDeprecated(classFile))
		{
			spw.startJavadoc();
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "/**");
			spw.endOfLineJavadoc();
			spw.print(Printer.UNKNOWN_LINE_NUMBER, " * @deprecated");
			spw.endOfLineJavadoc();
			spw.print(Printer.UNKNOWN_LINE_NUMBER, " */");
			spw.endJavadoc();
		}
				
		// Affichage des attributs de la classe
		WriteAttributes(
			spw, referenceMap, classFile, classFile.getAttributes());

		// Affichage des annotations de la classe
		AnnotationWriter.WriteAttributeAnnotations(
				spw, referenceMap, classFile, classFile.getAttributes());
		
		// Affichage de la classe, de l'interface, de l'enum ou de l'annotation		
		 // Check annotation
		if ((classFile.access_flags & Constants.ACC_ANNOTATION) != 0)
		{
			// Retrait du flags 'abstract'
			classFile.access_flags &= ~Constants.ACC_ABSTRACT;			
		}
		
		 // Access : public private static volatile ...
		String access;
		
		if ((classFile.access_flags & Constants.ACC_ENUM) == 0)		
			access = classFile.isAInnerClass() ?
					AccessNestedClassToString(classFile.access_flags) :
					AccessClassToString(classFile.access_flags);
		else
			access = classFile.isAInnerClass() ?
					AccessNestedEnumToString(classFile.access_flags) :
					AccessEnumToString(classFile.access_flags);			

		spw.printKeyword(Printer.UNKNOWN_LINE_NUMBER, access);
		spw.printKeyword(
			Printer.UNKNOWN_LINE_NUMBER, GetType(classFile.access_flags));
		spw.print(Printer.UNKNOWN_LINE_NUMBER, ' ');

		ConstantPool constants = classFile.getConstantPool();
		
		AttributeSignature as = classFile.getAttributeSignature();
		if (as == null)
		{
			// Signature sans notations generiques
			spw.print(Printer.UNKNOWN_LINE_NUMBER, classFile.getClassName());
			
			if ((classFile.access_flags & Constants.ACC_ANNOTATION) != 0)
			{
				// Annotation
			}
			else if ((classFile.access_flags & Constants.ACC_ENUM) != 0)
			{
				// Enum
				 // Interfaces
				WriteInterfaces(spw, referenceMap, classFile);				
			}
			if ((classFile.access_flags & Constants.ACC_INTERFACE) != 0)
			{
				// Interface
				 // Super interface
				int[] interfaceIndexes = classFile.getInterfaces();
				if ((interfaceIndexes != null) && (interfaceIndexes.length > 0))
				{
					spw.startClassDeclarationExtends();
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "extends ");
					
					String signature = 
						'L' + constants.getConstantClassName(interfaceIndexes[0]) + ';';
					SignatureWriter.WriteSimpleSignature(
						spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, 
						classFile, signature);
					
					for (int i=1; i<interfaceIndexes.length; i++)
					{
						spw.print(Printer.UNKNOWN_LINE_NUMBER, ", ");
						signature = 
							'L' + constants.getConstantClassName(interfaceIndexes[i]) + ';';
						SignatureWriter.WriteSimpleSignature(
							spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, 
							classFile, signature);
					}
					
					spw.endClassDeclarationExtends();
				}
			}
			else
			{
				// Class
				 // Super class
				String internalSuperClassName = classFile.getSuperClassName();
				if ((internalSuperClassName != null) && 
					!Constants.INTERNAL_OBJECT_CLASS_NAME.equals(internalSuperClassName))
				{
					spw.startClassDeclarationExtends();
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "extends ");
					String signature = 'L' + internalSuperClassName + ';';
					SignatureWriter.WriteSimpleSignature(
						spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, 
						classFile, signature);
					spw.endClassDeclarationExtends();
				}
				
				// Interfaces
				WriteInterfaces(spw, referenceMap, classFile);	
			}		
		}
		else
		{
			// Signature contenant des notations generiques
			String signature = constants.getConstantUtf8(as.signature_index);
			SignatureWriter.WriteClassSignature(
				spw, referenceMap, classFile, signature);
		}
	}
		
	private static void WriteAttributes(
			Printer spw, ReferenceMap referenceMap, 
			ClassFile classFile, Attribute[] attributes)
	{
		if (attributes == null)
			return;
		
		ConstantPool constants = classFile.getConstantPool();
		spw.startComment();
		
		for (int i=0; i<attributes.length; i++)
		{
			Attribute attribute = attributes[i];
			
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "// Attribute: ");

			switch (attribute.tag)
			{
			case Constants.ATTR_UNKNOWN:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "ATTR_UNKNOWN");	
				spw.endOfLineComment();
				UnknowAttribute ua = (UnknowAttribute)attributes[i];
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "// -> name: " + 
						constants.getConstantUtf8(ua.attribute_name_index));
				spw.endOfLineComment();
				break;
			case Constants.ATTR_SOURCE_FILE:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "ATTR_SOURCE_FILE");							
				spw.endOfLineComment();
				AttributeSourceFile asf = (AttributeSourceFile)attributes[i];
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "// -> source file: " + 
						constants.getConstantUtf8(asf.sourcefile_index));
				spw.endOfLineComment();
				break;
			case Constants.ATTR_CODE:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "ATTR_CODE");
				spw.endOfLineComment();
				WriteAttributes(
						spw, referenceMap, classFile, 
						((AttributeCode)attributes[i]).attributes);
				spw.endOfLineComment();
				break;							
			case Constants.ATTR_SIGNATURE:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "ATTR_SIGNATURE");							
				spw.endOfLineComment();
				AttributeSignature as = (AttributeSignature)attributes[i];
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "// -> signature: " + 
						constants.getConstantUtf8(as.signature_index));
				spw.endOfLineComment();
				break;
			case Constants.ATTR_ENCLOSING_METHOD:
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "ATTR_ENCLOSING_METHOD");							
				spw.endOfLineComment();
				AttributeEnclosingMethod aem = 
					(AttributeEnclosingMethod)attributes[i];
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(aem.method_index);
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "// -> method: " + 
						constants.getConstantUtf8(cnat.name_index) +
						constants.getConstantUtf8(cnat.descriptor_index));
				spw.endOfLineComment();
				break;
			}
		}
		
		spw.endComment();
	}

	private static void WriteEnumValues(
		Preferences preferences, Printer spw, 
		ReferenceMap referenceMap, ClassFile classFile)
	{
		List<Instruction> values = classFile.getEnumValues();
		
		if (values != null)
		{
			final int length = values.size();

			if (length > 0)
			{
				spw.startEnumValueDeclaration();					

				ConstantPool constants = classFile.getConstantPool();
				Method staticMethod = classFile.getStaticMethod();				
				FastSourceWriterVisitor swv = new FastSourceWriterVisitor(
					keywords, preferences, spw, referenceMap, classFile,
					staticMethod.access_flags, staticMethod.getLocalVariables());
				Field[] fields = classFile.getFields();
				ArrayList<InvokeNew> invokeNews = 
					new ArrayList<InvokeNew>(length);
				
				// Pour chaque valeur, recherche du l'attribut d'instance, puis 
		    	// du constructeur
		    	for (int i=0; i<length; i++)
		    	{
		    		GetStatic getStatic = (GetStatic)values.get(i);
		    		ConstantFieldref cfr = 
		    			constants.getConstantFieldref(getStatic.index);
		    		ConstantNameAndType cnat = constants.getConstantNameAndType(
		    			cfr.name_and_type_index);
		    		
		    		for (int j=fields.length-1; j>=0; --j)
		    		{
		    			Field field = fields[j];
		    			
		    			if ((field.name_index != cnat.name_index) || 
		    				(field.descriptor_index != cnat.descriptor_index))
		    				continue;
		    			
	    				ValueAndLocalVariables valv = 
	    					field.getValueAndLocalVariables();
	    				invokeNews.add( (InvokeNew)valv.getValue() );
	    				break;
		    		}
		    	}
				
				// Affichage des valeurs
				InvokeNew invokeNew = invokeNews.get(0);
				int lineNumber = WriteEnumValue(
					spw, swv, values.get(0), invokeNew);

				for (int i=1; i<length; i++)
				{
					spw.print(lineNumber, ", ");
					InvokeNew nextInvokeNew = invokeNews.get(i);

					if (invokeNew.lineNumber != nextInvokeNew.lineNumber)
						spw.endOfStatement();

					lineNumber = WriteEnumValue(
						spw, swv, values.get(i), nextInvokeNew);
					invokeNew = nextInvokeNew;
				}

		    	spw.print(lineNumber, ';');
				spw.endEnumValueDeclaration();
			}
		}
	}
	
	private static int WriteEnumValue(
		Printer spw, FastSourceWriterVisitor swv, 
		Instruction getStatic, InvokeNew invokeNew)
	{
		int lineNumber = invokeNew.lineNumber;
			
		getStatic.lineNumber = lineNumber;
		swv.visit(getStatic);
		
		List<Instruction> args = invokeNew.args;
		int length = args.size();

		if (length > 2)
		{
			spw.print(lineNumber, '(');

			Instruction instruction = args.get(2);
			lineNumber = instruction.lineNumber;
			
			swv.visit(instruction);
			
			for (int i=3; i<length; i++)
			{
				spw.print(lineNumber, ", ");
				
				instruction = args.get(i);
				lineNumber = instruction.lineNumber;
				
				swv.visit(instruction);
			}

			spw.print(lineNumber, ')');
		}
		
		return lineNumber;
	}
	
	private static void WriteFields(
		Preferences preferences, Printer spw, 
		ReferenceMap referenceMap, ClassFile classFile)
	{
		Field[] fields = classFile.getFields();
		
		if (fields == null)
			return;
		
	    for (int i=0; i<fields.length; i++)
		{
			Field field = fields[i];
			
	    	/* if ((field.access_flags & (Constants.ACC_SYNTHETIC|Constants.ACC_ENUM)) != 0)
	    		continue;*/
	    	
			spw.startFieldDeclaration();
			
    		WriteAttributes(spw, referenceMap, classFile, field.getAttributes());
			
			if (field.containsAttributeDeprecated() &&
				!field.containsAnnotationDeprecated(classFile))
			{	    		
				spw.startJavadoc();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "/**");
				spw.endOfLineJavadoc();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, " * @deprecated");
				spw.endOfLineJavadoc();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, " */");
				spw.endJavadoc();
			}
				
			AnnotationWriter.WriteAttributeAnnotations(
					spw, referenceMap, classFile, field.getAttributes());
			
			String access = AccessFieldToString(field.access_flags);
		    if (access.length() > 0)
		    {
				spw.print(Printer.UNKNOWN_LINE_NUMBER, access);
				spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");		    	
		    }
		    
			ConstantPool constants = classFile.getConstantPool();

			AttributeSignature as = field.getAttributeSignature();
		    int signatureIndex = (as == null) ? 
		    		field.descriptor_index : as.signature_index;
		    String signature = constants.getConstantUtf8(signatureIndex);
			SignatureWriter.WriteSimpleSignature(
				spw, Printer.UNKNOWN_LINE_NUMBER, referenceMap, classFile, 
				signature);
			spw.print(Printer.UNKNOWN_LINE_NUMBER, " ");	
			String fieldName = constants.getConstantUtf8(field.name_index);
			if (keywords.contains(fieldName))
				fieldName = Constants.JD_FIELD_PREFIX + fieldName;
			spw.print(Printer.UNKNOWN_LINE_NUMBER, fieldName);		    	

		    ConstantValue cv = field.getConstantValue(constants);
		    if(cv != null)
		    {
		    	spw.print(Printer.UNKNOWN_LINE_NUMBER, " = ");
		    	ConstantValueWriter.Write(
		    		spw, Printer.UNKNOWN_LINE_NUMBER, 
		    		classFile.getConstantPool(), cv, 
		    		(byte)signature.charAt(0));
		    }
		    else if (field.getValueAndLocalVariables() != null)
		    {
		    	ValueAndLocalVariables valueAndLocalVariables = 
		    		field.getValueAndLocalVariables();
		    	
				FastSourceWriterVisitor swv = new FastSourceWriterVisitor(
    				keywords, preferences, spw, referenceMap, classFile, 0, 
    				valueAndLocalVariables.getLocalVariables());
		    	
		    	spw.print(Printer.UNKNOWN_LINE_NUMBER, " = ");
		    	swv.visit(valueAndLocalVariables.getValue());
		    }
		    
		    spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
			spw.endFieldDeclaration();
	    }
	}

	private static void WriteMethods(
		Preferences preferences, Printer spw, 
		ReferenceMap referenceMap, ClassFile classFile)
	{
		Method[] methods = classFile.getMethods();

		if (methods == null)
			return;
		
		ConstantPool constants = classFile.getConstantPool();
		boolean multipleConstructorFlag = ContainsMultipleConstructor(classFile);

	    for (int i=0; i<methods.length; i++)
		{
	    	Method method = methods[i];
	    	
//	    	if ((method.access_flags & 
//	    		 (Constants.ACC_SYNTHETIC|Constants.ACC_BRIDGE)) != 0)
//	    		continue;

    		AttributeSignature as = method.getAttributeSignature();
    		
    		// Le descripteur et la signature sont differentes pour les 
    		// constructeurs des Enums ! Cette information est passée à 
    		// "SignatureWriter.WriteMethodSignature(...)".
		    boolean descriptorFlag = (as == null);
    		int signatureIndex = descriptorFlag ? 
		    		method.descriptor_index : as.signature_index;
		    String signature = constants.getConstantUtf8(signatureIndex);
		    	
	    	if (((classFile.access_flags & Constants.ACC_ENUM) != 0) && 
	    		IsAMethodOfEnum(classFile, method, signature))
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
    					if ((classFile.access_flags & Constants.ACC_ENUM) != 0)
    					{
    						if (SignatureAnalyzer.GetParameterSignatureCount(signature) == 2)
    						{
    							// Ne pas afficher le constructeur par defaut 
    							// des Enum si il est vide et si c'est le seul 
    							// constructeur.
    							continue;
    						}
    					}
    					else
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
	    	
    		spw.startMethodDeclaration();
    		
			WriteAttributes(
				spw, referenceMap, classFile, method.getAttributes());
			
			if (method.containsAttributeDeprecated() &&
				!method.containsAnnotationDeprecated(classFile))
			{
				spw.startJavadoc();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "/**");
				spw.endOfLineJavadoc();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, " * @deprecated");
				spw.endOfLineJavadoc();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, " */");
				spw.endJavadoc();
			}
			
			if (method.containsError())
			{
				spw.startErrorBlock();
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "// ERROR //");
				spw.endErrorBlock();
			}
			
			AnnotationWriter.WriteAttributeAnnotations(
					spw, referenceMap, classFile, method.getAttributes());
			
			if (method.name_index == constants.classConstructorIndex)
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "static"); 	
			}
			else
			{
				String access = AccessMethodToString(method.access_flags);	
				if (access.length() > 0)
				{
					spw.print(Printer.UNKNOWN_LINE_NUMBER, access);
					spw.print(Printer.UNKNOWN_LINE_NUMBER, ' ');
				}
				
				SignatureWriter.WriteMethodSignature(
					keywords, spw, referenceMap, classFile, 
					method, signature, descriptorFlag);
				
				int[] exceptionIndexes = method.getExceptionIndexes();
				if (exceptionIndexes != null)
				{
					spw.startMethodDeclarationThrows();
					spw.print(Printer.UNKNOWN_LINE_NUMBER, Constants.INDENT);
					spw.print(Printer.UNKNOWN_LINE_NUMBER, "throws ");
					for (int j=0; j<exceptionIndexes.length; j++)
					{
						if (j > 0)
							spw.print(Printer.UNKNOWN_LINE_NUMBER, ", ");
						String internalClassName = 
							constants.getConstantClassName(exceptionIndexes[j]);
						spw.print(Printer.UNKNOWN_LINE_NUMBER, 
							InternalClassNameToShortClassName(
								referenceMap, classFile, internalClassName));
					}
					spw.endMethodDeclarationThrows();
				}
			}
			
			if (method.getCode() == null)
			{
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
			}
			else
			{
				spw.startStatementBlock();
				ByteCodeWriter.Write(spw, referenceMap, classFile, method);
				spw.endStatementBlock();
				
				if ((method.access_flags & 
			    		 (Constants.ACC_SYNTHETIC|Constants.ACC_BRIDGE)) == 0)
				{
					spw.startStatementBlock();
					JavaCodeWriter.Write(
						keywords, preferences, spw, referenceMap, classFile, method);
					spw.endStatementBlock();
					
					spw.startStatementBlock();
					FastWriter.Write(
						keywords, preferences, spw, referenceMap, classFile, method);
					spw.endStatementBlock();
				}
			}

			spw.endMethodDeclaration();    		
		}
	}

	private static boolean ContainsMultipleConstructor(ClassFile classFile)
	{
		ConstantPool constants = classFile.getConstantPool();
		Method[] methods = classFile.getMethods();
		boolean flag = false;
		
		for (int i=0; i<methods.length; i++)
		{
	    	Method method = methods[i];
	    	
	    	if ((method.access_flags & 
	    		 (Constants.ACC_SYNTHETIC|Constants.ACC_BRIDGE)) != 0)
	    		continue;

    		if (method.name_index == constants.instanceConstructorIndex)
	    	{
    			if (flag)
    				// A other constructor has been found
    				return true;
    			// A first constructor has been found
    			flag = true;
	    	}
		}
		
		return false;
	}
	
	private static boolean IsAMethodOfEnum(
		ClassFile classFile, Method method, String signature)
	{
		ConstantPool constants = classFile.getConstantPool();

		if ((method.access_flags & (Constants.ACC_PUBLIC|Constants.ACC_STATIC)) == 
			(Constants.ACC_PUBLIC|Constants.ACC_STATIC))
		{
			String methodName = constants.getConstantUtf8(method.name_index);

			if (methodName.equals(Constants.ENUM_VALUEOF_METHOD_NAME))
			{
				String s = "(Ljava/lang/String;)" + classFile.getInternalClassName();	    				
				if (s.equals(signature))
	    			// Ne pas afficher la methode 
					// "public static enumXXX valueOf(String paramString)".
					return true;
			}
	
			if (methodName.equals(Constants.ENUM_VALUES_METHOD_NAME))
			{
				String s = "()[" + classFile.getInternalClassName();	    				
				if (s.equals(signature))
	    			// Ne pas afficher la methode 
					// "public static enumXXX[] values()".
					return true;
			}
		}
		
		return false;
	}
	
	private static void WriteInnerClasses(
		Preferences preferences, Printer spw, 
		ReferenceMap referenceMap, ClassFile classFile)
	{
		ClassFile[] innerClassFiles = classFile.getInnerClassFiles();
		
		if (innerClassFiles == null)
			return;
		
		for (int i=0; i<innerClassFiles.length; i++)
		{
			ClassFile innerClassFile = innerClassFiles[i];
			
	    	if (((innerClassFile.access_flags & Constants.ACC_SYNTHETIC) != 0) ||
	    		(innerClassFile.getInternalAnonymousClassName() != null))
	    		continue;
			
			WriteClass(preferences, spw, referenceMap, innerClassFile);
		}
	}

	// --------------------------------------------------------------------- //
	
	private static String InternalClassNameToInternalPackageName(String path)
	{
		int index = path.lastIndexOf(Constants.INTERNAL_PACKAGE_SEPARATOR);
		return (index == -1) ? "" : path.substring(0, index);
	}
	
	private static String InternalClassNameToQualifiedClassName(String path)
	{
		return path.replace(Constants.INTERNAL_PACKAGE_SEPARATOR, 
				            Constants.PACKAGE_SEPARATOR)
				   .replace(Constants.INTERNAL_INNER_SEPARATOR, 
				            Constants.INNER_SEPARATOR);
	}
	
	private static String AccessMethodToString(int access_flags)
	{
		StringBuffer buf = new StringBuffer();

		for(int i=0; i<Constants.ACCESS_METHOD_NAMES.length; i++) 
		{
			int p = (1 << i);
			
			if ((access_flags & p) != 0) 
				if (Constants.ACCESS_METHOD_NAMES[i] != null)
					buf.append(Constants.ACCESS_METHOD_NAMES[i] + " ");
		}
		
		return buf.toString().trim();
	}
	
	private static String InternalClassNameToShortClassName(
		ReferenceMap referenceMap, ClassFile classFile, 
		String internalClassName)
	{
		int index = internalClassName.lastIndexOf(Constants.INTERNAL_PACKAGE_SEPARATOR);

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
							Constants.INTERNAL_PACKAGE_SEPARATOR, 
							Constants.PACKAGE_SEPARATOR);
			}
		}
		
		return internalClassName.replace(
				Constants.INTERNAL_INNER_SEPARATOR, 
				Constants.INNER_SEPARATOR);
	}
	
	private static String GetType(int access_flags) 
	{
		if ((access_flags & Constants.ACC_ANNOTATION) != 0)
			return "@interface";
		if ((access_flags & Constants.ACC_ENUM) != 0)
			return "enum";
		if ((access_flags & Constants.ACC_INTERFACE) != 0)
			return "interface";
		return "class";
	}

	private static String AccessClassToString(int access_flags)
	{
		StringBuffer buf = new StringBuffer();

		if ((access_flags & Constants.ACC_PUBLIC) != 0) 
			buf.append("public ");
		if ((access_flags & Constants.ACC_FINAL) != 0) 
			buf.append("final ");
		if ((access_flags & Constants.ACC_ABSTRACT) != 0) 
			buf.append("abstract ");

		return buf.toString();
	}
	
	private static String AccessEnumToString(int access_flags)
	{
		return ((access_flags & Constants.ACC_PUBLIC) != 0) ? "public " : " ";
	}
	
	private static String AccessFieldToString(int access_flags)
	{
		StringBuffer buf = new StringBuffer();

		for(int i=0; i<Constants.ACCESS_FIELD_NAMES.length; i++) 
		{
			int p = (1 << i);
			
			if((access_flags & p) != 0) 
				if((p != Constants.ACC_SUPER) && (p != Constants.ACC_INTERFACE))
					if (Constants.ACCESS_FIELD_NAMES[i] != null)
						buf.append(Constants.ACCESS_FIELD_NAMES[i]).append(' ');
		}
		
		return buf.toString();
	}
	
	private static String AccessNestedClassToString(int access_flags)
	{
		StringBuffer buf = new StringBuffer();

		for(int i=0; i<Constants.ACCESS_NESTED_CLASS_NAMES.length; i++) 
		{
			int p = (1 << i);
			
			if((access_flags & p) != 0) 
				if((p != Constants.ACC_SUPER) && (p != Constants.ACC_INTERFACE))
					buf.append(Constants.ACCESS_NESTED_CLASS_NAMES[i]).append(' ');
		}
		
		if ((access_flags & Constants.ACC_ABSTRACT) != 0) 
			buf.append("abstract ");
		
		return buf.toString();
	}
	
	private static String AccessNestedEnumToString(int access_flags)
	{
		StringBuffer buf = new StringBuffer();

		for(int i=0; i<Constants.ACCESS_NESTED_ENUM_NAMES.length; i++) 
		{
			int p = (1 << i);
			
			if((access_flags & p) != 0) 
				if((p != Constants.ACC_SUPER) && (p != Constants.ACC_INTERFACE))
					buf.append(Constants.ACCESS_NESTED_ENUM_NAMES[i]).append(' ');
		}
		
		if ((access_flags & Constants.ACC_ABSTRACT) != 0) 
			buf.append("abstract ");
		
		return buf.toString();
	}	
	
	static
	{
		keywords = new HashSet<String>();
	
		keywords.add("abstract");
		keywords.add("assert");
		keywords.add("@interface");
		keywords.add("boolean");
		keywords.add("break");
		keywords.add("byte");
		keywords.add("case");
		keywords.add("catch");
		keywords.add("char");
		keywords.add("class");
		keywords.add("continue");
		keywords.add("default");
		keywords.add("do");
		keywords.add("double");
		keywords.add("else");
		keywords.add("enum");
		keywords.add("extends");
		keywords.add("false");
		keywords.add("final");
		keywords.add("finally");
		keywords.add("float");
		keywords.add("for");
		keywords.add("goto");
		keywords.add("if");
		keywords.add("implements");
		keywords.add("import");
		keywords.add("instanceof");
		keywords.add("int");
		keywords.add("interface");
		keywords.add("long");
		keywords.add("new");
		keywords.add("null");
		keywords.add("package");
		keywords.add("public");
		keywords.add("return");
		keywords.add("short");
		keywords.add("static");
		keywords.add("super");
		keywords.add("switch");
		keywords.add("synchronized");
		keywords.add("this");
		keywords.add("throw");
		keywords.add("true");
		keywords.add("try");
		keywords.add("void");
		keywords.add("while");
	}
}