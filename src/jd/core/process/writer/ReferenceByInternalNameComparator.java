package jd.core.process.writer;

import java.util.Comparator;

import jd.core.model.reference.Reference;

public class ReferenceByInternalNameComparator implements Comparator<Reference>
{
	public int compare(Reference o1, Reference o2) {
		return o1.getInternalName().compareTo(o2.getInternalName());
	}
}
