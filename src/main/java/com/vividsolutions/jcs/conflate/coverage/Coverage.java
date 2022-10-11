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

import com.vividsolutions.jump.feature.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.Debug;

import java.util.*;

/**
 * A coverage is a set of adjacent polygons build from a FeatureCollection.
 */
public class Coverage {

    private final Map<Feature,CoverageFeature> featureMap =
        new TreeMap<>(new FeatureUtil.IDComparator());
    private final VertexMap vertexMap = new VertexMap();
    private final FeatureCollection features;
    private final FeatureUpdateRecorder updates =  new FeatureUpdateRecorder();
    private Set<Coordinate> adjustableCoords = null;
    private FeatureCollection adjustedFC;

    public Coverage(FeatureCollection features) {
        this.features = features;
    }

    public FeatureCollection getFeatures()  { return features; }
    public FeatureCollection getAdjustedFeatures()  { return adjustedFC; }
    public FeatureUpdateRecorder getUpdates()  { return updates; }

    /**
     * Sets the coordinates which can be adjusted.
     * This allows precise control over which coordinates can be adjusted.
     * For instance, coordinates which are known to be in matched segments
     * can be prevented from being adjusted.
     *
     * @param adjustableCoords a Set of the coordinates which can be adjusted
     */
    public void setAdjustableCoordinates(Set<Coordinate> adjustableCoords) {
        this.adjustableCoords = adjustableCoords;
    }

    public FeatureCollection getAdjustmentIndicators() {
        GeometryFactory fact = new GeometryFactory();
        List<Geometry> indicatorLineList = new ArrayList<>();
        Collection<Vertex> vertices = vertexMap.getVertices();
        for (Vertex v : vertices) {
            if (v.isAdjusted()) {
                Coordinate[] lineSeg = new Coordinate[] { v.getOriginalCoordinate(), v.getAdjustedCoordinate() };
                Geometry line = fact.createLineString(lineSeg);
                indicatorLineList.add(line);
            }
        }
        return FeatureDatasetFactory.createFromGeometryWithLength(indicatorLineList, "LENGTH");
    }

    public CoverageFeature getCoverageFeature(Feature f) {
        CoverageFeature cgf = featureMap.get(f);
        if (cgf == null) {
            cgf = new CoverageFeature(f, vertexMap, adjustableCoords);
            featureMap.put(f, cgf);
        }
        return cgf;
    }

   /**
    * Returns Polygon from featureList
    * @TODO : process multipolygons
    */
    public List<CoverageFeature> getCoverageFeatureList(List<Feature> featureList) {
        List<CoverageFeature> cgfList = new ArrayList<>();
        for (Feature feature : featureList) {
            // currently only polygons are handled
            if (feature.getGeometry() instanceof Polygon) {
                cgfList.add(getCoverageFeature(feature));
            }
        }
        return cgfList;
    }

    public void computeAdjustedFeatureUpdates(double distanceTolerance, boolean interpolate_z, double scale) {
        adjustedFC = new FeatureDataset(features.getFeatureSchema());
        Collection<CoverageFeature> cgfColl = featureMap.values();
        for (CoverageFeature cgf : cgfColl) {
            Debug.println("    feature " + cgf.getFeature().getID());
            if (cgf.isAdjusted(distanceTolerance, interpolate_z, scale)) {
                Debug.println("    feature " + cgf.getFeature().getID() + " is adjusted");
                Geometry g = cgf.getAdjustedGeometry(distanceTolerance, interpolate_z, scale);
                // The following tip is able to transform an auto-intersecting
                // polygon into a MultiPolygon (only if it is noded)
                if (!g.isValid()) g = g.buffer(0);
                // don't update geometry if it's not valid
//                Debug.println("    adjusted geometry for : " + cgf.getFeature().getID() + " " +
//                    ((g==null)?"g=null   ":"g!=null   ") + (g.isValid()?"g.isValid()":"!g.isValid()"));
                Feature originalFeat = cgf.getFeature();
                Feature f = originalFeat.clone(false);
                f.setGeometry(g);
                adjustedFC.add(f);
                // record this feature as an update to the original
                updates.update(cgf.getFeature(), f);
            }
        }
    }

}
