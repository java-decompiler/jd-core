/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.model.fragment.FlexibleFragment;
import org.jd.core.v1.model.fragment.Fragment;
import org.jd.core.v1.model.fragment.SpacerBetweenMovableBlocksFragment;
import org.jd.core.v1.service.layouter.visitor.UpdateSpacerBetweenMovableBlocksVisitor;
import org.jd.core.v1.util.DefaultList;
import org.junit.Test;

import static org.jd.core.v1.model.javafragment.EndMovableJavaBlockFragment.END_MOVABLE_BLOCK;
import static org.jd.core.v1.model.javafragment.StartMovableJavaBlockFragment.START_MOVABLE_FIELD_BLOCK;
import static org.jd.core.v1.model.javafragment.StartMovableJavaBlockFragment.START_MOVABLE_METHOD_BLOCK;
import static org.jd.core.v1.model.javafragment.StartMovableJavaBlockFragment.START_MOVABLE_TYPE_BLOCK;

import junit.framework.TestCase;

public class UpdateSpacerBetweenMovableBlocksVisitorTest extends TestCase {
    @Test
    public void test() {
        UpdateSpacerBetweenMovableBlocksVisitor visitor = new UpdateSpacerBetweenMovableBlocksVisitor();
        DefaultList<FlexibleFragment> fragments = new DefaultList<>();
        FlexibleFragment spacer1, spacer2, spacer3, spacer4, spacer5, spacer6, spacer7;

        fragments.add(START_MOVABLE_TYPE_BLOCK);
            fragments.add(START_MOVABLE_FIELD_BLOCK);
            fragments.add(END_MOVABLE_BLOCK);
            fragments.add(spacer1 = new SpacerBetweenMovableBlocksFragment(0, 2, Integer.MAX_VALUE, 7, "Spacer 1"));
            fragments.add(START_MOVABLE_FIELD_BLOCK);
            fragments.add(END_MOVABLE_BLOCK);

            fragments.add(spacer2 = new SpacerBetweenMovableBlocksFragment(0, 2, Integer.MAX_VALUE, 7, "Spacer 2"));

            fragments.add(START_MOVABLE_METHOD_BLOCK);
            fragments.add(END_MOVABLE_BLOCK);

            fragments.add(spacer3 = new SpacerBetweenMovableBlocksFragment(0, 2, Integer.MAX_VALUE, 7, "Spacer 3"));

            fragments.add(START_MOVABLE_FIELD_BLOCK);
            fragments.add(END_MOVABLE_BLOCK);

            fragments.add(spacer4 = new SpacerBetweenMovableBlocksFragment(0, 2, Integer.MAX_VALUE, 7, "Spacer 4"));

            fragments.add(START_MOVABLE_TYPE_BLOCK);
                fragments.add(START_MOVABLE_FIELD_BLOCK);
                fragments.add(END_MOVABLE_BLOCK);
                fragments.add(spacer5 = new SpacerBetweenMovableBlocksFragment(0, 2, Integer.MAX_VALUE, 7, "Spacer 5"));
                fragments.add(START_MOVABLE_FIELD_BLOCK);
                fragments.add(END_MOVABLE_BLOCK);

                fragments.add(spacer6 = new SpacerBetweenMovableBlocksFragment(0, 2, Integer.MAX_VALUE, 7, "Spacer 6"));

                fragments.add(START_MOVABLE_METHOD_BLOCK);
                fragments.add(END_MOVABLE_BLOCK);
            fragments.add(END_MOVABLE_BLOCK);

            fragments.add(spacer7 = new SpacerBetweenMovableBlocksFragment(0, 2, Integer.MAX_VALUE, 7, "Spacer 7"));

            fragments.add(START_MOVABLE_METHOD_BLOCK);
            fragments.add(END_MOVABLE_BLOCK);
        fragments.add(END_MOVABLE_BLOCK);

        visitor.reset();

        for (Fragment fragment : fragments) {
            fragment.accept(visitor);
        }

        assertEquals(1, spacer1.getInitialLineCount());
        assertEquals(2, spacer2.getInitialLineCount());
        assertEquals(2, spacer3.getInitialLineCount());
        assertEquals(2, spacer4.getInitialLineCount());
        assertEquals(1, spacer5.getInitialLineCount());
        assertEquals(2, spacer6.getInitialLineCount());
        assertEquals(2, spacer7.getInitialLineCount());
    }
}