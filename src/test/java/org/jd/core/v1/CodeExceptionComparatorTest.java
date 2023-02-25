/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.bcel.classfile.CodeException;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ControlFlowGraphMaker;
import org.junit.Test;

import java.util.Arrays;

import junit.framework.TestCase;

public class CodeExceptionComparatorTest extends TestCase {
    @Test
    public void test() throws Exception {
        // CodeException(int index, int startPc, int endPc, int handlerPc, int catchType)
        CodeException ce0 = new CodeException(10, 80, 50, 1);
        CodeException ce1 = new CodeException(20, 30, 35, 2);
        CodeException ce2 = new CodeException(20, 25, 35, 3);
        CodeException ce3 = new CodeException(25, 30, 40, 0);
        CodeException ce4 = new CodeException(10, 30, 60, 0);

        CodeException[] codeExceptions = { ce0, ce1, ce2, ce3, ce4 };

        ControlFlowGraphMaker.CodeExceptionComparator comparator = new ControlFlowGraphMaker.CodeExceptionComparator();

        Arrays.sort(codeExceptions, comparator);

        assertSame(codeExceptions[0], ce4);
        assertSame(codeExceptions[1], ce0);
        assertSame(codeExceptions[2], ce2);
        assertSame(codeExceptions[3], ce1);
    }
}