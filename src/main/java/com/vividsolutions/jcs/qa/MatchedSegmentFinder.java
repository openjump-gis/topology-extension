

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

import com.vividsolutions.jcs.conflate.boundarymatch.SegmentMatcher;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDatasetFactory;
import com.vividsolutions.jump.geom.LineSegmentUtil;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CoordinateArrays;
import fr.michaelm.jump.plugin.topology.I18NPlug;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Finds line segments in one or two
 * FeatureCollections which match but are not identical.
 * If a single FeatureCollection is being tested, it is assumed to be a coverage.
 * In this case, duplicate segments
 * are removed before matches are tested, since only segments which are not
 * paired are of interest.
 */
public class MatchedSegmentFinder {


    public static class Parameters {
        /**
         * The distance below which segments are considered to match
         */
        public double distanceTolerance = 1.0;
        
        /**
         * The maximum angle between matching segments.
         */
        public double angleTolerance = 22.5;
        /**
         * The allowable segment orientations to match
         */
        public int segmentOrientation = SegmentMatcher.OPPOSITE_ORIENTATION;
    }

    private static final GeometryFactory factory = new GeometryFactory();
    private static final String FEATURES = I18NPlug.getI18N("features");
    
    private final FeatureCollection[] inputFC = new FeatureCollection[2];
    private final FeatureCollection[] matchedFC = new FeatureCollection[2];
    private FeatureCollection sizeIndicatorFC = null;

    private final List[] matchedLines = { new ArrayList(), new ArrayList() };
    private Parameters param;
    private final TaskMonitor monitor;
    private boolean isComputed = false;
    private final SegmentMatcher segMatcher;

    private final LineSegment candidateSeg = new LineSegment();
    private final Envelope itemEnv = new Envelope();
    // a list of Geometry's
    private final List<Geometry> sizeIndicators = new ArrayList<>();
    
    //private Quadtree segIndex = new Quadtree();
    private final SpatialIndex segIndex = new STRtree();
    
    public MatchedSegmentFinder(
            FeatureCollection referenceFC,
            FeatureCollection subjectFC,
            Parameters param,
            TaskMonitor monitor) {
        inputFC[0] = referenceFC;
        inputFC[1] = subjectFC;
        this.param = param;
        segMatcher = new SegmentMatcher(param.distanceTolerance, param.angleTolerance, param.segmentOrientation);
        this.monitor = monitor;
    }
    
  /**
   *
   * @return a FeatureCollection of LineString Features
   */
    public FeatureCollection getMatchedSegments(int i) {
        computeMatches();
        return matchedFC[i];
    }
  
    public FeatureCollection getSizeIndicators() {
        computeMatches();
        return sizeIndicatorFC;
    }

    private void computeMatches() {
        if (isComputed) return;
        isComputed = true;

        monitor.report(I18NPlug.getI18N("qa.MatchedSegmentFinder.creating-segment-index") + "...");
        createIndex(inputFC[0]);
        monitor.report(I18NPlug.getI18N("qa.MatchedSegmentFinder.testing-segments") + "...");
        findMatches(inputFC[1]);
        matchedFC[0] = FeatureDatasetFactory.createFromGeometry(matchedLines[0]);
        matchedFC[1] = FeatureDatasetFactory.createFromGeometry(matchedLines[1]);
        sizeIndicatorFC = FeatureDatasetFactory.createFromGeometryWithLength(sizeIndicators, "LENGTH");
    }

    private void createIndex(FeatureCollection fc) {
        for (Feature f : fc.getFeatures()) {
            addSegmentsToIndex(f);
        }
    }

    private void addSegmentsToIndex(Feature f) {
        Geometry g = f.getGeometry();
        List<Coordinate[]> coordArrays = CoordinateArrays.toCoordinateArrays(g, true);
        int count = 0;
        for (Coordinate[] coords : coordArrays) {
            addSegmentsToIndex(f, coords, count++);
        }
    }

    private void addSegmentsToIndex(Feature f, Coordinate[] coord, int shellIndex) {
        for (int i = 0; i < coord.length - 1; i++) {
            FeatureSegment fs = new FeatureSegment(f, coord[i], coord[i + 1], shellIndex, i);
            Envelope itemEnv = new Envelope(coord[i], coord[i + 1]);
            segIndex.insert(itemEnv, fs);
        }
    }

    private void findMatches(FeatureCollection fc) {
        int featuresProcessed = 0;
        int totalFeatures = fc.size();

        for (Feature f : fc.getFeatures()) {
            if (monitor.isCancelRequested()) return;
            featuresProcessed++;
            monitor.report(featuresProcessed, totalFeatures, FEATURES);
            findMatches(f);
        }
    }
    
    private void findMatches(Feature f) {
        Geometry g = f.getGeometry();
        List<Coordinate[]> coordArrays = CoordinateArrays.toCoordinateArrays(g, true);
        int count = 0;
        for (Coordinate[] coords : coordArrays) {
            findMatches(f, coords, count++);
        }
    }
    
    private void findMatches(Feature f, Coordinate[] coord, int shellIndex) {
        for (int i = 0; i < coord.length - 1; i++) {
            FeatureSegment querySeg = new FeatureSegment(f, coord[i], coord[i + 1], shellIndex, i);
            itemEnv.init(coord[i], coord[i + 1]);
            List<FeatureSegment> candidateSegments = segIndex.query(itemEnv);
            boolean hasMatch = checkMatches(f, querySeg, candidateSegments);
            if (hasMatch) {
                // save the matched segment in matchedFC[1]
                matchedLines[1].add(LineSegmentUtil.asGeometry(factory, querySeg));
            }
        }
    }

    
    private boolean checkMatches(Feature f, FeatureSegment querySeg,
                                 List<FeatureSegment> candidateSegments) {
        boolean hasMatch = false;
        for (FeatureSegment candidateFS : candidateSegments) {
            // if segments are from same feature, they don't match
            if (candidateFS.getFeature() == f) continue;
            candidateSeg.p0 = candidateFS.p0;
            candidateSeg.p1 = candidateFS.p1;
            boolean isMatch = segMatcher.isMatch(querySeg, candidateSeg);
            boolean isEqual = querySeg.equalsTopo(candidateSeg);
            if (isMatch && ! isEqual) {
              // save the matched segment in matchedFC[0]
              matchedLines[0].add(LineSegmentUtil.asGeometry(factory, candidateSeg));
              hasMatch = true;
              // check for relative size of IDs to avoid creating duplicate indicators
              // if (querySeg.getFeature().getID() > candidateFS.getFeature().getID()) {
              List<Geometry> indicators =
                  InternalMatchedSegmentFinder.createIndicatorList(querySeg, candidateFS);
              sizeIndicators.addAll(indicators);
              // }
            }
        }
        return hasMatch;
    }

}
