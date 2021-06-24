package org.jd.core.v1.service.deserializer.classfile.attribute;

import org.jd.core.v1.service.deserializer.classfile.ClassFileFormatException;

public class InvalidAttributeLengthException extends ClassFileFormatException {

    private static final long serialVersionUID = 1L;

    public InvalidAttributeLengthException() {
        super("Invalid attribute length");
    }
}
