

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

package com.vividsolutions.jcs.plugin.qa;

import java.awt.Color;
import javax.swing.ImageIcon;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.util.feature.*;
import com.vividsolutions.jcs.conflate.boundarymatch.SegmentMatcher;
import com.vividsolutions.jcs.qa.MatchedSegmentFinder;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;


public class MatchedSegmentsPlugIn extends ThreadedBasePlugIn {

  private final static I18N i18n = I18N.getInstance("fr.michaelm.jump.plugin.topology");
  
  private final static String TOPOLOGY = i18n.get("Topology");
  
  private final static String GAP_SIZE_LAYER_NAME =
      i18n.get("qa.MatchedSegmentsPlugIn.gap-size-layer-name");
  
  private final static String LAYER1 = i18n.get("Layer") + " 1";
  private final static String LAYER2 = i18n.get("Layer") + " 2";
  private final static String DIST_TOL = i18n.get("dist-tolerance");
  private final static String ANGLE_TOL = i18n.get("angle-tolerance");
  
  public static final String SEGMENT_ORIENTATION =
      i18n.get("qa.MatchedSegmentsPlugIn.segment-orientation");
  public static final String SAME_ORIENTATION =
      i18n.get("qa.MatchedSegmentsPlugIn.same-orientation");
  public static final String OPPOSITE_ORIENTATION =
      i18n.get("qa.MatchedSegmentsPlugIn.opposite-orientation");
  public static final String EITHER_ORIENTATION =
      i18n.get("qa.MatchedSegmentsPlugIn.either-orientation");
  
  private Layer layer1, layer2;
  private final MatchedSegmentFinder.Parameters param = new MatchedSegmentFinder.Parameters();

  public MatchedSegmentsPlugIn() { }

