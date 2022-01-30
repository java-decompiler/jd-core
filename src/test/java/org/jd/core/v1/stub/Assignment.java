package org.jd.core.v1.stub;

import java.awt.Rectangle;

public class Assignment {

    void compute(Rectangle[] r) {
        r[7].y = (r[8].y = r[6].y = r[4].y + r[4].height);
    }
}
