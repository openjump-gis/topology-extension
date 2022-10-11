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

package com.vividsolutions.jcs.conflate.coverage;

import com.vividsolutions.jcs.conflate.boundarymatch.SegmentMatcher;
import com.vividsolutions.jcs.qa.FeatureSegment;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.Debug;

import java.util.Set;

/**
 * Models the shell or a hole of a polygon which can be matched to other shells
 * and adjusted to contain new vertices.
 */
public class Shell extends GeometryComponent {

    // A reference to the coverage MapVertex which maps coordinates with
    // adjustable Vertices (a Vertex hold information about the source coordinate,
    // the adjusted coordinate and the shells involved in this adjustment)
    private final VertexMap vertexMap;
    // Original ring
    private LinearRing ring;
    // Coordinates localized on a adjustableCoords are Vertices
    private boolean[] isVertex;
    // Non null segments of this shell
    private Segment[] segments;
    //private Coordinate[] initCoord;
    private Coordinate[] uniqueCoord;
    // adjusted coordinates are computed in computeAdjusted()
    private Coordinate[] adjustedCoord;
    // feature and index are just added for tracing purpose
    private final int featureID;
    // index of this shell in the feature (exterior shell is 0, holes are 1, 2, 3...)
    private final int shellIndex;
    // this marker is set to true if this Shell Segments have been marked
    // with marked with isInIndex marker (to know if they are in SegmentIndex). 
    private boolean isInSegmentIndexInitialized = false;

    public Shell(VertexMap vertexMap, int featureID, int shellIndex) {
        this.vertexMap = vertexMap;
        this.shellIndex = shellIndex;
        this.featureID = featureID;
    }
    
    public int getFeatureID() {
        return featureID;
    }
    
    public int getShellIndex() {
        return shellIndex;
    }

    public VertexMap getVertexMap() {
        return vertexMap;
    }

    public LinearRing getRing() {
        return ring;
    }

    public void initialize(LinearRing ring, Set<Coordinate> adjustableCoords) {
        this.ring = ring;
        uniqueCoord = CoordinateArrays.removeRepeatedPoints(ring.getCoordinates());
        isVertex = new boolean[uniqueCoord.length];
        segments = new Segment[uniqueCoord.length-1];
        for (int i = 0 ; i < uniqueCoord.length ; i++) {
            createVertex(i, adjustableCoords);
        }
        for (int i = 0 ; i < uniqueCoord.length-1 ; i++) {
            if (isVertex[i] || isVertex[i+1]) segments[i] = createSegment(i);
        }
    }
    
    //public boolean isIsInIndexInitialized() {
    //    return isInSegmentIndexInitialized;
    //}
    
    public void inSegmentIndexInitialization(SegmentIndex index) {
        if (!isInSegmentIndexInitialized) {
            for (int i = 0; i < segments.length; i++) {
                if (segments[i]!=null) segments[i].setIsInIndex(isInIndex(index, i));
            }
        }
        isInSegmentIndexInitialized = true;
    }

   /**
    * Creates a Vertex for vertex i <b>if</b>
    * this is a vertex that might be modified.
    *
    * @param i index
    * @param adjustableCoords Set of adjustable coordinates
    */
    private void createVertex(int i, Set<Coordinate> adjustableCoords) {
        Coordinate pt = uniqueCoord[i];
        if (adjustableCoords != null && !adjustableCoords.contains(pt)) return;
        Vertex v0 = vertexMap.get(pt);
        v0.addShell(this);
        isVertex[i] = true;
    }

    public Segment getSegment(int i) {
        if (segments[i] == null) segments[i] = createSegment(i);
        return segments[i];
    }

    private Segment createSegment(int i) {
        Vertex v0 = vertexMap.get(uniqueCoord[i]);
        Vertex v1 = vertexMap.get(uniqueCoord[i+1]);
        return new Segment(v0, v1, this, uniqueCoord[i].z, uniqueCoord[i+1].z);
    }

