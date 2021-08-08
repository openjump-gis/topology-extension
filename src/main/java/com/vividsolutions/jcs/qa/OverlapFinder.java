/*
 *  The JCS Conflation Suite (JCS) is a library of Java classes that
 *  can be used to build automated or semi-automated conflation solutions.
 *
 *  Copyright (C) 2002 Vivid Solutions
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  For more information, contact:
 *
 *  Vivid Solutions
 *  Suite #1A
 *  2328 Government Street
 *  Victoria BC  V8T 5G5
 *  Canada
 *
 *  (250)385-6040
 *  jcs.vividsolutions.com
 */

package com.vividsolutions.jcs.qa;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.IntersectionMatrix;
import org.locationtech.jts.geom.Location;

import java.util.*;

/**
 * Finds features which overlap, in one or two datasets.
 */
public class OverlapFinder {

  private final static I18N i18n = I18N.getInstance("fr.michaelm.jump.plugin.topology");

  private static final String FEATURES = i18n.get("features");

  private final OverlappingFeatures[] overlappingFeatures;
  private final int scanFCIndex;

  private FeatureCollection overlapIndicatorFC;
  private FeatureCollection overlapSizeIndicatorFC;

  private final List<Geometry> overlapIndicators = new ArrayList<>();// a list of Geometry's
  private final List<Geometry> overlapSizeIndicators = new ArrayList<>();// a list of Geometry's
  private Envelope fence = null;

  private boolean isComputed = false;

  public OverlapFinder(
      FeatureCollection inputFC)
  {
    overlappingFeatures = new OverlappingFeatures[1];
    overlappingFeatures[0] = new OverlappingFeatures(inputFC);
    scanFCIndex = 0;
  }

  public OverlapFinder(FeatureCollection fc0, FeatureCollection fc1)
  {
    overlappingFeatures = new OverlappingFeatures[2];
    overlappingFeatures[0] = new OverlappingFeatures(fc0);
    overlappingFeatures[1] = new OverlappingFeatures(fc1);
    scanFCIndex = 1;
  }

  private boolean isSingleInput() { return overlappingFeatures.length == 1; }

  public void setFence(Envelope fence)  { this.fence = fence; }

  public FeatureCollection getOverlappingFeatures()
  {
    return getOverlappingFeatures(0);
  }

  public FeatureCollection getOverlappingFeatures(int i)
  {
    return overlappingFeatures[i].getOverlappingFeatures();
  }

  public FeatureCollection getOverlapIndicators()
  {
    return overlapIndicatorFC;
  }


  public FeatureCollection getOverlapSizeIndicators()
  {
    return overlapSizeIndicatorFC;
  }

  private FeatureCollection getQueryFC(FeatureCollection fc)
  {
    if (fence == null) return fc;
    // if using fence just scan features in fence
    List<Feature> fenceFeat = fc.query(fence);
    return new FeatureDataset(fenceFeat, fc.getFeatureSchema());
  }

  private boolean isTestNeeded(Feature f0, Feature f1)
  {
    if (! isSingleInput()) return true;
    return f0.getID() < f1.getID();
  }

  private void recordFeatures(Feature f0, Feature f1)
  {
    overlappingFeatures[0].add(f0);
    overlappingFeatures[scanFCIndex].add(f1);
  }

  public void computeOverlaps(TaskMonitor monitor)
  {
    if (isComputed) return;
    monitor.allowCancellationRequests();

    FeatureCollection queryFC = getQueryFC(overlappingFeatures[scanFCIndex].inputFC);
    monitor.report(i18n.get("qa.OverlapFinder.building-feature-index"));
    FeatureCollection indexFC = new IndexedFeatureCollection(overlappingFeatures[0].inputFC);
    int totalSegments = queryFC.size();
    int count = 0;
    monitor.report(i18n.get("qa.OverlapFinder.finding-overlaps"));
    for (Feature f : queryFC.getFeatures()) {
      monitor.report(++count, totalSegments, FEATURES);
      List<Feature> closeFeat = indexFC.query(f.getGeometry().getEnvelopeInternal());
      for (Feature closeF : closeFeat) {

        // Since the overlaps relation is symmetric, we
        // can avoid redundantly comparing each pair of features twice
        // if we only compare the smaller ID to the larger.
        // This also avoids comparing features with themselves.
        //
        // We can't actually use the OGC overlaps predicate, since it
        // is false if one geometry is wholely contained in the other.
        // Instead, we check for the interiors intersecting using relate().
        if (isTestNeeded(f, closeF)) {
          IntersectionMatrix im = f.getGeometry().relate(closeF.getGeometry());
          boolean interiorsIntersect = im.get(Location.INTERIOR, Location.INTERIOR) >= 0;
          if (interiorsIntersect) {
            recordFeatures(closeF, f);
            addIndicators(closeF, f);
          }
        }
      }
    }
    //overlappingFC = new FeatureDataset(overlappingFeatures[0], inputFC.getFeatureSchema());
    overlapIndicatorFC = FeatureDatasetFactory.createFromGeometry(overlapIndicators);
    overlapSizeIndicatorFC = FeatureDatasetFactory.createFromGeometryWithLength(overlapSizeIndicators, "LENGTH");

    isComputed = true;
  }

  /**
   * Computes indicator for a pair of overlapping geometries.
   * Tries using {@link OverlapBoundaryIndicators} first; if it
   * can't compute indicators (because of a robustness failure or a linear collapse)
   * uses the slower but more robust {@link OverlapSegmentIndicators}
   * @param f0 first feature
   * @param f1 second feature
   */
  private void addIndicators(Feature f0, Feature f1)
  {
    List<Geometry> overlapIndList;
    List<Geometry> overlapSizeIndList;

    OverlapBoundaryIndicators obi = new OverlapBoundaryIndicators(f0.getGeometry(), f1.getGeometry());
    overlapIndList = obi.getOverlapIndicators();
    overlapSizeIndList = obi.getSizeIndicators();
    if (overlapIndList.size() > 0 && overlapSizeIndList.size() > 0) {
      overlapIndicators.addAll(overlapIndList);
      overlapSizeIndicators.addAll(overlapSizeIndList);
      return;
    }

    OverlapSegmentIndicators osi = new OverlapSegmentIndicators(f0.getGeometry(), f1.getGeometry());
    overlapIndList = osi.getOverlapIndicators();
    overlapSizeIndList = osi.getSizeIndicators();
    // as long as there is at least one indicator computed, use the segment indicators
    // (there should always be segment indicators, even if there is no size indicator)
    if (overlapIndList.size() > 0 || overlapSizeIndList.size() > 0) {
      overlapIndicators.addAll(overlapIndList);
      overlapSizeIndicators.addAll(overlapSizeIndList);
      return;
    }
    // no indicators were computed - print a warning
    System.out.println(
        i18n.get("qa.OverlapFinder.warning-could-not-compute-overlap-indicators"));
    System.out.println(f0.getGeometry());
    System.out.println(f1.getGeometry());
  }

  private static class OverlappingFeatures {

    private final FeatureCollection inputFC;
    private final Set<Feature> overlappingFeatureSet;

    OverlappingFeatures(FeatureCollection inputFC)
    {
      this.inputFC = inputFC;
      overlappingFeatureSet = new TreeSet<>(new FeatureUtil.IDComparator());
    }

    FeatureCollection getOverlappingFeatures()
    {
      return new FeatureDataset(overlappingFeatureSet, inputFC.getFeatureSchema());
    }

    void add(Feature f)
    {
      overlappingFeatureSet.add(f);
    }
  }
}
