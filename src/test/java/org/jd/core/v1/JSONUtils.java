package org.jd.core.v1;

public abstract class JSONUtils {
    
    private static final int[] ESC_CODES = {};

    public void quoteAsString1(final CharSequence input, final StringBuilder output) {
        final char[] qbuf = {};
        final int escCodeCount = ESC_CODES.length;
        int inPtr = 0;
        final int inputLen = input.length();

        outer:
        while (inPtr < inputLen) {
            tight_loop:
            while (true) {
                final char c = input.charAt(inPtr);
                if (c < escCodeCount && ESC_CODES[c] != 0) {
                    break tight_loop;
                }
                output.append(c);
                if (++inPtr >= inputLen) {
                    break outer;
                }
            }
            // something to escape; 2 or 6-char variant?
            final char d = input.charAt(inPtr++);
            final int escCode = ESC_CODES[d];
            final int length = (escCode < 0)
                    ? _appendNumeric(d, qbuf)
                    : _appendNamed(escCode, qbuf);

            output.append(qbuf, 0, length);
        }
    }

    public void quoteAsString2(final CharSequence input, final StringBuilder output) {
        final char[] qbuf = {};
        final int escCodeCount = ESC_CODES.length;
        int inPtr = 0;
        final int inputLen = input.length();

        while (inPtr < inputLen) {
            while (true) {
                final char c = input.charAt(inPtr);
                if (c < escCodeCount && ESC_CODES[c] != 0) {
                    break;
                }
                output.append(c);
                if (++inPtr >= inputLen) {
                    return;
                }
            }
            // something to escape; 2 or 6-char variant?
            final char d = input.charAt(inPtr++);
            final int escCode = ESC_CODES[d];
            final int length = (escCode < 0)
                    ? _appendNumeric(d, qbuf)
                    : _appendNamed(escCode, qbuf);

            output.append(qbuf, 0, length);
        }
    }

    public void test(int escCode, char d, char[] qbuf) {
        final int length = (escCode < 0)
                ? _appendNumeric(d, qbuf)
                : _appendNamed(escCode, qbuf);
        System.out.println(length);
    }
    
    protected abstract int _appendNamed(int escCode, char[] qbuf);
    protected abstract int _appendNumeric(char d, char[] qbuf);
}
