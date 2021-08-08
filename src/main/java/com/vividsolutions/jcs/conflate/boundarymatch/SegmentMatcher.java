/*
 * The JCS Conflation Suite (JCS) is a library of Java classes that
 * can be used to build automated or semi-automated conflation solutions.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jcs.conflate.boundarymatch;

import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.geom.*;
import com.vividsolutions.jump.geom.*;

/**
 * A SegmentMatcher computes information about whether two boundary
 * LineSegments match
 */
public class SegmentMatcher {

    // the possible relative orientations of matched segments
    public static final int SAME_ORIENTATION = 1;
    public static final int OPPOSITE_ORIENTATION = 2;
    public static final int EITHER_ORIENTATION = 3;
    private static final GeometryFactory FACTORY = new GeometryFactory();

    public static boolean isCloseTo(Coordinate coord, LineSegment seg, double tolerance) {
        return coord.distance(seg.getCoordinate(0)) < tolerance ||
            coord.distance(seg.getCoordinate(1)) < tolerance;
    }


    //public static final double ANGLE_TOLERANCE = Math.PI / 8;   // 22.5 degrees
    public static final double PI2 = 2.0 * Math.PI;

    /**
     * Computes an equivalent angle in the range 0 <= ang < 2*PI
     * This method is now recursive.
     *
     * @param angle the angle to be normalized
     * @return the normalized equivalent angle
     */
    public static double normalizedAngle(double angle) {
        if (angle < 0.0) return normalizedAngle(angle + PI2);
        else if (angle >= PI2) return normalizedAngle(angle - PI2);
        else return angle;
    }

    /**
     * Computes the minimum angle between two line segments.
     * The minimum angle is a positive angle between 0 and PI (this is the
     * cw angle between 0 and 1 if cw angle is < PI and the ccw angle if not).
     * (LineSegment.angle returns an angle in the range [-PI, PI]
     */
    public static double angleDiff(LineSegment seg0, LineSegment seg1) {
        double a0 = normalizedAngle(seg0.angle());
        double a1 = normalizedAngle(seg1.angle());
        double delta = Math.min(normalizedAngle(a0-a1), normalizedAngle(a1-a0));
        return delta;
    }

    // temp storage for point args
    private final LineSegment line0 = new LineSegment();
    private final LineSegment line1 = new LineSegment();

    private final double distanceTolerance;
    private final double angleTolerance;
    private final double angleToleranceRad;
    private final int segmentOrientation;

    public SegmentMatcher(double distanceTolerance, double angleTolerance) {
        this(distanceTolerance, angleTolerance, OPPOSITE_ORIENTATION);
    }
    
    public SegmentMatcher(double distanceTolerance, double angleTolerance, int segmentOrientation) {
        this.distanceTolerance = distanceTolerance;
        this.angleTolerance = angleTolerance;
        angleToleranceRad = Angle.toRadians(angleTolerance);
        this.segmentOrientation = segmentOrientation;
    }

    public double getDistanceTolerance() { return distanceTolerance; }

    public boolean isMatch(Coordinate p00, Coordinate p01, Coordinate p10, Coordinate p11) {
        line0.p0 = p00;
        line0.p1 = p01;
        line1.p0 = p10;
        line1.p1 = p11;
        return isMatch(line0, line1);
    }

    /**
     * Computes whether two segments match.
     * This matching algorithm uses the following conditions to determine if
     * two line segments match:
     * <ul>
     * <li> The segments have similar slope.
     * I.e., the difference in slope between the two segments is less than
     * the angle tolerance (this test is made irrespective of orientation)
     * <li> The segments have a mutual overlap
     * (e.g. they both have a non-null projection on
     * the other)
     * <li> The Hausdorff distance between the mutual projections of the segments
     * is less than the distance tolerance.  This ensures that matched segments
     * are close along their entire length.
     * </ul>
     * <p>
     * This relation is symmetrical.
     *
     * @param seg1 first segment line
     * @param seg2 second segment line
     * @return <code>true</code> if the segments match
     */
    public boolean isMatch(LineSegment seg1, LineSegment seg2) {
        boolean isMatch = true;
        double dAngle = angleDiff(seg1, seg2);
        double dAngleInv = angleDiff(new LineSegment(seg1.p1, seg1.p0), seg2);
        switch (segmentOrientation) {
            case OPPOSITE_ORIENTATION:
                if (dAngleInv > angleToleranceRad) {
                    isMatch = false;
                    return isMatch;
                }
                break;
            case SAME_ORIENTATION:
                if (dAngle > angleToleranceRad) {
                    isMatch = false;
                    return isMatch;
                }
                break;
            case EITHER_ORIENTATION:
                if (dAngle > angleToleranceRad && dAngleInv > angleToleranceRad) {
                    isMatch = false;
                    return isMatch;
                }
                break;
        }

        LineSegment projSeg1 = seg2.project(seg1);
        LineSegment projSeg2 = seg1.project(seg2);
        if (projSeg1 == null || projSeg2 == null) {
            isMatch = false;
            return isMatch;
        }

        if (new DiscreteHausdorffDistance(projSeg1.toGeometry(FACTORY), projSeg2.toGeometry(FACTORY)).distance() > distanceTolerance) {
            isMatch = false;
        }
        return isMatch;
    }

    /**
     * Test whether there is an overlap between the segments in either direction.
     * A segment overlaps another if it projects onto the segment.
     */
    public boolean hasMutualOverlap(LineSegment src, LineSegment tgt) {
        if (projectsOnto(src, tgt)) return true;
        if (projectsOnto(tgt, src)) return true;
        return false;
    }

    public boolean projectsOnto(LineSegment seg1, LineSegment seg2) {
        double pos0 = seg2.projectionFactor(seg1.p0);
        double pos1 = seg2.projectionFactor(seg1.p1);
        if (pos0 >= 1.0 && pos1 >= 1.0) return false;
        if (pos0 <= 0.0 && pos1 <= 0.0) return false;
        return true;
    }

}