    // [2013-01-26] segmentIndex contains segments with original coordinates
    // 
    private boolean isInIndex(SegmentIndex segmentIndex, int i) {
        return segmentIndex.contains(new FeatureSegment(null, uniqueCoord[i], uniqueCoord[i + 1], -1, -1));
    }

    public boolean match(Shell shell, SegmentMatcher segmentMatcher,
                                      SegmentIndex matchedSegmentIndex) {
        if (segmentMatcher == null) {
//            Debug.println("Segment matcher should not be null !");
            return false;
        }
        else if (matchedSegmentIndex == null) {
//            Debug.println("matchedSegmentIndex should not be null !");
            return false;
        }
        // this method might cause the coordinates to change, so make sure they are recomputed
        adjustedCoord = null;
        boolean isAdjusted = false;


        // Only matched segments are considered for adjustment
        // (Non-matched ones either are already paired,
        // or have no match candidates)

        // this is O(n^2), which can be a problem for large polygons
        // (MD - although much better now with the short-circuiting of unmatched segs)
        this.inSegmentIndexInitialization(matchedSegmentIndex);
        shell.inSegmentIndexInitialization(matchedSegmentIndex);
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == null) continue;
            if (!segments[i].isInIndex()) continue;
            Envelope env = new Envelope(segments[i].getLineSegment().p0.x, segments[i].getLineSegment().p1.x, segments[i].getLineSegment().p0.y, segments[i].getLineSegment().p1.y);
            env.expandBy(2*segmentMatcher.getDistanceTolerance());
            for (int j = 0; j < shell.segments.length; j++) {
                if (shell.segments[j] == null) continue;
                if (!shell.segments[j].isInIndex()) continue;

                Segment seg0 = getSegment(i);
                Segment seg1 = shell.getSegment(j);

                // Inefficient - we already know which segments match
                // Also, could this be done symmetrically?
                // eg the segment added to both segments at the same time?
                LineSegment lineSeg0 = seg0.getLineSegment();
                LineSegment lineSeg1 = seg1.getLineSegment();
                // heuristic to speed up match checking
                //if (lineSeg0.distance(lineSeg1) > 2.0 * segmentMatcher.getDistanceTolerance()) {
                //    Debug.println("out of tolerance");
                //    continue;
                //}
                
                boolean isMatch = segmentMatcher.isMatch(lineSeg0, lineSeg1);
                boolean isTopoEqual = lineSeg0.equalsTopo(lineSeg1);
                if (isMatch && !isTopoEqual) {
                    //Debug.println("MATCH !");
                    isAdjusted |= seg0.addMatchedSegment(seg1, segmentMatcher.getDistanceTolerance());
                } 
                else {
                    //Debug.println("NOT MATCH");
                }
            }
            
        }
        //System.out.println("         isadjusted:"+isAdjusted);
        return isAdjusted;
    }

    public boolean isAdjusted(double distanceTolerance, boolean interpolate_z, double scale) {
        computeAdjusted(distanceTolerance, interpolate_z, scale);
        boolean isAdjusted = ! CoordinateArrays.equals(uniqueCoord, adjustedCoord);
        return isAdjusted;
    }

    public Coordinate[] getAdjusted(double distanceTolerance, boolean interpolate_z, double scale) {
        computeAdjusted(distanceTolerance, interpolate_z, scale);
        return adjustedCoord;
    }

    private void computeAdjusted(double distanceTolerance, boolean interpolate_z, double scale) {
        // already computed
        if (adjustedCoord != null) return;
        CoordinateList coordList = new CoordinateList();
//        Debug.println("      Adjust coordinates for " + featureID + "/" + shellIndex);
        // For each segment of this Shell
        for (int i = 0; i < segments.length; i++) {
            Coordinate pt = getAdjustedCoordinate(i);
            if (interpolate_z && segments[i] != null) {
                pt = (Coordinate)pt.clone();
                pt.z = segments[i].getZIni();
            }
            // add first coordinate
            coordList.add(pt, false);
//            if (!pt.equals(uniqueCoord[i])) {
//                Debug.println("        " + i + " : move " + uniqueCoord[i] + " -> " + pt);
//            }
            if (segments[i] != null) {
                // add inserted coordinates
                coordList.addAll(segments[i].getInsertedCoordinates(interpolate_z, scale), false);
//                Debug.println("        " + i + " : insert " + segments[i].getInsertedCoordinates(interpolate_z, scale));
            } 
        }
        coordList.closeRing();
        // open the ring so that following cleaning operations can use the
        // modulo arithmetic
        coordList.remove(coordList.size()-1);
        CoordinateList noRepeatCoordList = removeRepeatedSegments(coordList);
        noRepeatCoordList = removeMicroLoops(noRepeatCoordList, distanceTolerance);
        noRepeatCoordList.closeRing();
        adjustedCoord = noRepeatCoordList.toCoordinateArray();
    }

    // Get the new adjusted coordinate for point i 
    private Coordinate getAdjustedCoordinate(int i) {
        if (segments[i] != null) {
            Coordinate c = segments[i].getVertex(0).getCoordinate();
            //[mmichaud 2013-01-26] improvement : if the new position of the
            // vertex has itself been adjusted, return the new new position
            // TODO : may it enter an infinite loop ? 
            if (!vertexMap.contains(c)) return c;
            Vertex v = vertexMap.get(c);
            return v.getCoordinate();
        }
        Coordinate pt = uniqueCoord[i];
        if (!vertexMap.contains(pt)) {
            return pt;
        }
        Vertex v = vertexMap.get(pt);
        return v.getCoordinate();
    }

   /**
    * Remove any repeated segments
    * (e.g. a pattern of Coordinates of the form "a-b-a" is converted to "a" )
    *
    * @param coordList list of coordinates to clean
    */
    private CoordinateList removeRepeatedSegments(CoordinateList coordList) {
        int size = coordList.size();
        for (int i = 0; i < size && size > 3; i++) {
            Coordinate a = coordList.getCoordinate(i%size);
            // check for a-b-a pattern
            Coordinate b = coordList.getCoordinate((i+1)%size);
            Coordinate c = coordList.getCoordinate((i+2)%size);
            if (a.equals(c)) {
                coordList.remove(Math.max((i+2)%size,(i+1)%size));
                coordList.remove(Math.min((i+2)%size,(i+1)%size));
                size = size-2;
            }
        }
        return coordList;
    }
    
    /**
    * Remove micro-loops
    * (e.g. a pattern of Coordinates of the form "a-b-c-a" with a-b, b-c and c-a
    * smaller than tolerance is converted to "a" )
    *
    * @param coordList list of coordinates to clean
    */
    private CoordinateList removeMicroLoops(CoordinateList coordList, double distanceTolerance) {
        int size = coordList.size();
        for (int i = 0; i < size && size > 4; i++) {
            Coordinate a = coordList.getCoordinate(i%(size));
            // check for a-b-c-a pattern
            Coordinate b = coordList.getCoordinate((i+1)%(size));
            Coordinate c = coordList.getCoordinate((i+2)%(size));
            Coordinate d = coordList.getCoordinate((i+3)%(size));
            if (a.equals(d) && a.distance(b)<distanceTolerance &&
                               b.distance(c)<distanceTolerance &&
                               c.distance(d)<distanceTolerance) {
                int[] indices = new int[]{(i+1)%size, (i+2)%size, (i+3)%size};
                java.util.Arrays.sort(indices);
                for (int index = 2 ; index >=0 ; index--) {
                    coordList.remove(index);
                    size--;
                }
            }
        }
        return coordList;
    }

    public boolean isConflict() {
        for (Segment segment : segments) {
            if (segment == null) continue;
            if (segment.isConflict()) return true;
            if (segment.getVertex(0).isConflict()) return true;
            if (segment.getVertex(1).isConflict()) return true;
        }
        return false;
    }
    
    public boolean equals(Object o) {
        if (o instanceof Shell) {
            Shell other = (Shell)o;
            return featureID == other.getFeatureID() && shellIndex == other.getShellIndex();
        } 
        else return false;
    }
    
    public int hashCode() {
        return featureID << 15 + shellIndex;
    }
}
