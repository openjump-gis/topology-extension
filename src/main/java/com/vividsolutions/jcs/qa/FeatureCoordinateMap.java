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

package com.vividsolutions.jcs.qa;

import java.util.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.util.CoordinateArrays;
import com.vividsolutions.jump.task.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Keeps a count of
 * distinct Coordinates (as a Map from LineSegments to counts).
 * It can be used to retrieve a list of unique coordinates.
 * LineSegments are normalized before being counted
 * (so the segment comparison is independent of point order).
 * <p>
 * Zero-length segments can be ignored if required.  This is useful
 * for handling geometries with repeated points.
 */
public class FeatureCoordinateMap {

    public static Set<Feature> getFeaturesWithVertices(TaskMonitor monitor,
                                              FeatureCollection fc,
                                              Collection<Coordinate> coords) {
        FeatureCoordinateMap map = new FeatureCoordinateMap(monitor);
        map.add(fc);
        Set<Feature> featuresWithVertices = new HashSet<>();
        for (Coordinate coord : coords) {
            featuresWithVertices.addAll(map.getFeatures(coord));
        }
        return featuresWithVertices;
    }

    //private Map coordMap = new TreeMap();
    private final Map<Coordinate,List<Feature>> coordMap = new HashMap<Coordinate,List<Feature>>();
    private final TaskMonitor monitor;
    private Envelope fence = null;

    /**
     * Creates a new counter, allowing control over
     * whether zero-length segments are counted.
     */
    public FeatureCoordinateMap(TaskMonitor monitor) {
        this.monitor = monitor;
    }

    public void setFence(Envelope fence) {
        this.fence = fence;
    }

    public void add(FeatureCollection fc) {
        monitor.allowCancellationRequests();
        monitor.report("Adding features to Feature-Coordinate map");
        int totalFeatures = fc.size();
        int j = 0;
        for (Iterator i = fc.iterator(); i.hasNext() && ! monitor.isCancelRequested(); ) {
            Feature feature = (Feature) i.next();
            j++;
            monitor.report(j, totalFeatures, "features");
            add(feature);
        }
    }
    public void add(Feature f) {
        Geometry g = f.getGeometry();
        // skip if using fence and feature is not in fence
        if (fence != null && ! g.getEnvelopeInternal().intersects(fence)) return;
        List<Coordinate[]> coordArrays = CoordinateArrays.toCoordinateArrays(g, true);
        for (Coordinate[] coords : coordArrays) {
            for (int j = 0; j < coords.length - 1; j++) {
                add(coords[j], f);
            }
        }
    }
    
    public void add(Coordinate pt, Feature f) {
        List<Feature> featureList = coordMap.computeIfAbsent(pt, k -> new ArrayList<>());
        featureList.add(f);
    }

    public List<Feature> getFeatures(Coordinate p) {
        return coordMap.get(p);
    }

}
