package org.jd.core.v1;

public abstract class AbstractFileAppender extends AbstractAppender {
    public abstract static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B> {
        int bufferSize;

        public B setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return asBuilder();
        }
    }
}
