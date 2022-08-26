package org.jd.core.v1;

import java.io.FileFilter;

public abstract class AbstractAppender {
    public abstract static class Builder<B extends Builder<B>> {
        FileFilter filter;

        public B setFilter(FileFilter filter) {
            this.filter = filter;
            return asBuilder();
        }

        @SuppressWarnings("unchecked")
        B asBuilder() {
            return (B) this;
        }
    }
}
