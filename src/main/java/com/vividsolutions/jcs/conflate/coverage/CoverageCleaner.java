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


import com.vividsolutions.jcs.qa.*;
import com.vividsolutions.jcs.conflate.boundarymatch.*;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.*;

import fr.michaelm.jump.plugin.topology.I18NPlug;

import java.util.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.Debug;


/**
 * Removes gaps and overlaps from FeatureDatasets that are intended to have
 * coverage topology.
 */
public class CoverageCleaner {
  
    public static boolean hasMultiPolygonFeature(List<Feature> featureList) {
        for (Feature feature : featureList) {
            if (feature.getGeometry() instanceof MultiPolygon) {
                return true;
            }
        }
        return false;
    }

    public static class Parameters {
        public Parameters(){}
        public Parameters(double distanceTolerance, double angleTolerance) {
            this.distanceTolerance = distanceTolerance;
            this.angleTolerance = angleTolerance;
        }
       /**
        * The distance tolerance below which segments and vertices are considered to match
        */
        public double distanceTolerance = 1.0;
       /**
        * The maximum angle between matching segments.
        */
        public double angleTolerance = 22.5;
        /**
         * If true, coordinates inserted in a segment from another feature will
         * have a z interpolated on the segment ends rather than the z of the
         * other feature
         */
        public boolean interpolateZ = false;
        /**
         * Scale factor to reduce precision of the z if interpolate_z is true
         */
        public double zScale = 1.0;
    }

    //private static GeometryFactory geomFactory = new GeometryFactory();

    //input data
    private Parameters param;
    private final FeatureCollection inputFC;

    // working data
    private final Coverage cvg;
    private FeatureCollection matchedFC;
    private List<FeatureSegment> matchedSegments = null;
    private SegmentIndex matchedSegmentIndex = null;
    private Set<Coordinate> matchedSegmentCoordSet = null;

    /**
     * The features which may be adjusted (due to having matched segments
     * or vertices touching matched segments)
     */
    //private FeatureCollection candidateFeatures = null;

    private Geometry fenceGeometry = null;

    private final TaskMonitor monitor;

    public CoverageCleaner(FeatureCollection inputFC, TaskMonitor monitor) {
        this.inputFC = inputFC;
        cvg = new Coverage(inputFC);
        this.monitor = monitor;
    }

    /**
     * Sets a fence to use for processing.
     * If set, only segments within fence will be adjusted.
     *
     * @param fenceGeometry the fence geometry to use, if any
     */
    public void setFence(Geometry fenceGeometry) {
      this.fenceGeometry = fenceGeometry;
    }

    public void process(Parameters param) {
        this.param = param;
        monitor.report(I18NPlug.getI18N("qa.CoverageCleaner.matching-segments"));

        if (monitor.isCancelRequested()) return;
        // Find matching FeatureSegments (intersecting fence if fence is not null)
        // use InternalMatchedSegmentFinder to
        // - retain unique segments (remove segments topologically equals to each other)
        // - create a spatial index with unique segments
        // - put matching unique segments in a list (matchedSegments)
        // find Features containing matching segments
        Debug.println("1 - Get matched segments and matched features");
        matchedFC = getMatched();
        
        // creates a SegmentIndex (matchedSegmentIndex) containing each single matching FeatureSegment
        // creates a Set containing all theit coordinates (matchedSegmentCoordSet)
        Debug.println("2 - Load matched segment index (and coord set)");
        loadMatchedSegmentIndex();
        
        // Associate the set of unique coordinates belonging to a matching segment
        // with the coverage object   
        Debug.println("3 - SetAdjustableCoordinates (put coord set into coverage)");
        cvg.setAdjustableCoordinates(matchedSegmentCoordSet);

        // Get all features with a point included in matchedSegmentCoordSet
        // reason : features with no matching segment but with a coordinate in 
        // matchedSegmentCoordSet may have to be adjusted
        Debug.println("4 - Get all Features involved (from coord set)");
        Collection<Feature> adjustableFeatureList =
            FeatureCoordinateMap.getFeaturesWithVertices(monitor,
                                                         cvg.getFeatures(),
                                                         matchedSegmentCoordSet);
        
        // Creates a new dataset with adjustable features
        Debug.println("5 - Creates a dataset from adjustable features");
        FeatureCollection adjustableFeatures =
            new FeatureDataset(adjustableFeatureList, inputFC.getFeatureSchema());
            
        // Adjust features with matching segments
        // For each candidate feature 
        // - find near features
        // - adjust candidate feature with near features (see CoverageFeature#computeAdjustment)
        // Then update features (Coverage#computeAdjustedFeatureUpdates)
        Debug.println("6 - Adjust adjustable features (having a coord in coord set) and candidate features (having a matching segment)");
        adjustNearFeatures(matchedFC, adjustableFeatures);
    }

