package org.jd.core.v1;

public class TestEquals {
    
    private String text;
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TestEquals that = (TestEquals) o;
        return !((this.text != null) ? !this.text.equals(that.text) : (that.text != null));
    }
}