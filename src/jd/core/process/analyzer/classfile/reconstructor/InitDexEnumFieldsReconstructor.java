package jd.core.process.analyzer.classfile.reconstructor;

import java.util.ArrayList;
import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.AAStore;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AStore;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.util.StringConstants;


/*
 * Les valeurs des Enum des classes produites par "javac" sont correctement 
 * reconnues par "InitStaticFieldsReconstructor.Reconstruct(...)".
 * 
 * Cette classe a ete creee car les enums produits par "dex2jar" contients un 
 * motif particulier (Extrait de: gr\androiddev\FuelPrices\StaticTools.class):
 * 
 * private static final synthetic LocationProvider ANY;
 * private static synthetic LocationProvider BESTOFBOTH;
 * private static synthetic LocationProvider ENUM$VALUES[];
 * private static synthetic LocationProvider GPS;
 * private static synthetic LocationProvider NETWORK;
 * 
 * static 
 * {
 *   BESTOFBOTH = new LocationProvider("BESTOFBOTH", 0);
 *   ANY = new LocationProvider("ANY", 1);
 *   GPS = new LocationProvider("GPS", 2);
 *   NETWORK = new LocationProvider("NETWORK", 3);
 *   LocationProvider alocationprovider[] = new LocationProvider[4];
 *   alocationprovider[0] = BESTOFBOTH;
 *   alocationprovider[1] = ANY;
 *   alocationprovider[2] = GPS;
 *   alocationprovider[3] = NETWORK;
 *   ENUM$VALUES = alocationprovider;
 * }
 * 
 * --> Les instructions d'initialisation et les champs ne sont pas classes dans le meme ordre.
 * --> Un tableau local est utilise.
 */
public class InitDexEnumFieldsReconstructor 
{
	public static void Reconstruct(ClassFile classFile)
	{
		Method method = classFile.getStaticMethod();
		if (method == null)
			return;
		
		Field[] fields = classFile.getFields();
		if (fields == null)
			return;
		
		List<Instruction> list = method.getFastNodes();	
		if (list == null)
			return;
		
		ConstantPool constants = classFile.getConstantPool();

		// Search field initialisation from the end
		
		// Search PutStatic("ENUM$VALUES", ALoad(...))
		int indexInstruction = list.size();
		
		if (indexInstruction > 0)
		{
			// Saute la derniere instruction 'return'
			indexInstruction--;
					
			while (indexInstruction-- > 0)
			{
				Instruction instruction = list.get(indexInstruction);		
				if (instruction.opcode != ByteCodeConstants.PUTSTATIC)
					break;
				
				PutStatic putStatic = (PutStatic)instruction;
				if (putStatic.valueref.opcode != ByteCodeConstants.ALOAD)
					break;
				
				ConstantFieldref cfr = constants.getConstantFieldref(putStatic.index);
				if (cfr.class_index != classFile.getThisClassIndex())
					break;
					
				ConstantNameAndType cnat = 
					constants.getConstantNameAndType(cfr.name_and_type_index);
				
				String name = constants.getConstantUtf8(cnat.name_index);
				if (! name.equals(StringConstants.ENUM_VALUES_ARRAY_NAME_ECLIPSE))
					break;

				int indexField = fields.length;

				while (indexField-- > 0)
				{
					Field field = fields[indexField];
					
					if (((field.access_flags & (ClassFileConstants.ACC_STATIC|ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_FINAL|ClassFileConstants.ACC_PRIVATE)) == 
							(ClassFileConstants.ACC_STATIC|ClassFileConstants.ACC_SYNTHETIC|ClassFileConstants.ACC_FINAL|ClassFileConstants.ACC_PRIVATE)) && 
						(cnat.descriptor_index == field.descriptor_index) &&
						(cnat.name_index == field.name_index))						
					{
						// "ENUM$VALUES = ..." found.
						ALoad aload = (ALoad)putStatic.valueref;
						int localEnumArrayIndex = aload.index;
						int index = indexInstruction;
						
						// Middle instructions of pattern : AAStore(...)
						ArrayList<Instruction> values = new ArrayList<Instruction>();
						
						while (index-- > 0)
						{
							instruction = list.get(index);
							if (instruction.opcode != ByteCodeConstants.AASTORE)
								break;
							AAStore aastore = (AAStore)instruction;
							if ((aastore.arrayref.opcode != ByteCodeConstants.ALOAD) ||
								(aastore.valueref.opcode != ByteCodeConstants.GETSTATIC) ||
								(((ALoad)aastore.arrayref).index != localEnumArrayIndex))
								break;
							values.add(aastore.valueref);
						}
						
						// FastDeclaration(AStore(...))
						if (instruction.opcode != FastConstants.DECLARE) 
							break;
						FastDeclaration declaration = (FastDeclaration)instruction;
						if (declaration.instruction.opcode != ByteCodeConstants.ASTORE)
							break;
						AStore astore = (AStore)declaration.instruction;
						if (astore.index != localEnumArrayIndex)
							break;
						
						int valuesLength = values.size();
						
						if (valuesLength > 0)
						{
							// Pattern found.
							
							// Construct new pattern
							InitArrayInstruction iai = 
								new InitArrayInstruction(
									ByteCodeConstants.INITARRAY, 
									putStatic.offset, 
									declaration.lineNumber, 
									new ANewArray(
										ByteCodeConstants.ANEWARRAY, 
										putStatic.offset, 
										declaration.lineNumber, 
										classFile.getThisClassIndex(), 
										new IConst(
											ByteCodeConstants.ICONST, 
											putStatic.offset, 
											declaration.lineNumber, 
											valuesLength)), 
									values);
							field.setValueAndMethod(iai, method);	
							
							// Remove PutStatic
							list.remove(indexInstruction);
							// Remove AAStores
							while (--indexInstruction > index)
								list.remove(indexInstruction);
							// Remove FastDeclaration
							list.remove(indexInstruction);
						}
						
						break;
					}
				}
			}
		}
	}	
}