    private FeatureCollection getMatched() {
        InternalMatchedSegmentFinder.Parameters msfParam
            = new InternalMatchedSegmentFinder.Parameters();
        msfParam.distanceTolerance = param.distanceTolerance;
        msfParam.angleTolerance = param.angleTolerance;
        InternalMatchedSegmentFinder msf = new InternalMatchedSegmentFinder(cvg.getFeatures(), msfParam, monitor);
        msf.setFence(fenceGeometry);
        FeatureCollection fc = msf.getMatchedFeatures();
        matchedSegments = msf.getMatchedFeatureSegments();
        return fc;
    }

    private void loadMatchedSegmentIndex() {
        // mmichaud HashSet instead of TreeSet
        matchedSegmentCoordSet = new HashSet<>();
        matchedSegmentIndex = new SegmentIndex();
        for (FeatureSegment segment : matchedSegments) {
            Debug.println("  - load " + segment);
            // check if this potential gap should be a candidate for fixing
            if (!isInFence(segment)) continue;
            matchedSegmentIndex.add(segment);
            matchedSegmentCoordSet.add(segment.p0);
            matchedSegmentCoordSet.add(segment.p1);
        }
    }

    private boolean isInFence(LineSegment seg) {
        if (fenceGeometry == null) return true;
        Geometry line = fenceGeometry.getFactory().createLineString(
            new Coordinate[] { seg.p0, seg.p1 });
        return fenceGeometry.contains(line);
    }

    public FeatureCollection getMatchedFeatures() {
        return matchedFC;
    }

    public FeatureCollection getAdjustedFeatures() {
        return cvg.getAdjustedFeatures();
    }

    public FeatureCollection getUpdatedFeatures() {
        return cvg.getUpdates().applyUpdates(cvg.getFeatures());
    }
 
    public FeatureCollection getAdjustmentIndicators() {
        return cvg.getAdjustmentIndicators();
    }

    /**
     * Process all features in the FeatureCollection, computing adjustments
     * for them to match their neighbour features.
     * To ensure each feature is processed once only, the relation "isNearTo"
     * is traversed in breadth-first order, and each feature is processed as it
     * is encountered.
     *
     * @param matchedFC the collection of Features containing gaps to be adjusted.
     * @param adjustableFC the Features which can be adjusted
     */
    private void adjustNearFeatures(FeatureCollection matchedFC, FeatureCollection adjustableFC) {
        
        monitor.report(I18NPlug.getI18N("qa.CoverageCleaner.adjusting-features"));
        
        SegmentMatcher segmentMatcher =
            new SegmentMatcher(param.distanceTolerance, param.angleTolerance);
        
        // Only index the features which have potential matches
        NearFeatureFinder nff = new NearFeatureFinder(adjustableFC);
        
        /**
         * MD - can we get away with only comparing matched features?
         * This would be faster, since fewer features are in the index.
         * (MD - actually doesn't appear to make much overall speed difference)
         * However, it may cause problems with coverage consistency
         * (non-matched features may still share vertices which are adjusted, and
         * thus must be adjusted themselves)
         */
        //NearFeatureFinder nff = new NearFeatureFinder(matchedFC);
        
        int featuresProcessed = 0;
        int totalFeatures = matchedFC.size();
        //long t0 = System.currentTimeMillis();
        Debug.println("  6.1 Iteration on matchedFC");
        for (Feature f : matchedFC.getFeatures()) {
            if (monitor.isCancelRequested()) return;
            featuresProcessed++;
            // currently only polygons are handled
            if (!(f.getGeometry() instanceof Polygon)) continue;
            monitor.report(featuresProcessed, totalFeatures, I18NPlug.getI18N("features"));
            
            List<Feature> nearFeatures = nff.findNearFeatures(f, param.distanceTolerance);
            // currently only polygons are handled
            if (hasMultiPolygonFeature(nearFeatures)) continue;
            
            CoverageFeature cgf = cvg.getCoverageFeature(f);
            // don't bother if already processed
            if (cgf.isProcessed()) {
                Debug.println("    Feature " + f.getID() + " already processed");
                continue;
            }
            
            Debug.println("    Feature " + f.getID() + " compute adjustments");
            cgf.computeAdjustment(cvg.getCoverageFeatureList(nearFeatures),
                                segmentMatcher, matchedSegmentIndex);
        }
        Debug.println("  6.2 computeAdjustedFeatureUpdates");
        cvg.computeAdjustedFeatureUpdates(param.distanceTolerance, param.interpolateZ, param.zScale);
    }

}
