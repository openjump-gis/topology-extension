/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
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

package com.vividsolutions.jcs.qa;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import com.vividsolutions.jump.feature.Feature;

import java.util.*;

/**
 * A FeatureSegment is a line segment contained in an edge of a feature
 */
public class FeatureSegment extends LineSegment {

    private final Feature feature;
    private final int shell;
    private final int segment;
    
    Set<FeatureSegment> matches = null;
    
    // shell index and segment index are added for tracability
    public FeatureSegment(Feature feature, Coordinate p0, Coordinate p1, int shell, int segment) {
        super(p0, p1);
        this.feature = feature;
        this.shell = shell;
        this.segment = segment;
    }

    public Feature getFeature() {
        return feature;
    }
    
    public int getShellID() {
        return shell;
    }
    
    public int getSegmentID() {
        return segment;
    }
    
    public void addMatch(FeatureSegment match) {
        if (matches == null) matches = new HashSet<>();
        matches.add(match);
    }
    
    public Set<FeatureSegment> getMatches() { return matches; }
    
    public boolean equals(Object o) {
        if (o instanceof FeatureSegment) {
            FeatureSegment other = (FeatureSegment)o;
            // Start with the case where segment have opposite direction as
            // this is a more probable case in our use case 
            if (p0.equals(other.p1) && p1.equals(other.p0)) return true;
            else if (p0.equals(other.p0) && p1.equals(other.p1)) return true;
            else return false;
        }
        return false;
    }

    public static int hashCode(double d) {
        long f = Double.doubleToLongBits(d);
        return (int) (f ^ (f >>> 32));
    }
    
    /** 
     * This hashcode iscomputed so that two opposite segments have same hashcode.
     * This is consistant with equals method of FeatureSegment
     */
    public int hashCode() {
        int result = 17;
        result = 37 * result + hashCode(Math.min(p0.x, p1.x));
        result = 37 * result + hashCode(Math.max(p0.x, p1.x));
        result = 37 * result + hashCode(Math.min(p0.y, p1.y));
        result = 37 * result + hashCode(Math.max(p0.y, p1.y));
        return result;
    }
    
    public String toString() {
        return "FeatureSegment " + feature.getID() + "/" + shell + "/" + segment /*+ ":" + super.toString()*/;
    }
    
}
