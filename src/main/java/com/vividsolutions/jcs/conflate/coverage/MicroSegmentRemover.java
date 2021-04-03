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

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.*;

/**
 * Removes micro-segments from a geometry, carefully choosing the best point
 * to remove (the point with the most flat angle)
 * @author Michael Michaud
 */
public class MicroSegmentRemover {

    private final Geometry source;

    private double squareTolerance;
    private Geometry result;
    private Polygon fence;
    boolean geomChanged = false;
    List<Point> removedPoints;
    GeometryFactory gf;
    
   /**
    * Creates a MicroSegmentRemover for a Geometry.
    * @param geom geometry to clean
    */
    public MicroSegmentRemover(Geometry geom) {
        this.source = geom;
        this.gf = geom.getFactory();
    }
    
    /**
    * Creates a MicroSegmentRemover for a Geometry.
    * @param geom geometry to clean
    * @param fence limit the area where the clean can be done
    */
    public MicroSegmentRemover(Geometry geom, Geometry fence) {
        this.source = geom;
        assert (fence == null || fence instanceof Polygon) : "fence has to be a Polygon";
        this.fence = (Polygon)fence;
        this.gf = geom.getFactory();
    }
    
   /**
    * Removes micro segments from the geometry
    * Remove the point of the segment whith the most flat angle.
    * @param tolerance length tolerance
    */
    public void removeMicroSegments(double tolerance) {
        this.squareTolerance = tolerance * tolerance;
        removedPoints = new ArrayList<>();
        if (source.getDimension() < 1) return;
        List<Geometry> coll = new ArrayList<>();
        for (int i = 0 ; i < source.getNumGeometries() ; i++) {
            coll.add(removeMicroSegmentsFromGeometry(source.getGeometryN(i)));
        }
        result = gf.buildGeometry(coll);
    }
    
    public boolean hasGeometryChanged() {return geomChanged;}
    
    public Geometry getResultingGeometry() {return result;}
    
    public List<Point> getRemovedPoints() {return removedPoints;}
    
    private Geometry removeMicroSegmentsFromGeometry(Geometry geom) {
        if (geom instanceof Lineal) {
            return removeMicroSegmentsFromLineString((LineString)geom);
        }
        else if (geom instanceof Polygonal) {
            Polygon poly = (Polygon)geom;
            LinearRing exteriorRing = removeMicroSegmentsFromLinearRing(poly.getExteriorRing());
            LinearRing[] holes = new LinearRing[poly.getNumInteriorRing()];
            for (int i = 0 ; i < poly.getNumInteriorRing() ; i++) {
                holes[i] = removeMicroSegmentsFromLinearRing(poly.getInteriorRingN(i));
            }
            return gf.createPolygon(exteriorRing, holes);
        }
        else return geom;
    }
    
    private LineString removeMicroSegmentsFromLineString(LineString lineString) {
        Coordinate[] cc = lineString.getCoordinates();
        CoordinateList cl = new CoordinateList(cc, false);
        if (cl.size() != cc.length) geomChanged = true;
        for (int i = 0 ; i < cl.size()-1 ; i++) {
            int size = cl.size();
            if (isMicro(cl.getCoordinate(i), cl.getCoordinate(i+1)) && size>2) {
                if (fence != null &&
                    (!fence.contains(gf.createPoint(cl.getCoordinate(i))) ||
                     !fence.contains(gf.createPoint(cl.getCoordinate(i+1))))) continue;
                if (i==0) {
                    cl.remove(1);
                }
                else if (i==size-2) {
                    cl.remove(i);
                }
                else if (i>0 && i<size-2){
                    double sin2A = sin2AOB(cl.getCoordinate(i-1), cl.getCoordinate(i), cl.getCoordinate(i+1));
                    double sin2B = sin2AOB(cl.getCoordinate(i), cl.getCoordinate(i+1), cl.getCoordinate(i+2));
                    System.out.println("A:" + cl.getCoordinate(i) + Math.asin(Math.sqrt(sin2A))*180/Math.PI);
                    System.out.println("B:" + cl.getCoordinate(i+1) + Math.asin(Math.sqrt(sin2B))*180/Math.PI);
                    if (sin2A<sin2B) {
                        removedPoints.add(gf.createPoint(cl.getCoordinate(i)));
                        System.out.println("sinA<sinB" +
                            cl.getCoordinate(i) + "(" + sin2A + ")" +
                            " - " +
                            cl.getCoordinate((i+1)) + "(" + sin2B + ")");
                        cl.remove(i);
                    }
                    else {
                        removedPoints.add(gf.createPoint(cl.getCoordinate(i+1)));
                        System.out.println("sinB<sinA" +
                            cl.getCoordinate(i) + "(" + sin2A + ")" +
                            " - " +
                            cl.getCoordinate((i+1)) + "(" + sin2B + ")");
                        cl.remove(i+1);
                    }
                }
                geomChanged = true;
                i--;
            }
        }
        return geomChanged?gf.createLineString(cl.toCoordinateArray()):lineString;
    }
    
