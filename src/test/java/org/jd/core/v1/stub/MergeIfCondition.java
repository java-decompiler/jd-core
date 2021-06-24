package org.jd.core.v1.stub;

import java.awt.geom.Point2D;

public class MergeIfCondition {

    @SuppressWarnings("static-access")
    double distance(Point2D p1, Point2D p2) {
        if (p1.distance(p1.getX(), p1.getY(), p2.getX(), p2.getY()) == 0
                && p2.distance(p2.getX(), p2.getY(), p1.getX(), p1.getY()) == 0)
            return 0;
        return p1.distance(p2);
    }
}
