/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.model.javafragment;

import org.jd.core.v1.model.fragment.FlexibleFragment;
import org.jd.core.v1.util.DefaultList;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportsFragment extends FlexibleFragment implements JavaFragment {

    private final Map<String, Import> importMap = new HashMap<>();

    public ImportsFragment(int weight) {
        super(0, -1, -1, weight, "Imports");
    }

    public void addImport(String internalName, String qualifiedName) {
        Import imp = importMap.get(internalName);

        if (imp == null) {
            importMap.put(internalName, new Import(internalName, qualifiedName));
        } else {
            imp.incCounter();
        }
    }

    public boolean incCounter(String internalName) {
        Import imp = importMap.get(internalName);

        if (imp == null) {
            return false;
        }
        imp.incCounter();
        return true;
    }

    public boolean isEmpty() {
        return importMap.isEmpty();
    }

    public void initLineCounts() {
        maximalLineCount = initialLineCount = lineCount = importMap.size();
    }

    public boolean contains(String internalName) {
        return importMap.containsKey(internalName);
    }

    @Override
    public int getLineCount() {
        if (lineCount == -1) {
            throw new IllegalStateException("Call initLineCounts() before");
        }
        return lineCount;
    }

    public Collection<Import> getImports() {
        int lineCount = getLineCount();
        int size = importMap.size();

        if (lineCount >= size) {
            return importMap.values();
        }
        DefaultList<Import> imports = new DefaultList<>(importMap.values());
        imports.sort(Comparator.comparing(Import::getCounter).reversed());
        // Remove less used imports
        List<Import> subList = imports.subList(lineCount, size);
        for (Import imp0rt : subList) {
            importMap.remove(imp0rt.getInternalName());
        }
        subList.clear();
        return imports;
    }

    public static class Import {
        private final String internalName;
        private final String qualifiedName;
        private int counter;

        public Import(String internalName, String qualifiedName) {
            this.internalName = internalName;
            this.qualifiedName = qualifiedName;
            this.counter = 1;
        }

        public String getInternalName() {
            return internalName;
        }
        public String getQualifiedName() {
            return qualifiedName;
        }
        public int getCounter() {
            return counter;
        }
        public void incCounter() {
            counter++;
        }
    }

    @Override
    public void accept(JavaFragmentVisitor visitor) {
        visitor.visit(this);
    }

}
