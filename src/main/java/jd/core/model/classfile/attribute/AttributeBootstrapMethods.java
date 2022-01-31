/*******************************************************************************
 * Copyright (C) 2007-2019 GPLv3
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
package jd.core.model.classfile.attribute;

import org.apache.bcel.classfile.BootstrapMethod;

public class AttributeBootstrapMethods extends Attribute {
    
    private final BootstrapMethod[] bootstrapMethods;

    public AttributeBootstrapMethods(byte tag, BootstrapMethod[] bootstrapMethods) {
        super(tag);
        this.bootstrapMethods = bootstrapMethods;
    }
    
    public BootstrapMethod[] getBootstrapMethods() {
        return bootstrapMethods;
    }

    public BootstrapMethod getBootstrapMethod(int i) {
        return bootstrapMethods[i];
    }
}
