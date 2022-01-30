package org.jd.core.v1.service.deserializer.classfile.attribute;

import jd.core.process.deserializer.ClassFormatException;

public class InvalidAttributeLengthException extends ClassFormatException {

    private static final long serialVersionUID = 1L;

    public InvalidAttributeLengthException() {
        super("Invalid attribute length");
    }
}