  public void initialize(PlugInContext context) throws Exception {
    context.getFeatureInstaller().addMainMenuPlugin(this,
        new String[]{MenuNames.PLUGINS, TOPOLOGY},
        getName() + "...", false, null,
        new MultiEnableCheck()
            .add(context.getCheckFactory().createWindowWithLayerNamePanelMustBeActiveCheck())
            .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
  }

  public String getName() {
    return i18n.get("qa.MatchedSegmentsPlugIn.find-misaligned-segments");
  }

  public boolean execute(PlugInContext context) {
    MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(),
        i18n.get("qa.MatchedSegmentsPlugIn.find-misaligned-segments"), true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) { return false; }
    getDialogValues(dialog);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context) {
    monitor.allowCancellationRequests();

    monitor.report(
        i18n.get("qa.MatchedSegmentsPlugIn.finding-misaligned-segments") + "...");
    MatchedSegmentFinder msf = new MatchedSegmentFinder(
        layer1.getFeatureCollectionWrapper(),
        layer2.getFeatureCollectionWrapper(),
        param,
        monitor);
    
    FeatureCollection matchedSeg0 = msf.getMatchedSegments(0);
    Layer lyr = context.addLayer(StandardCategoryNames.QA,
        i18n.get("qa.MatchedSegmentsPlugIn.layer-prefix") +
        layer1.getName(), matchedSeg0);
    LayerStyleUtil.setLinearStyle(lyr, Color.red, 2, 4);
    lyr.fireAppearanceChanged();

    FeatureCollection matchedSeg1 = msf.getMatchedSegments(1);
    Layer lyr2 = context.addLayer(StandardCategoryNames.QA,
        i18n.get("qa.MatchedSegmentsPlugIn.layer-prefix") +
        layer2.getName(), matchedSeg1);
    LayerStyleUtil.setLinearStyle(lyr2, Color.green, 2, 4);
    lyr2.fireAppearanceChanged();

    FeatureCollection sizeInd = msf.getSizeIndicators();
    Layer lyrSize = context.getLayerManager().getLayer(GAP_SIZE_LAYER_NAME);

    if (lyrSize == null) {
      lyrSize = context.getLayerManager().addLayer(StandardCategoryNames.QA,
          GAP_SIZE_LAYER_NAME, sizeInd);
      LayerStyleUtil.setLinearStyle(lyrSize, Color.blue, 2, 4);
    }
    else {
      lyrSize.setFeatureCollection(sizeInd);
    }
    lyrSize.fireAppearanceChanged();
    //lyrSize.setDescription("Gap Size Indicators (Distance Tol = " + param.distanceTolerance + ")");
    lyrSize.setDescription(
        i18n.get("qa.MatchedSegmentsPlugIn.gap-size-indicator") +
        " (" + DIST_TOL + " = " + param.distanceTolerance + ")");

    createOutput(context, matchedSeg0, matchedSeg1, sizeInd);

  }
  private void createOutput(PlugInContext context,
      FeatureCollection matchedSeg1,
      FeatureCollection matchedSeg2,
      FeatureCollection sizeInd)
  {
    context.getOutputFrame().createNewDocument();
    context.getOutputFrame().addHeader(1,
        i18n.get("qa.MatchedSegmentsPlugIn.misaligned-segments"));
    context.getOutputFrame().addField(LAYER1 + ": ", layer1.getName() );
    context.getOutputFrame().addField(LAYER2 + ": ", layer2.getName() );
    context.getOutputFrame().addField(i18n.get("dist-tolerance")
        + ": ", "" + param.distanceTolerance);
    context.getOutputFrame().addField(i18n.get("angle-tolerance")
        + ": ", "" + param.angleTolerance);
    context.getOutputFrame().addText(" ");

    context.getOutputFrame().addField(
        i18n.get("qa.MatchedSegmentsPlugIn.nb-misaligned-segments-in") +
        layer1.getName() + ": ", "" + matchedSeg1.size());
    context.getOutputFrame().addField(
        i18n.get("qa.MatchedSegmentsPlugIn.nb-misaligned-segments-in") +
        layer2.getName() + ": ", "" + matchedSeg2.size());

    double[] minMax = FeatureStatistics.minMaxValue(sizeInd, "LENGTH");
    context.getOutputFrame().addField(
        i18n.get("qa.MatchedSegmentsPlugIn.min-gap-size"), "" + minMax[0]);
    context.getOutputFrame().addField(
        i18n.get("qa.MatchedSegmentsPlugIn.max-gap-size"), "" + minMax[1]);
  }

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarImage(new ImageIcon(getClass().getResource("MatchSegments.png")));
    dialog.setSideBarDescription(
        i18n.get("qa.MatchedSegmentsPlugIn.description") + " " +
            i18n.get("qa.MatchedSegmentsPlugIn.more-description")
    );
    dialog.addLayerComboBox(LAYER1, context.getCandidateLayer(0),
                                null, context.getLayerManager());
    Layer lyr2 = context.getSelectedLayers().length > 1 ?
        context.getCandidateLayer(1) :
        context.getCandidateLayer(0);
    dialog.addLayerComboBox(LAYER2, lyr2, null, context.getLayerManager());
    dialog.addDoubleField(DIST_TOL, param.distanceTolerance, 4,
        i18n.get("qa.MatchedSegmentsPlugIn.dist-tolerance-tooltip"));
    dialog.addDoubleField(ANGLE_TOL, param.angleTolerance, 8,
        i18n.get("qa.MatchedSegmentsPlugIn.angle-tolerance-tooltip"));
    dialog.addLabel(SEGMENT_ORIENTATION);
    dialog.addRadioButton(
        EITHER_ORIENTATION, SEGMENT_ORIENTATION, true,
        i18n.get("qa.MatchedSegmentsPlugIn.either-orientation-tooltip")
    );
    dialog.addRadioButton(
        OPPOSITE_ORIENTATION, SEGMENT_ORIENTATION, false,
        i18n.get("qa.MatchedSegmentsPlugIn.opposite-orientation-tooltip")
    );
    dialog.addRadioButton(
        SAME_ORIENTATION, SEGMENT_ORIENTATION, false,
        i18n.get("qa.MatchedSegmentsPlugIn.same-orientation-tooltip")
    );
  }

  private void getDialogValues(MultiInputDialog dialog) {
    layer1 = dialog.getLayer(LAYER1);
    layer2 = dialog.getLayer(LAYER2);
    param.distanceTolerance = dialog.getDouble(DIST_TOL);
    param.angleTolerance = dialog.getDouble(ANGLE_TOL);
    if (dialog.getBoolean(EITHER_ORIENTATION))
        param.segmentOrientation = SegmentMatcher.EITHER_ORIENTATION;
    else if (dialog.getBoolean(OPPOSITE_ORIENTATION))
        param.segmentOrientation = SegmentMatcher.OPPOSITE_ORIENTATION;
    else if (dialog.getBoolean(SAME_ORIENTATION))
        param.segmentOrientation = SegmentMatcher.SAME_ORIENTATION;
  }

}
