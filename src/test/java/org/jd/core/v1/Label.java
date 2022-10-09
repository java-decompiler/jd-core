package org.jd.core.v1;

public class Label {
    private String iZeroOffsetParseText;

    @SuppressWarnings("unused")
    public int parseInto(CharSequence text, int position) {
        int limit = text.length() - position;

        zeroOffset: {
            if (iZeroOffsetParseText.length() == 0) {
                if (limit > 0) {
                    char c = text.charAt(position);
                    if (c == '-' || c == '+') {
                        break zeroOffset;
                    }
                }
                return position;
            }
        }
        boolean negative;
        char c = text.charAt(position);
        if (c == '-') {
            negative = true;
        } else if (c == '+') {
            negative = false;
        } else {
            return ~position;
        }
        return position;
    }
}