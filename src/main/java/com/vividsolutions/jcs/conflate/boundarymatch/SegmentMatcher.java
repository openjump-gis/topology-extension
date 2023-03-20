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

    public static boolean isCloseTo(Coordinate coord, LineSegment seg, double tolerance) {
        return coord.distance(seg.getCoordinate(0)) < tolerance ||
            coord.distance(seg.getCoordinate(1)) < tolerance;
    }


    //public static final double ANGLE_TOLERANCE = Math.PI / 8;   // 22.5 degrees
    final static double TWO_PI = 2.0 * Math.PI;
    final static double HALF_PI = Math.PI / 2.0;
    final static double QRTR_PI = Math.PI * 0.25;
    //final static double THREE_QRTR_PI = Math.PI * 0.75;
    private static final double A = 0.0776509570923569;
    private static final double B = -0.287434475393028;
    private static final double C = (QRTR_PI - A - B);



    /**
     * Computes an equivalent angle in the range 0 <= ang < 2*PI
     * This method is now recursive.
     *
     * @param angle the angle to be normalized
     * @return the normalized equivalent angle
     */
    public static double normalizedAngle(double angle) {
        if (angle < 0.0) return normalizedAngle(angle + TWO_PI);
        else if (angle >= TWO_PI) return normalizedAngle(angle - TWO_PI);
        else return angle;
    }

    /**
     * Computes the minimum angle between two line segments.
     * The minimum angle is a positive angle between 0 and PI (this is the
     * cw angle between 0 and 1 if cw angle is < PI and the ccw angle if not).
     * (LineSegment.angle returns an angle in the range [-PI, PI]
     */
    public static double angleDiff(LineSegment seg0, LineSegment seg1) {
        double a0 = normalizedAngle(segmentAngleFast(seg0));
        double a1 = normalizedAngle(segmentAngleFast(seg1));
        return Math.min(normalizedAngle(a0-a1), normalizedAngle(a1-a0));
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
        LineSegment projSeg1 = seg2.project(seg1);
        LineSegment projSeg2 = seg1.project(seg2);
        if (projSeg1 == null || projSeg2 == null) {
        	return false;
        }
        
        double hDiff = hausdorffDistance(projSeg1, projSeg2);
        if (hDiff > distanceTolerance) {
        	return false;
        }
        double dAngle = angleDiff(seg1, seg2);
        double dAngleInv = angleDiff(new LineSegment(seg1.p1, seg1.p0), seg2);
        switch (segmentOrientation) {
            case OPPOSITE_ORIENTATION:
                if (dAngleInv > angleToleranceRad) {
                    return false;
                }
                break;
            case SAME_ORIENTATION:
                if (dAngle > angleToleranceRad) {
                    return false;
                }
                break;
            case EITHER_ORIENTATION:
                if (dAngle > angleToleranceRad && dAngleInv > angleToleranceRad) {
                    return false;
                }
                break;
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
    
	  private static double segmentAngleFast(LineSegment l) {
		    return fastAtan2(l.p1.y - l.p0.y, l.p1.x - l.p0.x);
	  }

    /**
     * Maximum absolute error of ~0.00085 rad (~0.049º).
     */
    private static double fastAccurateAtan(final double x) {
        final double xx = x * x;
        return ((A * xx + B) * xx + C) * x;
    }

    /**
     * Maximum absolute error of ~0.00085 rad (~0.049º).
     * Computation time is about 15% of that of java Math.atan2.
     *
     * Note : unlike java Math.atan2 which returns 0.0,
     * fastAtan2(0.0, 0.0) returns NaN
     *
     * @return Angle from x axis positive side to (x,y) position, in radians, in
     *         [-PI,PI].
     */
    public static double fastAtan2(final double y, final double x) {
        // Adapted from https://www.dsprelated.com/showarticle/1052.php
        final double ay = Math.abs(y), ax = Math.abs(x);
        final boolean invert = ay > ax;
        final double z = invert ? ax / ay : ay / ax; // [0,1]
        double th = fastAccurateAtan(z); // [0,π/4]
        if (invert) {
            th = HALF_PI - th; // [0,π/2]
        }
        if (x < 0) {
            th = Math.PI - th; // [0,π]
        }
        return Math.copySign(th, y); // [-π,π]
    }

	private static double hausdorffDistance(final LineSegment s1, final LineSegment s2) {
	    double maxDist1 = 0.0;
	    double maxDist2 = 0.0;
	    maxDist1 = Math.max(s1.distance(s2.p0), maxDist1);
	    maxDist1 = Math.max(s1.distance(s2.p1), maxDist1);
	    maxDist2 = Math.max(s2.distance(s1.p0), maxDist2);
	    maxDist2 = Math.max(s2.distance(s1.p1), maxDist2);
	    return Math.max(maxDist1, maxDist2);
	}

}
