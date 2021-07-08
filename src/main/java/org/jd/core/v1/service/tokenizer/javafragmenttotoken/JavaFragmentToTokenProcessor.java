/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.tokenizer.javafragmenttotoken;

import org.jd.core.v1.model.javafragment.JavaFragment;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.visitor.TokenizeJavaFragmentVisitor;
import org.jd.core.v1.util.DefaultList;

import java.util.List;

/**
 * Convert a list of fragments to a list of tokens.<br><br>
 *
 * Input:  List<{@link org.jd.core.v1.model.fragment.Fragment}><br>
 * Output: List<{@link org.jd.core.v1.model.token.Token}><br>
 */
public class JavaFragmentToTokenProcessor {

    public DefaultList<Token> process(List<JavaFragment> fragments) {
        TokenizeJavaFragmentVisitor visitor = new TokenizeJavaFragmentVisitor(fragments.size() * 3);

        // Create tokens
        for (JavaFragment fragment : fragments) {
            fragment.accept(visitor);
        }

        return visitor.getTokens();
    }
}
