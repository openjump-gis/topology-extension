

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

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.geom.LineSegmentEnvelopeIntersector;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CoordinateArrays;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.Debug;

import java.util.*;

/**
 * Keeps a count of distinct LineSegments (as a Map from LineSegments to counts).
 * It can be used to retrieve a list of unique segments (up to point order).
 * LineSegments are normalized before being counted
 * (so the segment comparison is independent of point order).
 * <p>
 * Zero-length segments can be ignored if required.  This is useful
 * for handling geometries with repeated points.
 */
public class FeatureSegmentCounter {

    private static final I18N i18n = I18N.getInstance("fr.michaelm.jump.plugin.topology");
    private static final GeometryFactory factory = new GeometryFactory();
    private static final Integer ONE = 1;

    // Should use HashMap instead of TreeMap, but hashCode function
    // is not implemented in LineSegment [mmichaud 2010-01-19]
    // private Map<LineSegment,FeatureSegmentCount> segmentMap = new TreeMap<LineSegment,FeatureSegmentCount>();
    private final Map<FeatureSegment,Integer> segmentCounter = new HashMap<>();
    private final TaskMonitor monitor;

    private boolean countZeroLengthSegments = true;
    private Geometry fence = null;
    private LineSegmentEnvelopeIntersector lineEnvInt;

    /**
     * Creates a new counter, allowing control over
     * whether zero-length segments are counted.
     *
     * @param countZeroLengthSegments if <code>false</code>, zero-length segments will be ignored
     */
    public FeatureSegmentCounter(boolean countZeroLengthSegments, TaskMonitor monitor) {
        this.countZeroLengthSegments = countZeroLengthSegments;
        this.monitor = monitor;
    }

    public void setFence(Geometry fence) {
      this.fence = fence;
    }

    public void add(FeatureCollection fc) {
        monitor.allowCancellationRequests();
        monitor.report(i18n.get("qa.FeatureSegmentCounter.adding-features-to-counter"));
        int totalFeatures = fc.size();
        int j = 0;
        for (Iterator i = fc.iterator(); i.hasNext() && ! monitor.isCancelRequested(); ) {
            Feature feature = (Feature) i.next();
            j++;
            monitor.report(j, totalFeatures, i18n.get("features"));
            add(feature);
        }
    }

    public void add(Feature f) {
        Geometry g = f.getGeometry();
        // skip if using fence and feature is not in fence
        if (fence != null && !g.intersects(fence)) return;
        
        for (int i = 0 ; i < g.getNumGeometries() ; i++) {
            List<Coordinate[]> coordArrayList =
                CoordinateArrays.toCoordinateArrays(g.getGeometryN(i), true);
            int lineCount = 0;
            for (Coordinate[] coords : coordArrayList) {
                for (int j = 0; j < coords.length - 1; j++) {
                    // skip if using fence AND seg is not in fence
                    if (fence != null) {
                        LineString segLine = factory.createLineString(new Coordinate[] { coords[j], coords[j + 1] });
                        if (!fence.intersects(segLine)) continue;
                    }
                    FeatureSegment fs = new FeatureSegment(f, coords[j], coords[j + 1], lineCount, j);
                    add(fs);
                    Debug.println("      - add " + fs.getFeature().getID()+"/"+fs.getShellID()+"/"+fs.getSegmentID());
                }
                lineCount++;
            }
        }
    }

    public void add(FeatureSegment seg) {
        if (!countZeroLengthSegments && seg.p0.equals(seg.p1)) return;
        Integer count = segmentCounter.get(seg);
        if (count == null) {
            segmentCounter.put(seg, ONE);
        }
        else {
            segmentCounter.put(seg, count+1);
        }
    }


    /**
     *
     * @return a List of unique LineSegments or FeatureSegments
     */
    public List<FeatureSegment> getUniqueSegments() {
        List<FeatureSegment> unique = new ArrayList<>();
        for (Map.Entry<FeatureSegment,Integer> entry : segmentCounter.entrySet()) {
            if (entry.getValue().equals(ONE)) {
                unique.add(entry.getKey());
            }
        }
        return unique;
    }

    
    /**
     * Returns the number of segments topologically equals to this one
     */
    public int getCount(LineSegment seg) {
        return segmentCounter.get(seg);
    }

}
