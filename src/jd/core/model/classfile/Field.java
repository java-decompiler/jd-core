package jd.core.model.classfile;

import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeConstantValue;
import jd.core.model.classfile.attribute.AttributeConstants;
import jd.core.model.classfile.constant.ConstantValue;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.util.UtilConstants;


public class Field extends FieldOrMethod
{
	private ValueAndMethod valueAndMethod = null;
	/* 
	 * Attributs pour l'affichage des champs synthetique des classes anonymes.
	 * Champs modifié par:
	 * 1) ClassFileAnalyzer.AnalyseAndModifyConstructors(...) pour y placer le 
	 *    numero (position) du parametre du constructeur initialisant le champs.
	 */
	public int anonymousClassConstructorParameterIndex;
	/*
	 * 2) NewInstructionReconstructorBase.InitAnonymousClassConstructorParameterName(...) 
	 *    pour y placer l'index du nom du parametre.
	 * 3) SourceWriterVisitor.writeGetField(...) pour afficher le nom du
	 *    parameter de la classe englobante.
	 */
	public int outerMethodLocalVariableNameIndex;
		
	public Field(
		int access_flags, int name_index, 
		int descriptor_index, Attribute[] attributes)
	{
		super(access_flags, name_index, descriptor_index, attributes);
		this.anonymousClassConstructorParameterIndex = UtilConstants.INVALID_INDEX;
		this.outerMethodLocalVariableNameIndex = UtilConstants.INVALID_INDEX;
	}
	
	public ConstantValue getConstantValue(ConstantPool constants) 
	{
		if (this.attributes != null)
			for(int i=0; i<this.attributes.length; i++)
				if (this.attributes[i].tag == AttributeConstants.ATTR_CONSTANT_VALUE)
				{
					AttributeConstantValue acv = (AttributeConstantValue)this.attributes[i];
					return constants.getConstantValue(acv.constantvalue_index);
				}

		return null;
	}

	public ValueAndMethod getValueAndMethod() 
	{	
		return valueAndMethod; 
	}
	
	public void setValueAndMethod(Instruction value, Method method) 
	{ 
		this.valueAndMethod = new ValueAndMethod(value, method); 
	}
	
	public static class ValueAndMethod
	{
		private Instruction value;
		private Method method;
		
		ValueAndMethod(Instruction value, Method method)
		{
			this.value = value;
			this.method = method;
		}

		public Method getMethod() 
		{
			return method;
		}

		public Instruction getValue() 
		{
			return value;
		}		
	}
}