    private LinearRing removeMicroSegmentsFromLinearRing(LineString lineString) {
        assert lineString instanceof LinearRing : "Input must be a LinearRing";
        Coordinate[] cc = lineString.getCoordinates();
        CoordinateList cl = new CoordinateList(cc, false);
        if (cl.size() != cc.length) geomChanged = true;
        cl.remove(cl.size()-1);
        for (int i = 0 ; i < cl.size() ; i++) {
            int size = cl.size();
            if (isMicro(cl.getCoordinate(i), cl.getCoordinate((i+1)%size)) && size>4) {
                if (fence != null &&
                    (!fence.contains(gf.createPoint(cl.getCoordinate(i))) ||
                     !fence.contains(gf.createPoint(cl.getCoordinate((i+1)%size))))) continue;
                double sin2A = sin2AOB(cl.getCoordinate((size+i-1)%size), cl.getCoordinate(i), cl.getCoordinate((i+1)%size));
                double sin2B = sin2AOB(cl.getCoordinate(i), cl.getCoordinate((i+1)%size), cl.getCoordinate((i+2)%size));
                if (sin2A<sin2B) {
                    removedPoints.add(gf.createPoint(cl.getCoordinate(i)));
                    cl.remove(i);
                }
                else {
                    removedPoints.add(gf.createPoint(cl.getCoordinate((i+1)%size)));
                    cl.remove((i+1)%size);
                }
                geomChanged = true;
                i--;
            }
        }
        cl.closeRing();
        return geomChanged?gf.createLinearRing(cl.toCoordinateArray()):(LinearRing)lineString;
    }
    
    // Compare square length to check if tis is a micro segment
    // Comparing squares avoid a square root calculation which is longer
    private boolean isMicro(Coordinate c0, Coordinate c1) {
        double dx = c1.x - c0.x;
        double dy = c1.y - c0.y;
        return dx*dx + dy*dy < squareTolerance;
    }
    
    // Returns the square sinus of angle A-O-B
    // This method is used to remove the point of the micro-segment with the
    // most flat angle
    private double sin2AOB(Coordinate A, Coordinate O, Coordinate B) {
        double dxa = A.x - O.x;
        double dya = A.y - O.y;
        double dxb = B.x - O.x;
        double dyb = B.y - O.y;
        return (dxa*dyb-dya*dxb)*(dxa*dyb-dya*dxb)/(dxa*dxa+dya*dya)/(dxb*dxb+dyb*dyb);
    }
    
    private double cos2OAOB(Coordinate A, Coordinate O, Coordinate B) {
        double oax = A.x - O.x;
        double oay = A.y - O.y;
        double obx = B.x - O.x;
        double oby = B.y - O.y;
        return (oax*obx + oay*oby) * (oax*obx + oay*oby) / (oax*oax+oay*oay)/(obx*obx+oby*oby);
    }
    
}
