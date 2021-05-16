package jd.core.process.writer;

import java.util.Comparator;

import jd.core.model.reference.Reference;

public class ReferenceByCountComparator implements Comparator<Reference>
{
	public int compare(Reference o1, Reference o2) {
		return o2.getCounter() - o1.getCounter();
	}
}
