package org.jd.core.v1;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExpectedExceptionMatcherBuilder {
    private final List<Matcher<?>> matchers = new ArrayList<Matcher<?>>();

    @SuppressWarnings({ "unchecked", "rawtypes", "unused" })
    private List<Matcher<? super Throwable>> castedMatchers() {
        return new ArrayList<Matcher<? super Throwable>>((Collection) matchers);
    }
}