/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.layouter.visitor;

import org.jd.core.v1.model.fragment.EndFlexibleBlockFragment;
import org.jd.core.v1.model.fragment.EndMovableBlockFragment;
import org.jd.core.v1.model.fragment.FixedFragment;
import org.jd.core.v1.model.fragment.FlexibleFragment;
import org.jd.core.v1.model.fragment.FragmentVisitor;
import org.jd.core.v1.model.fragment.SpacerBetweenMovableBlocksFragment;
import org.jd.core.v1.model.fragment.StartFlexibleBlockFragment;
import org.jd.core.v1.model.fragment.StartMovableBlockFragment;
import org.jd.core.v1.service.layouter.model.Section;
import org.jd.core.v1.util.DefaultList;

public class BuildSectionsVisitor implements FragmentVisitor {
    private final DefaultList<Section> sections = new DefaultList<>();
    private DefaultList<FlexibleFragment> flexibleFragments = new DefaultList<>();
    private Section previousSection;

    @Override
    public void visit(FlexibleFragment fragment) { flexibleFragments.add(fragment); }
    @Override
    public void visit(EndFlexibleBlockFragment fragment) { flexibleFragments.add(fragment); }
    @Override
    public void visit(EndMovableBlockFragment fragment) { flexibleFragments.add(fragment); }
    @Override
    public void visit(SpacerBetweenMovableBlocksFragment fragment) { flexibleFragments.add(fragment); }
    @Override
    public void visit(StartFlexibleBlockFragment fragment) { flexibleFragments.add(fragment); }
    @Override
    public void visit(StartMovableBlockFragment fragment) { flexibleFragments.add(fragment); }

    @Override
    public void visit(FixedFragment fragment) {
        previousSection = new Section(flexibleFragments, fragment, previousSection);
        sections.add(previousSection);
        flexibleFragments = new DefaultList<>();
    }

    public DefaultList<Section> getSections() {
        sections.add(new Section(flexibleFragments, null, previousSection));
        return sections;
    }
}
