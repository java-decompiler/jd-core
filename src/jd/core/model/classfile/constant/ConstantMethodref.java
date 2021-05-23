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
package jd.core.model.classfile.constant;

import java.util.List;


public class ConstantMethodref extends Constant 
{
	final public int class_index;
	final public int name_and_type_index;

	private List<String> listOfParameterSignatures;
	private String returnedSignature;
	
	public ConstantMethodref(
		byte tag, int class_index, int name_and_type_index)
	{
		super(tag);
		this.class_index = class_index;
		this.name_and_type_index = name_and_type_index;
		
		this.listOfParameterSignatures = null;
		this.returnedSignature = null;
	}

	public ConstantMethodref(
		byte tag, int class_index, int name_and_type_index,
		List<String> listOfParameterSignatures, String returnedSignature)
	{
		super(tag);
		this.class_index = class_index;
		this.name_and_type_index = name_and_type_index;
		
		this.listOfParameterSignatures = listOfParameterSignatures;
		this.returnedSignature = returnedSignature;
	}

	public List<String> getListOfParameterSignatures() 
	{
		return listOfParameterSignatures;
	}
	public void setParameterSignatures(List<String> listOfParameterSignatures) 
	{
		this.listOfParameterSignatures = listOfParameterSignatures;
	}
	public int getNbrOfParameters()
	{
		return (this.listOfParameterSignatures == null) ? 
				0 : this.listOfParameterSignatures.size();
	}
	
	public String getReturnedSignature() 
	{
		return returnedSignature;
	}
	public void setReturnedSignature(String returnedSignature) 
	{
		this.returnedSignature = returnedSignature;
	}
	public boolean returnAResult()
	{
		return (this.returnedSignature == null) ? 
				false : !"V".equals(this.returnedSignature);
	}
}
