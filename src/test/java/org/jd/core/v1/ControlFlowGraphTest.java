/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.cfg.ControlFlowGraphPlantUMLWriter;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.ExceptionHandler;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.SwitchCase;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.ControlFlowGraph;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.Loop;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.processor.ConvertClassFileProcessor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.*;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;
import org.jd.core.v1.util.StringConstants;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.*;
import static org.junit.Assert.assertNotEquals;

import junit.framework.TestCase;

public class ControlFlowGraphTest extends TestCase {
    protected ClassFileDeserializer deserializer = new ClassFileDeserializer();
    protected ConvertClassFileProcessor converter = new ConvertClassFileProcessor();
    protected ClassPathLoader loader = new ClassPathLoader();
    protected TypeMaker typeMaker = new TypeMaker(loader);

    // --- Basic test ----------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk170BasicDoSomethingWithString() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/Basic", "doSomethingWithString"));
        }
    }

    // --- Test 'if' and 'if-else' ---------------------------------------------------------------------------------- //
    @Test
    public void testJdk170If() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "if_")));
        }
    }

    @Test
    public void testJdk170IfIf() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifIf")));

            BasicBlock ifBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF, ifBB.getType());
            assertEquals(TYPE_IF, ifBB.getSub1().getType());
        }
    }

    @Test
    public void testJdk170MethodCallInIfCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "methodCallInIfCondition")));
        }
    }

    @Test
    public void testJdk170IlElse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElse")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_CONDITION, ifElseBB.getCondition().getType());
            assertTrue(ifElseBB.getCondition().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170IlElseIfElse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseIfElse")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifORCondition")));
            BasicBlock ifBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_OR, ifBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub1().getType());
            assertFalse(ifBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_OR, ifBB.getCondition().getSub2().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub1().getType());
            assertFalse(ifBB.getCondition().getSub2().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub2().getType());
            assertTrue(ifBB.getCondition().getSub2().getSub2().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170IfANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifANDCondition")));
            BasicBlock ifBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_AND, ifBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub1().getType());
            assertTrue(ifBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_AND, ifBB.getCondition().getSub2().getType());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub1().getType());
            assertTrue(ifBB.getCondition().getSub2().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, ifBB.getCondition().getSub2().getSub2().getType());
            assertTrue(ifBB.getCondition().getSub2().getSub2().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170IfElseORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseORCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifElseBB.getCondition().getSub1().getType());
            assertFalse(ifElseBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElseANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseANDCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION, ifElseBB.getCondition().getSub1().getType());
            assertTrue(ifElseBB.getCondition().getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElse6ANDAnd2ORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElse6ANDAnd2ORCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElse6ORAnd2ANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElse6ORAnd2ANDCondition")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testJdk170IfElseORAndANDConditions() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseORAndANDConditions")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub2().getType());
        }
    }

    @Test
    public void testIfElseANDAndORConditions() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkIfElseReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/IfElse", "ifElseANDAndORConditions")));
            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getType());
            assertEquals(TYPE_CONDITION_AND, ifElseBB.getCondition().getSub1().getType());
            assertEquals(TYPE_CONDITION_OR, ifElseBB.getCondition().getSub2().getType());
        }
    }

    protected static ControlFlowGraph checkIfReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock ifBB = checkIfCommonReduction(cfg);

        assertEquals(TYPE_IF, ifBB.getType());

        return cfg;
    }

    protected static ControlFlowGraph checkIfElseReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock ifElseBB = checkIfCommonReduction(cfg);

        assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
        assertNotNull(ifElseBB.getSub2());
        assertEquals(ifElseBB.getSub2().getNext(), END);

        return cfg;
    }

    protected static BasicBlock checkIfCommonReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);
        assertEquals(TYPE_START, startBB.getType());

        assertNotNull(startBB.getNext());

        BasicBlock ifBB = startBB.getNext().getNext();

        assertNotNull(ifBB.getCondition());
        assertNotNull(ifBB.getSub1());
        assertEquals(ifBB.getSub1().getNext(), END);
        assertNotNull(ifBB.getNext());

        return ifBB;
    }

    // --- Test outer & inner classes ------------------------------------------------------------------------------- //
    @Test
    public void testJdk170OuterClass() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/OuterClass", StringConstants.INSTANCE_CONSTRUCTOR));
        }
    }

    // --- Test ternary operator ------------------------------------------------------------------------------------ //
    @Test
    public void testJdk170TernaryOperatorsInTernaryOperator() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorsInTernaryOperator"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorsInReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorsInReturn"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorsInReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorsInReturn"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIf1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIf1"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionTernaryOperatorBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_TERNARY_OPERATOR, conditionTernaryOperatorBB.getType());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getCondition().getType());
            assertTrue(conditionTernaryOperatorBB.getCondition().mustInverseCondition());
            assertEquals(TYPE_GOTO_IN_TERNARY_OPERATOR, conditionTernaryOperatorBB.getSub1().getType());
            assertEquals(TYPE_STATEMENTS, conditionTernaryOperatorBB.getSub2().getType());
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIf1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIf1"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse1"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElse1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse1"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse2"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElse2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse2"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse3"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionAndBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_AND, conditionAndBB.getType());
            assertEquals(TYPE_CONDITION, conditionAndBB.getSub1().getType());
            assertTrue(conditionAndBB.getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, conditionAndBB.getSub2().getType());
            assertFalse(conditionAndBB.getSub2().mustInverseCondition());
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElse3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse3"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse4"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse5"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElse6() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElse6"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElseFalse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseFalse"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElseFalse() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseFalse"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElseANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseANDCondition"));
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElseANDCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseANDCondition"));
        }
    }

    @Test
    public void testJdk118TernaryOperatorInIfElseORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseORCondition"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionOrBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB.getSub2().getType());
            assertTrue(conditionOrBB.getSub2().mustInverseCondition());

            BasicBlock conditionOrBB2 = conditionOrBB.getSub1();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB2.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB2.getSub1().getType());
            assertFalse(conditionOrBB2.getSub1().mustInverseCondition());

            BasicBlock conditionTernaryOperatorBB = conditionOrBB2.getSub2();

            assertEquals(TYPE_CONDITION_TERNARY_OPERATOR, conditionTernaryOperatorBB.getType());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getCondition().getType());
            assertTrue(conditionTernaryOperatorBB.getCondition().mustInverseCondition());
            assertEquals(TYPE_GOTO_IN_TERNARY_OPERATOR, conditionTernaryOperatorBB.getSub1().getType());
            assertEquals(TYPE_STATEMENTS, conditionTernaryOperatorBB.getSub2().getType());
        }
    }

    @Test
    public void testJdk170TernaryOperatorInIfElseORCondition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TernaryOperator", "ternaryOperatorInIfElseORCondition"));

            BasicBlock ifElseBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());
            assertEquals(TYPE_STATEMENTS, ifElseBB.getNext().getType());
            assertEquals(TYPE_RETURN, ifElseBB.getNext().getNext().getType());

            BasicBlock conditionOrBB = ifElseBB.getCondition();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB.getSub2().getType());
            assertTrue(conditionOrBB.getSub2().mustInverseCondition());

            BasicBlock conditionOrBB2 = conditionOrBB.getSub1();

            assertEquals(TYPE_CONDITION_OR, conditionOrBB2.getType());
            assertEquals(TYPE_CONDITION, conditionOrBB2.getSub1().getType());
            assertFalse(conditionOrBB2.getSub1().mustInverseCondition());

            BasicBlock conditionTernaryOperatorBB = conditionOrBB2.getSub2();

            assertEquals(TYPE_CONDITION_TERNARY_OPERATOR, conditionTernaryOperatorBB.getType());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getCondition().getType());
            assertTrue(conditionTernaryOperatorBB.getCondition().mustInverseCondition());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getSub1().getType());
            assertTrue(conditionTernaryOperatorBB.getSub1().mustInverseCondition());
            assertEquals(TYPE_CONDITION, conditionTernaryOperatorBB.getSub2().getType());
            assertFalse(conditionTernaryOperatorBB.getSub2().mustInverseCondition());
        }
    }

    // --- Test 'switch' -------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk170SimpleSwitch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "simpleSwitch")));
        }
    }

    @Test
    public void testJdk170SwitchFirstBreakMissing() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchFirstBreakMissing")));
            SwitchCase sc0 = switchBB.getSwitchCases().get(0);

            assertFalse(sc0.isDefaultCase());
            assertEquals(0, sc0.getValue());
            assertEquals(sc0.getBasicBlock().getNext(), END);

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(1, sc1.getValue());
            assertEquals(sc0.getBasicBlock().getNext(), SWITCH_BREAK);
        }
    }

    @Test
    public void testJdk170SwitchSecondBreakMissing() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchSecondBreakMissing")));
            SwitchCase sc0 = switchBB.getSwitchCases().get(0);

            assertTrue(sc0.isDefaultCase());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, sc1.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_STATEMENTS, sc2.getBasicBlock().getType());
            assertEquals(TYPE_END, sc2.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170SwitchDefault() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchDefault")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_END, sc1.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_STATEMENTS, sc2.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, sc2.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170LookupSwitchDefault() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "lookupSwitchDefault")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());
            assertEquals(TYPE_STATEMENTS, scDefault.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, scDefault.getBasicBlock().getNext().getType());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_END, sc1.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_STATEMENTS, sc2.getBasicBlock().getType());
            assertEquals(TYPE_SWITCH_BREAK, sc2.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170SwitchOneExitInFirstCase() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOneExitInFirstCase")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());
            assertEquals(TYPE_STATEMENTS, scDefault.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, scDefault.getBasicBlock().getNext().getType());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(1, sc2.getValue());
            assertEquals(TYPE_THROW, sc2.getBasicBlock().getType());
        }
    }

    @Test
    public void testJdk170SwitchOneExitInSecondCase() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOneExitInSecondCase")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());
            assertEquals(TYPE_STATEMENTS, scDefault.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, scDefault.getBasicBlock().getNext().getType());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(0, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, sc1.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170SwitchOneExitInLastCase() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOneExitInLastCase")));
            SwitchCase sc0 = switchBB.getSwitchCases().get(0);

            assertFalse(sc0.isDefaultCase());
            assertEquals(0, sc0.getValue());
            assertEquals(TYPE_STATEMENTS, sc0.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, sc0.getBasicBlock().getNext().getType());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(1, sc1.getValue());
            assertEquals(TYPE_STATEMENTS, sc1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, sc1.getBasicBlock().getNext().getType());
        }
    }

    @Test
    public void testJdk170ComplexSwitch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock switchBB = checkSwitchReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "complexSwitch")));
            SwitchCase scDefault = switchBB.getSwitchCases().get(0);

            assertTrue(scDefault.isDefaultCase());

            SwitchCase sc1 = switchBB.getSwitchCases().get(1);

            assertFalse(sc1.isDefaultCase());
            assertEquals(1, sc1.getValue());

            SwitchCase sc2 = switchBB.getSwitchCases().get(2);

            assertFalse(sc2.isDefaultCase());
            assertEquals(2, sc2.getValue());
        }
    }

    @Test
    public void testJdk170SwitchOnLastPosition() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchOnLastPosition"));
        }
    }

    @Test
    public void testJdk170SwitchFirstIfBreakMissing() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/Switch", "switchFirstIfBreakMissing"));
        }
    }

    @Test
    public void testJdk170SwitchString() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/AdvancedSwitch", "switchString"));
        }
    }

    protected static BasicBlock checkSwitchReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock switchBB = startBB.getNext();

        assertNotNull(switchBB);
        assertEquals(TYPE_SWITCH, switchBB.getType());

        BasicBlock next = switchBB.getNext();
        assertNotNull(next);
        assertEquals(TYPE_STATEMENTS, next.getType());

        assertNotNull(next.getNext());
        assertEquals(TYPE_RETURN, next.getNext().getType());

        return switchBB;
    }

    // --- Test 'while' --------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk170SimpleWhile() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "simpleWhile"));
        }
    }

    @Test
    public void testJdk170WhileIfContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileIfContinue"));
        }
    }

    @Test
    public void testJdk170WhileIfBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileIfBreak"));
        }
    }

    @Test
    public void testJdk170WhileWhile() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileWhile"));
        }
    }

    @Test
    public void testJdk170WhileThrow() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileThrow"));
        }
    }

    @Test
    public void testJdk170WhileTrue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileTrue"));
        }
    }

    @Test
    public void testJdk170WhileTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileTryFinally"));
        }
    }

    @Test
    public void testJdk170TryWhileFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "tryWhileFinally"));
        }
    }

    @Test
    public void testJdk170InfiniteWhileTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "infiniteWhileTryFinally"));
        }
    }

    @Test
    public void testJdk170TryInfiniteWhileFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "tryInfiniteWhileFinally"));
        }
    }

    @Test
    public void testJdk170WhileTrueIf() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileTrueIf"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock firstIfBB = mainLoopBB.getSub1().getNext();

            assertEquals(TYPE_IF, firstIfBB.getType());

            BasicBlock innerLoopBB = firstIfBB.getSub1().getNext();

            assertEquals(TYPE_LOOP, innerLoopBB.getType());
            assertEquals(TYPE_LOOP_END, innerLoopBB.getNext().getType());

            BasicBlock secondIfBB = firstIfBB.getNext();

            assertEquals(TYPE_LOOP_START, secondIfBB.getSub1().getNext().getType());
        }
    }

    @Test
    public void testJdk170WhileContinueBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "whileContinueBreak"));
        }
    }

    @Test
    public void testJdk170TwoWiles() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/While", "twoWiles"));

            BasicBlock firstLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, firstLoopBB.getType());

            BasicBlock nextLoopBB = firstLoopBB.getNext();

            assertEquals(TYPE_LOOP, nextLoopBB.getType());

            BasicBlock stmtBB = nextLoopBB.getNext();

            assertEquals(TYPE_STATEMENTS, stmtBB.getType());

            BasicBlock returnBB = stmtBB.getNext();

            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    // --- Test 'do-while' ------------------------------------------------------------------------------------------ //
    @Test
    public void testJdk170DoWhileWhile() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "doWhileWhile"));
        }
    }

    @Test
    public void testJdk170DoWhileTestPreInc() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "doWhileTestPreInc"));
        }
    }

    @Test
    public void testJdk170DoWhileTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "doWhileTryFinally"));
        }
    }

    @Test
    public void testJdk170TryDoWhileFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/DoWhile", "tryDoWhileFinally"));
        }
    }

    // --- Test 'for' ----------------------------------------------------------------------------------------------- //
    @Test
    public void testJdk150ForTryReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.5.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forTryReturn"));
            BasicBlock loopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, loopBB.getType());
            assertEquals(TYPE_STATEMENTS, loopBB.getNext().getType());
            assertEquals(TYPE_RETURN, loopBB.getNext().getNext().getType());

            BasicBlock bb = loopBB.getSub1();

            assertEquals(TYPE_IF, bb.getType());
            assertEquals(TYPE_LOOP_END, bb.getNext().getType());

            bb = bb.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_TRY, bb.getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_START, bb.getNext().getNext().getNext().getType());
        }
    }

    @Test
    public void testJdk170IfForIfReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "ifForIfReturn"));
        }
    }

    @Test
    public void testJdk170ForIfContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forIfContinue"));
        }
    }

    @Test
    public void testJdk170ForIfIfContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forIfIfContinue"));
        }
    }

    @Test
    public void testJdk170ForMultipleVariables2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forMultipleVariables2"));
        }
    }

    @Test
    public void testJdk170ForBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/For", "forBreak"));
        }
    }

    // --- Test 'break' and 'continue' ------------------------------------------------------------------------------ //
    @Test
    public void testJdk170DoWhileContinue() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "doWhileContinue"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock sub1 = mainLoopBB.getSub1();

            assertEquals(TYPE_STATEMENTS, sub1.getType());
            assertEquals(TYPE_IF, sub1.getNext().getType());
            assertEquals(TYPE_IF, sub1.getNext().getSub1().getType());
            assertEquals(TYPE_IF, sub1.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_START, sub1.getNext().getNext().getNext().getType());
        }
    }

    @Test
    public void testJdk170TripleDoWhile1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "tripleDoWhile1"));
        }
    }

    @Test
    public void testJdk170TripleDoWhile2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "tripleDoWhile2"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock innerLoopBB = mainLoopBB.getSub1();

            assertEquals(TYPE_LOOP, innerLoopBB.getType());
            assertEquals(TYPE_IF, innerLoopBB.getNext().getType());
            assertEquals(TYPE_LOOP_START, innerLoopBB.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_END, innerLoopBB.getNext().getSub1().getType());

            BasicBlock innerInnerLoopBB = innerLoopBB.getSub1();

            assertEquals(TYPE_LOOP, innerInnerLoopBB.getType());
            assertEquals(TYPE_IF, innerInnerLoopBB.getNext().getType());
            assertEquals(TYPE_LOOP_START, innerInnerLoopBB.getNext().getNext().getType());
            assertEquals(TYPE_LOOP_END, innerInnerLoopBB.getNext().getSub1().getType());

            BasicBlock bb = innerInnerLoopBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_IF, bb.getNext().getNext().getType());
            assertEquals(TYPE_JUMP, bb.getNext().getNext().getSub1().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getNext().getType());
            assertEquals(TYPE_IF, bb.getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_LOOP_START, bb.getNext().getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_LOOP_END, bb.getNext().getNext().getNext().getNext().getSub1().getType());
        }
    }

    @Test
    public void testJdk170DoWhileWhileIf() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "doWhileWhileIf"));
        }
    }

    @Test
    public void testJdk170DoWhileWhileTryBreak() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/BreakContinue", "doWhileWhileTryBreak"));

            BasicBlock mainLoopBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_LOOP, mainLoopBB.getType());

            BasicBlock innerLoopBB = mainLoopBB.getSub1();

            assertEquals(TYPE_LOOP, innerLoopBB.getType());
            assertEquals(TYPE_IF, innerLoopBB.getSub1().getType());
            assertEquals(TYPE_TRY, innerLoopBB.getSub1().getSub1().getType());
            assertEquals(TYPE_IF, innerLoopBB.getSub1().getSub1().getSub1().getType());
            assertEquals(TYPE_JUMP, innerLoopBB.getSub1().getSub1().getSub1().getNext().getType());
        }
    }

    // --- Test 'try-catch-finally' --------------------------------------------------------------------------------- //
    @Test
    public void testJdk170MethodTryCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTrySwitchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTrySwitchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_SWITCH, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTrySwitchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTrySwitchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_SWITCH, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler321MethodTryCatchCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler321MethodTryCatchFinally1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally1"));
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatchExitInTry() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatchOneExitInTry")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh1.getBasicBlock().getNext().getType());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatchExitInFirstCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatchOneExitInFirstCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh1.getBasicBlock().getNext().getType());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_STATEMENTS, next.getType());
            assertEquals(TYPE_RETURN, next.getNext().getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchCatchExitInLastCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            BasicBlock tryBB = checkTryCatchFinallyReduction(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchCatchOneExitInLastCatch")));

            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh0.getBasicBlock().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_RETURN, eh1.getBasicBlock().getNext().getType());

            BasicBlock next = tryBB.getNext();

            assertEquals(TYPE_END, next.getType());
        }
    }

    @Test
    public void testJdk170MethodTrySwitchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTrySwitchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_SWITCH, bb.getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();

            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    @Test
    public void testJdk131MethodTryCatchFinallyInTryCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.3.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyInTryCatchFinally"));
        }
    }

    @Test
    public void testJdk170MethodTryCatchFinallyInTryCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyInTryCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_TRY, bb.getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getType());
            assertEquals(TYPE_TRY, bb.getNext().getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_LOOP, bb.getNext().getNext().getNext().getNext().getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getNext().getNext().getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_LOOP, eh0.getBasicBlock().getNext().getType());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getNext().getNext().getType());

            BasicBlock returnBB = tryBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN_VALUE, returnBB.getType());
        }
    }

    protected static BasicBlock checkTryCatchFinallyReduction(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock simpleBB = startBB.getNext();

        assertNotNull(simpleBB);
        assertEquals(TYPE_STATEMENTS, simpleBB.getType());

        BasicBlock tryBB = simpleBB.getNext();

        assertNotNull(tryBB);
        assertNotNull(tryBB.getSub1());
        assertNotNull(tryBB.getExceptionHandlers());
        assertEquals(2, tryBB.getExceptionHandlers().size());

        for (ExceptionHandler exceptionHandler : tryBB.getExceptionHandlers()) {
            assertNotNull(exceptionHandler.getInternalThrowableName());
            assertNotNull(exceptionHandler.getBasicBlock());
        }

        return tryBB;
    }

    // --- Test 'try-with-resources' --------------------------------------------------------------------------------- //
    @Test
    public void testJdk170Try1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "try1Resource"));
        }
    }

    @Test
    public void testJdk170TryCatch1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatch1Resource"));
        }
    }

    @Test
    public void testJdk170TryFinally1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryFinally1Resource"));
        }
    }

    @Test
    public void testJdk170TryCatchFinally1Resource() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatchFinally1Resource"));
        }
    }

    @Test
    public void testJdk170TryCatchFinally2Resources() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatchFinally2Resources"));
        }
    }

    @Test
    public void testJdk170TryCatchFinally4Resources() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryWithResources", "tryCatchFinally4Resources"));
        }
    }

    // --- methodTryFinallyReturn --- //
    @Test
    public void testJdk170MethodTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN_VALUE, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_RETURN_VALUE, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- methodTryCatch3 --- //
    @Test
    public void testJdk170MethodTryCatch3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatch3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatch3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatch3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- methodTryFinally1 --- //
    @Test
    public void testJdk170MethodTryFinally1() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally1"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_END, bb.getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextSimpleBB = tryBB.getNext();
            assertNotNull(nextSimpleBB);
            assertEquals(TYPE_STATEMENTS, nextSimpleBB.getType());

            BasicBlock returnBB = nextSimpleBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN, returnBB.getType());
        }
    }

    // --- methodTryFinally3 --- //
    @Test
    public void testJdk170MethodTryFinally3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryFinally3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryFinally3() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally3"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- methodTryFinally4 --- //
    @Test
    public void testJdk170MethodTryFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getSub1().getType());
            assertEquals(TYPE_RETURN, bb.getNext().getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getSub1().getType());
            assertEquals(TYPE_RETURN, bb.getNext().getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());
            assertEquals(TYPE_IF, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, bb.getNext().getSub1().getType());
            assertEquals(TYPE_RETURN, bb.getNext().getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    // --- methodTryCatchFinally2 --- //
    @Test
    public void testJdk170MethodTryCatchFinally2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally2"));
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatchFinally2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally2"));
            BasicBlock tryBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getSub1().getType());
            assertEquals(TYPE_END, tryBB.getSub1().getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatchFinally2() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally2"));
            BasicBlock tryBB = cfg.getStart().getNext().getNext();

            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getSub1().getType());
            assertEquals(TYPE_END, tryBB.getSub1().getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());
        }
    }

    // --- methodTryCatchFinally4 --- //
    @Test
    public void testJdk170MethodTryCatchFinally4() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally4"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_THROW, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock returnBB = tryBB.getNext();

            assertNotNull(returnBB);
            assertEquals(TYPE_RETURN_VALUE, returnBB.getType());
        }
    }

    // --- methodTryCatchFinally5 --- //
    @Test
    public void testEclipseJavaCompiler321MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    @Test
    public void testJdk170MethodTryCatchFinally5() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinally5"));
        }
    }

    // --- methodTryTryReturnFinally*Finally --- //
    @Test
    public void testJdk170MethodTryTryReturnFinallyFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryReturnFinallyFinally"));
        }
    }

    @Test
    public void testEclipseJavaCompiler321MethodTryTryReturnFinallyCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryReturnFinallyCatchFinally"));
        }
    }

    @Test
    public void testJdk170MethodTryTryReturnFinallyCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryReturnFinallyCatchFinally"));
        }
    }

    // --- methodTryTryFinallyFinallyTryFinallyReturn --- //
    @Test
    public void testEclipseJavaCompiler370MethodTryTryFinallyFinallyTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryTryFinallyFinallyTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
        }
    }

    @Test
    public void testJdk170MethodTryTryFinallyFinallyTryFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_TRY, bb.getType());
            assertEquals(TYPE_TRY, bb.getNext().getType());
            assertEquals(TYPE_END, bb.getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getType());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getNext().getNext().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    // --- complexMethodTryCatchCatchFinally --- //
    @Test
    public void testJdk170MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testEclipseJavaCompiler321ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.2.1.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
        }
    }

    @Test
    public void testHarmonyJdkR533500ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-harmony-jdk-r533500.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testIbm_J9_VmComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-ibm-j9_vm.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk118ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk131ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.3.1.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk142ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.4.2.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_JSR, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(3, tryBB.getExceptionHandlers().size());

            assertEquals(TYPE_TRY, tryBB.getSub1().getType());
            assertEquals(TYPE_END, tryBB.getSub1().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNotNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
            assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getType());
            assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getNext().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNotNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
            assertEquals(TYPE_TRY, eh1.getBasicBlock().getNext().getType());
            assertEquals(TYPE_END, eh1.getBasicBlock().getNext().getNext().getType());

            ExceptionHandler eh2 = tryBB.getExceptionHandlers().get(2);

            assertNull(eh2.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getType());
            assertEquals(TYPE_JSR, eh2.getBasicBlock().getNext().getType());
            assertEquals(TYPE_THROW, eh2.getBasicBlock().getNext().getNext().getType());
            assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getNext().getBranch().getType());
            assertEquals(TYPE_TRY, eh2.getBasicBlock().getNext().getBranch().getNext().getType());
            assertEquals(TYPE_RET, eh2.getBasicBlock().getNext().getBranch().getNext().getNext().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_STATEMENTS, nextBB.getType());
            assertEquals(TYPE_RETURN, nextBB.getNext().getType());
        }
    }

    @Test
    public void testJdk150ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.5.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk160ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.6.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJdk170ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJikes1_22_1WindowsComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jikes-1.22-1.windows.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK118(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    @Test
    public void testJRockit90_150_06ComplexMethodTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jrockit-90_150_06.zip")) {
            checkComplexMethodTryCatchCatchFinally_JDK5(checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "complexMethodTryCatchCatchFinally")));
        }
    }

    protected void checkComplexMethodTryCatchCatchFinally_JDK5(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock simpleBB = startBB.getNext();

        assertEquals(TYPE_STATEMENTS, simpleBB.getType());

        BasicBlock tryBB = simpleBB.getNext();

        assertNotNull(tryBB);
        assertEquals(TYPE_TRY, tryBB.getType());
        assertNotNull(tryBB.getExceptionHandlers());
        assertEquals(3, tryBB.getExceptionHandlers().size());

        BasicBlock bb = tryBB.getSub1();

        assertEquals(TYPE_TRY, bb.getType());
        assertEquals(TYPE_TRY, bb.getNext().getType());
        assertEquals(TYPE_END, bb.getNext().getNext().getType());
        assertEquals(3, bb.getExceptionHandlers().size());
        assertEquals(3, bb.getNext().getExceptionHandlers().size());

        ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

        assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
        assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getType());
        assertEquals(TYPE_TRY, eh0.getBasicBlock().getNext().getNext().getType());
        assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getNext().getNext().getType());
        assertEquals(3, eh0.getBasicBlock().getNext().getExceptionHandlers().size());
        assertEquals(3, eh0.getBasicBlock().getNext().getNext().getExceptionHandlers().size());

        ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

        assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
        assertEquals(TYPE_TRY, eh1.getBasicBlock().getNext().getType());
        assertEquals(TYPE_TRY, eh1.getBasicBlock().getNext().getNext().getType());
        assertEquals(TYPE_END, eh1.getBasicBlock().getNext().getNext().getNext().getType());
        assertEquals(3, eh1.getBasicBlock().getNext().getExceptionHandlers().size());
        assertEquals(3, eh1.getBasicBlock().getNext().getNext().getExceptionHandlers().size());

        ExceptionHandler eh2 = tryBB.getExceptionHandlers().get(2);

        assertNull(eh2.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getType());
        assertEquals(TYPE_TRY, eh2.getBasicBlock().getNext().getType());
        assertEquals(TYPE_THROW, eh2.getBasicBlock().getNext().getNext().getType());
        assertEquals(3, eh2.getBasicBlock().getNext().getExceptionHandlers().size());

        BasicBlock nextBB = tryBB.getNext();

        assertNotNull(nextBB);
        assertEquals(TYPE_STATEMENTS, nextBB.getType());
        assertEquals(TYPE_RETURN, nextBB.getNext().getType());
    }

    protected void checkComplexMethodTryCatchCatchFinally_JDK118(ControlFlowGraph cfg) throws Exception {
        BasicBlock startBB = cfg.getStart();

        assertNotNull(startBB);

        BasicBlock simpleBB = startBB.getNext();

        assertEquals(TYPE_STATEMENTS, simpleBB.getType());

        BasicBlock tryBB = simpleBB.getNext();

        assertNotNull(tryBB);
        assertEquals(TYPE_TRY_JSR, tryBB.getType());
        assertNotNull(tryBB.getExceptionHandlers());
        assertEquals(3, tryBB.getExceptionHandlers().size());

        BasicBlock sub1 = tryBB.getSub1();

        assertEquals(TYPE_TRY_JSR, sub1.getType());
        assertEquals(TYPE_END, sub1.getNext().getType());
        assertEquals(3, sub1.getExceptionHandlers().size());

        ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

        assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());
        assertEquals(TYPE_TRY_JSR, eh0.getBasicBlock().getNext().getType());
        assertEquals(TYPE_END, eh0.getBasicBlock().getNext().getNext().getType());
        assertEquals(3, eh0.getBasicBlock().getNext().getExceptionHandlers().size());

        ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

        assertEquals(StringConstants.JAVA_LANG_EXCEPTION, eh1.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh1.getBasicBlock().getType());
        assertEquals(TYPE_TRY_JSR, eh1.getBasicBlock().getNext().getType());
        assertEquals(TYPE_END, eh1.getBasicBlock().getNext().getNext().getType());
        assertEquals(3, eh1.getBasicBlock().getNext().getExceptionHandlers().size());

        ExceptionHandler eh2 = tryBB.getExceptionHandlers().get(2);

        assertNull(eh2.getInternalThrowableName());
        assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getType());
        assertEquals(TYPE_JSR, eh2.getBasicBlock().getNext().getType());
        assertEquals(TYPE_THROW, eh2.getBasicBlock().getNext().getNext().getType());
        assertEquals(TYPE_STATEMENTS, eh2.getBasicBlock().getNext().getBranch().getType());
        assertEquals(TYPE_TRY_JSR, eh2.getBasicBlock().getNext().getBranch().getNext().getType());
        assertEquals(3, eh2.getBasicBlock().getNext().getBranch().getNext().getExceptionHandlers().size());

        BasicBlock nextBB = tryBB.getNext();

        assertNotNull(nextBB);
        assertEquals(TYPE_STATEMENTS, nextBB.getType());
        assertEquals(TYPE_RETURN, nextBB.getNext().getType());
    }

    // --- methodIfIfTryCatch --- //
    @Test
    public void testJdk118MethodIfIfTryCatch() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodIfIfTryCatch"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock ifBB = startBB.getNext();

            assertEquals(TYPE_IF, ifBB.getType());
            assertEquals(TYPE_RETURN, ifBB.getNext().getType());

            BasicBlock ifElseBB = ifBB.getSub1();

            assertEquals(TYPE_IF_ELSE, ifElseBB.getType());

            assertEquals(TYPE_STATEMENTS, ifElseBB.getSub1().getType());
            assertEquals(TYPE_TRY, ifElseBB.getSub1().getNext().getType());
            assertEquals(TYPE_END, ifElseBB.getSub1().getNext().getNext().getType());

            assertEquals(TYPE_STATEMENTS, ifElseBB.getSub2().getType());
            assertEquals(TYPE_END, ifElseBB.getSub2().getNext().getType());
        }
    }

    // --- methodTryCatchFinallyReturn --- //
    @Test
    public void testJdk170MethodTryCatchFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_RETURN_VALUE, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_RETURN_VALUE, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler370MethodTryCatchFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryCatchFinallyReturn() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(2, tryBB.getExceptionHandlers().size());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_STATEMENTS, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertEquals(StringConstants.JAVA_LANG_RUNTIME_EXCEPTION, eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            ExceptionHandler eh1 = tryBB.getExceptionHandlers().get(1);

            assertNull(eh1.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh1.getBasicBlock().getType());

            BasicBlock nextBB = tryBB.getNext();

            assertNotNull(nextBB);
            assertEquals(TYPE_END, nextBB.getType());
        }
    }

    // --- complexMethodTryFinallyReturn --- //
    @Test
    public void testJdk170MethodComplexTryCatchCatchFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinallyReturn"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getType());

            BasicBlock bb = tryBB.getSub1();

            assertEquals(TYPE_TRY, bb.getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            BasicBlock subTryBB = tryBB.getSub1();

            assertNotNull(subTryBB);
            assertEquals(TYPE_TRY, subTryBB.getType());
            assertEquals(TYPE_TRY, subTryBB.getNext().getType());
            assertEquals(TYPE_END, subTryBB.getNext().getNext().getType());
        }
    }

    @Test
    public void testJdk170MethodTryCatchTryCatchThrow() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.7.0.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryCatchTryCatchThrow"));
        }
    }

    // --- methodTryTryFinallyFinallyTryFinally --- //
    @Test
    public void testEclipseJavaCompiler370MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.7.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            BasicBlock subTryBB = tryBB.getSub1();

            assertEquals(TYPE_TRY_ECLIPSE, subTryBB.getType());
            assertNotNull(subTryBB.getExceptionHandlers());
            assertEquals(1, subTryBB.getExceptionHandlers().size());
            assertEquals(TYPE_STATEMENTS, subTryBB.getNext().getType());
            assertEquals(TYPE_END, subTryBB.getNext().getNext().getType());

            eh0 = subTryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());
        }
    }

    @Test
    public void testEclipseJavaCompiler3130MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-eclipse-java-compiler-3.13.0.zip")) {
            ControlFlowGraph cfg = checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
            BasicBlock startBB = cfg.getStart();

            assertNotNull(startBB);

            BasicBlock simpleBB = startBB.getNext();

            assertEquals(TYPE_STATEMENTS, simpleBB.getType());

            BasicBlock tryBB = simpleBB.getNext();

            assertNotNull(tryBB);
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getType());
            assertNotNull(tryBB.getExceptionHandlers());
            assertEquals(1, tryBB.getExceptionHandlers().size());
            assertEquals(TYPE_TRY_ECLIPSE, tryBB.getNext().getType());
            assertEquals(TYPE_STATEMENTS, tryBB.getNext().getNext().getType());
            assertEquals(TYPE_RETURN, tryBB.getNext().getNext().getNext().getType());

            ExceptionHandler eh0 = tryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_STATEMENTS, eh0.getBasicBlock().getType());

            BasicBlock subTryBB = tryBB.getSub1();

            assertEquals(TYPE_TRY_ECLIPSE, subTryBB.getType());
            assertNotNull(subTryBB.getExceptionHandlers());
            assertEquals(1, subTryBB.getExceptionHandlers().size());
            assertEquals(TYPE_STATEMENTS, subTryBB.getNext().getType());
            assertEquals(TYPE_END, subTryBB.getNext().getNext().getType());

            eh0 = subTryBB.getExceptionHandlers().get(0);

            assertNull(eh0.getInternalThrowableName());
            assertEquals(TYPE_THROW, eh0.getBasicBlock().getType());
        }
    }

    @Test
    public void testJdk118MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.1.8.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
        }
    }

    @Test
    public void testJdk131MethodTryTryFinallyFinallyTryFinally() throws Exception {
        try (InputStream resource = getResource("zip/data-java-jdk-1.3.1.zip")) {
            checkCFGReduction(searchMethod(resource, "org/jd/core/test/TryCatchFinally", "methodTryTryFinallyFinallyTryFinally"));
        }
    }

    protected ControlFlowGraph checkCFGReduction(Method method) throws Exception {
        ControlFlowGraph cfg = ControlFlowGraphMaker.make(method);

        assertNotNull(cfg);

        BasicBlock root = cfg.getStart();

        assertNotNull(root);

        String plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
        System.out.println("Step 0: " + ControlFlowGraphPlantUMLWriter.writePlantUMLUrl(plantuml));

        ControlFlowGraphGotoReducer.reduce(cfg);

        plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
        System.out.println("Step 1: " + ControlFlowGraphPlantUMLWriter.writePlantUMLUrl(plantuml));

        // --- Test natural loops --- //
        BitSet[] dominators = ControlFlowGraphLoopReducer.buildDominatorIndexes(cfg);
        List<Loop> naturalLoops = ControlFlowGraphLoopReducer.identifyNaturalLoops(cfg, dominators);

        for (Loop loop : naturalLoops) {
            System.out.println(loop);
        }

        ControlFlowGraphLoopReducer.reduce(cfg);
        plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
        System.out.println("Step 2: " + ControlFlowGraphPlantUMLWriter.writePlantUMLUrl(plantuml));

        BitSet visited = new BitSet();
        BitSet jsrTargets = new BitSet();

        for (int i=0, count=3; i<5; i++, count++) {
            List<ControlFlowGraphReducer> preferredReducers = ControlFlowGraphReducer.getPreferredReducers(method);
            
            boolean reduced = false;
            
            for (ControlFlowGraphReducer controlFlowGraphReducer : preferredReducers) {
            
                reduced = controlFlowGraphReducer.reduce(visited, cfg.getStart(), jsrTargets);
    
                System.out.println("# of visited blocks: " + visited.cardinality());
                visited.clear();
                plantuml = ControlFlowGraphPlantUMLWriter.write(cfg);
                System.out.println("Step " + count + ": " + ControlFlowGraphPlantUMLWriter.writePlantUMLUrl(plantuml));
    
                if (reduced) {
                    break;
                }
            }
            
            if (reduced) {
                break;
            }
        }

        checkFinalCFG(cfg);

        return cfg;
    }

    protected static void checkFinalCFG(ControlFlowGraph cfg) {
        List<BasicBlock> list = cfg.getBasicBlocks();

        for (int i=0, len=list.size(); i<len; i++) {
            BasicBlock basicBlock = list.get(i);

            if (basicBlock.getType() == TYPE_DELETED) {
                continue;
            }

            assertEquals("#" + basicBlock.getIndex() + " contains an invalid index", basicBlock.getIndex(), i);
            assertNotEquals("#" + basicBlock.getIndex() + " is a TRY_DECLARATION -> reduction failed", TYPE_TRY_DECLARATION, basicBlock.getType());
            assertNotEquals("#" + basicBlock.getIndex() + " is a SWITCH_DECLARATION -> reduction failed", TYPE_SWITCH_DECLARATION, basicBlock.getType());
            assertNotEquals("#" + basicBlock.getIndex() + " is a CONDITIONAL -> reduction failed", TYPE_CONDITIONAL_BRANCH, basicBlock.getType());
            assertNotEquals("#" + basicBlock.getIndex() + " is a GOTO -> reduction failed", TYPE_GOTO, basicBlock.getType());

            if (!basicBlock.matchType(GROUP_CONDITION)) {
                for (BasicBlock predecessor : basicBlock.getPredecessors()) {
                    assertTrue("#" + predecessor.getIndex() + " is a predecessor of #" + basicBlock.getIndex() + " but #" + predecessor.getIndex() + " does not refer it", predecessor.contains(basicBlock));
                }
            }

            if (basicBlock.matchType(TYPE_IF|TYPE_IF_ELSE)) {
                assertNotSame("#" + basicBlock.getIndex() + " is a IF or a IF_ELSE with a 'then' branch jumping to itself", basicBlock.getSub1(), basicBlock);
            }

            if (basicBlock.getType() == TYPE_IF_ELSE) {
                assertNotSame("#" + basicBlock.getIndex() + " is a IF_ELSE with a 'else' branch jumping to itself", basicBlock.getSub2(), basicBlock);
            }

            if (basicBlock.matchType(TYPE_TRY|TYPE_TRY_JSR)) {
                boolean containsFinally = false;
                Set<String> exceptionNames = new HashSet<>();
                int maxOffset = 0;

                for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    String name = exceptionHandler.getInternalThrowableName();
                    int offset = exceptionHandler.getBasicBlock().getFromOffset();

                    if (name == null) {
                        assertFalse("#" + basicBlock.getIndex() + " contains multiple finally handlers", containsFinally);
                        containsFinally = true;
                    } else {
                        assertFalse("#" + basicBlock.getIndex() + " contains multiple handlers for " + name, exceptionNames.contains(name));
                        exceptionNames.add(name);
                    }

                    assertFalse("#" + basicBlock.getIndex() + " have an invalid exception handler", exceptionHandler.getBasicBlock().matchType(GROUP_CONDITION));

                    if (maxOffset < offset) {
                        maxOffset = offset;
                    }
                }

                for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    BasicBlock bb = exceptionHandler.getBasicBlock();
                    int offset = bb.getFromOffset();

                    if (maxOffset != offset) {
                        // Search last offset
                        BasicBlock next = bb.getNext();

                        while ((bb != next) && next.matchType(GROUP_SINGLE_SUCCESSOR|TYPE_RETURN|TYPE_RETURN_VALUE|TYPE_THROW) && (next.getPredecessors().size() == 1)) {
                            bb = next;
                            next = next.getNext();
                            offset = bb.getFromOffset();
                        }

                        assertTrue("#" + basicBlock.getIndex() + " is a TRY or TRY_WITH_JST -> #" + exceptionHandler.getBasicBlock().getIndex() + " handler reduction failed", offset < maxOffset);
                    }
                }
            }

            if (basicBlock.getType() == TYPE_SWITCH) {
                for (SwitchCase switchCase : basicBlock.getSwitchCases()) {
                    assertFalse("#" + basicBlock.getIndex() + " have an invalid switch case", switchCase.getBasicBlock().matchType(GROUP_CONDITION));
                }
            }

            if (!basicBlock.matchType(GROUP_CONDITION)) {
                assertTrue("#" + basicBlock.getIndex() + " have an invalid next basic block", (basicBlock.getNext() == null) || !basicBlock.getNext().matchType(GROUP_CONDITION));
                assertTrue("#" + basicBlock.getIndex() + " have an invalid branch basic block", (basicBlock.getBranch() == null) || !basicBlock.getBranch().matchType(GROUP_CONDITION));
                assertTrue("#" + basicBlock.getIndex() + " have an invalid sub1 basic block", (basicBlock.getSub1() == null) || !basicBlock.getSub1().matchType(GROUP_CONDITION));
                assertTrue("#" + basicBlock.getIndex() + " have an invalid sub2 basic block", (basicBlock.getSub2() == null) || !basicBlock.getSub2().matchType(GROUP_CONDITION));
            }
        }

        BitSet visited = new BitSet(list.size());
        SilentWatchDog watchdog = new SilentWatchDog();
        BasicBlock result = checkBasicBlock(visited, cfg.getStart(), watchdog);

        assertFalse("DELETED basic block detected -> reduction failed", (result != null) && (result.getType() == TYPE_DELETED));
        assertFalse("Cycle detected -> reduction failed", (result != null) && (result.getType() != TYPE_DELETED));
    }

    protected static BasicBlock checkBasicBlock(BitSet visited, BasicBlock basicBlock, SilentWatchDog watchdog) {
        if ((basicBlock == null) || basicBlock.matchType(TYPE_END|TYPE_SWITCH_BREAK|TYPE_LOOP_START|TYPE_LOOP_CONTINUE|TYPE_LOOP_END|TYPE_RETURN)) {
            return null;
        }
        BasicBlock result;

        visited.set(basicBlock.getIndex());

        switch (basicBlock.getType()) {
            case TYPE_DELETED:
                return basicBlock;
            case TYPE_SWITCH_DECLARATION:
            case TYPE_SWITCH:
                for (SwitchCase switchCase : basicBlock.getSwitchCases()) {
                    result = checkBasicBlock(visited, basicBlock, switchCase.getBasicBlock(), watchdog);
                    if (result != null)
                        return result;
                }
                return null;
            case TYPE_TRY:
            case TYPE_TRY_JSR:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getSub1(), watchdog);
                if (result != null)
                    return result;
            case TYPE_TRY_DECLARATION:
                for (ExceptionHandler exceptionHandler : basicBlock.getExceptionHandlers()) {
                    result = checkBasicBlock(visited, basicBlock, exceptionHandler.getBasicBlock(), watchdog);
                    if (result != null)
                        return result;
                }
                return checkBasicBlock(visited, basicBlock, basicBlock.getNext(), watchdog);
            case TYPE_CONDITIONAL_BRANCH:
            case TYPE_JSR:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getNext(), watchdog);
                if (result != null)
                    return result;
                return checkBasicBlock(visited, basicBlock, basicBlock.getBranch(), watchdog);
            case TYPE_IF_ELSE:
            case TYPE_TERNARY_OPERATOR:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getSub2(), watchdog);
                if (result != null)
                    return result;
            case TYPE_IF:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getCondition(), watchdog);
                if (result != null)
                    return result;
            case TYPE_LOOP:
                result = checkBasicBlock(visited, basicBlock, basicBlock.getSub1(), watchdog);
                if (result != null)
                    return result;
            case TYPE_START:
            case TYPE_STATEMENTS:
            case TYPE_GOTO:
                return checkBasicBlock(visited, basicBlock, basicBlock.getNext(), watchdog);
            default:
                return null;
        }
    }

    protected static BasicBlock checkBasicBlock(BitSet visited, BasicBlock parent, BasicBlock child, SilentWatchDog watchdog) {
        if (!child.matchType(BasicBlock.GROUP_END) && !watchdog.silentCheck(parent, child)) {
            return parent;
        }

        return checkBasicBlock(visited, child, watchdog);
    }

    protected static class SilentWatchDog extends WatchDog {
        public boolean silentCheck(BasicBlock parent, BasicBlock child) {
            if (!child.matchType(BasicBlock.GROUP_END)) {
                Link link = new Link(parent, child);

                if (links.contains(link)) {
                    return false;
                }

                links.add(link);
            }

            return true;
        }
    }

    protected InputStream getResource(String zipName) {
        return this.getClass().getResourceAsStream("/" + zipName);
    }

    protected InputStream loadFile(String zipName) throws IOException {
        return new FileInputStream(zipName);
    }

    protected Method searchMethod(String internalTypeName, String methodName) throws Exception {
        return searchMethod(loader, typeMaker, internalTypeName, methodName, null);
    }

    protected Method searchMethod(InputStream is, String internalTypeName, String methodName) throws Exception {
        return searchMethod(is, internalTypeName, methodName, null);
    }

    protected Method searchMethod(InputStream is, String internalTypeName, String methodName, String methodDescriptor) throws Exception {
        if (is == null) {
            return null;
        }
        ZipLoader loader = new ZipLoader(is);
        TypeMaker typeMaker = new TypeMaker(loader);
        return searchMethod(loader, typeMaker, internalTypeName, methodName, methodDescriptor);
    }

    protected Method searchMethod(Loader loader, TypeMaker typeMaker, String internalTypeName, String methodName, String methodDescriptor) throws Exception {
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setMainInternalTypeName(internalTypeName);
        decompileContext.setLoader(loader);
        decompileContext.setTypeMaker(typeMaker);

        ClassFile classFile = deserializer.loadClassFile(loader, internalTypeName);
        decompileContext.setClassFile(classFile);

        CompilationUnit compilationUnit = converter.process(classFile, typeMaker, decompileContext);

        assertNotNull(compilationUnit);

        BaseTypeDeclaration typeDeclarations = compilationUnit.getTypeDeclarations();
        BodyDeclaration bodyDeclaration = null;

        if (typeDeclarations instanceof EnumDeclaration) {
            bodyDeclaration = ((EnumDeclaration)typeDeclarations).getBodyDeclaration();
        } else if (typeDeclarations instanceof AnnotationDeclaration) {
            bodyDeclaration = ((AnnotationDeclaration)typeDeclarations).getBodyDeclaration();
        } else if (typeDeclarations instanceof InterfaceDeclaration) {
            bodyDeclaration = ((InterfaceDeclaration)typeDeclarations).getBodyDeclaration();
        }

        if (bodyDeclaration != null) {
            ClassFileBodyDeclaration cfbd = (ClassFileBodyDeclaration) bodyDeclaration;

            for (ClassFileMemberDeclaration md : cfbd.getMethodDeclarations()) {
                if (md instanceof ClassFileMethodDeclaration) {
                    ClassFileMethodDeclaration cfmd = (ClassFileMethodDeclaration) md;
                    if (cfmd.getName().equals(methodName)) {
                        if ((methodDescriptor == null) || cfmd.getDescriptor().equals(methodDescriptor)) {
                            return cfmd.getMethod();
                        }
                    }
                } else if (md instanceof ClassFileConstructorDeclaration) {
                    ClassFileConstructorDeclaration cfcd = (ClassFileConstructorDeclaration) md;
                    if (cfcd.getMethod().getName().equals(methodName)) {
                        if ((methodDescriptor == null) || cfcd.getDescriptor().equals(methodDescriptor)) {
                            return cfcd.getMethod();
                        }
                    }
                } else if (md instanceof ClassFileStaticInitializerDeclaration) {
                    ClassFileStaticInitializerDeclaration cfsid = (ClassFileStaticInitializerDeclaration) md;
                    if (cfsid.getMethod().getName().equals(methodName)) {
                        return cfsid.getMethod();
                    }
                }
            }
        }

        return null;
    }
}
