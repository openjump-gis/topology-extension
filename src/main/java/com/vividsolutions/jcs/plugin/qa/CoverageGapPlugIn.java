

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
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.util.feature.*;
import com.vividsolutions.jcs.qa.*;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.*;


public class CoverageGapPlugIn extends ThreadedBasePlugIn {

  private final static I18N i18n = I18N.getInstance("fr.michaelm.jump.plugin.topology");

  private final static String TOPOLOGY = i18n.get("Topology");
  public final static String GAP_SEGMENT_LAYER_NAME =
      i18n.get("qa.CoverageGapPlugIn.gap-segment");
  public final static String GAP_SIZE_LAYER_NAME =
      i18n.get("qa.CoverageGapPlugIn.gap-size");

  private final static String LAYER = i18n.get("Layer");
  private final static String DIST_TOL = i18n.get("dist-tolerance");
  private final static String ANGLE_TOL = i18n.get("angle-tolerance");
  private final static String CREATE_NEW_LAYERS = i18n.get("create-new-layers");
  private final static String USE_FENCE = i18n.get("use-fence");

  private Layer layer;
  private final InternalMatchedSegmentFinder.Parameters param
      = new InternalMatchedSegmentFinder.Parameters();
  private boolean createNewLayers;
  private boolean useFence;

  public CoverageGapPlugIn() { }

  public void initialize(PlugInContext context) throws Exception {
    context.getFeatureInstaller().addMainMenuPlugin(this,
        new String[]{MenuNames.PLUGINS, TOPOLOGY},
        getName() + "...", false, null,
        new MultiEnableCheck()
            .add(context.getCheckFactory().createTaskWindowMustBeActiveCheck())
            .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
  }

  public String getName() {
    return i18n.get("qa.CoverageGapPlugIn.find-coverage-gaps");
  }

  public boolean execute(PlugInContext context) {
    MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(),
        i18n.get("qa.CoverageGapPlugIn.find-coverage-gaps"),
        true);
    setDialogValues(dialog, context);
    GUIUtil.centreOnWindow(dialog);
    dialog.setVisible(true);
    if (!dialog.wasOKPressed()) { return false; }
    getDialogValues(dialog);
    return true;
  }

  public void run(TaskMonitor monitor, PlugInContext context) {
    monitor.allowCancellationRequests();

    monitor.report(i18n.get("qa.CoverageGapPlugIn.finding-gaps")+"...");
    computeMatchedSegments(monitor, context);
    if (monitor.isCancelRequested()) return;
  }

  private void computeMatchedSegments(TaskMonitor monitor, PlugInContext context) {
    InternalMatchedSegmentFinder msf = new InternalMatchedSegmentFinder(
        layer.getFeatureCollectionWrapper(), param, monitor);
    if (useFence) {
      if (context.getLayerViewPanel().getFence() == null) {
        context.getWorkbenchFrame().warnUser(i18n.get("no-fence-defined"));
        return;
      }
      msf.setFence(context.getLayerViewPanel().getFence());
    }

    FeatureCollection segs = msf.getMatchedSegments();
    FeatureCollection sizeInd = msf.getSizeIndicators();
    if (monitor.isCancelRequested()) return;

    Layer lyr = null;
    String lyrName = GAP_SEGMENT_LAYER_NAME;
    if (createNewLayers)
      lyrName = GAP_SEGMENT_LAYER_NAME + " - " + layer.getName();
    else
      lyr = context.getLayerManager().getLayer(GAP_SEGMENT_LAYER_NAME);

    if (lyr == null) {
      lyr = context.getLayerManager().addLayer(StandardCategoryNames.QA,
          lyrName, segs);
      LayerStyleUtil.setLinearStyle(lyr, Color.red, 2, 4);
    }
    else {
      lyr.setFeatureCollection(segs);
    }
    lyr.fireAppearanceChanged();
    lyr.setDescription(i18n.get("qa.CoverageGapPlugIn.gap-segment-indicator") +
                           " (" + DIST_TOL + " = " + param.distanceTolerance + ")");

    Layer lyr2 = null;
    String lyr2Name = GAP_SIZE_LAYER_NAME;
    if (createNewLayers)
      lyr2Name = GAP_SIZE_LAYER_NAME + " - " + layer.getName();
    else
      lyr2 = context.getLayerManager().getLayer(GAP_SIZE_LAYER_NAME);

    if (lyr2 == null) {
      lyr2 = context.getLayerManager().addLayer(StandardCategoryNames.QA,
          lyr2Name, sizeInd);
      LayerStyleUtil.setLinearStyle(lyr2, Color.blue, 2, 4);
    }
    else {
      lyr2.setFeatureCollection(sizeInd);
    }
    lyr2.fireAppearanceChanged();
    lyr2.setDescription(i18n.get("qa.CoverageGapPlugIn.gap-size-indicator") +
                           " (" + DIST_TOL + " = " + param.distanceTolerance + ")");

    createdOutput(context, segs, sizeInd);
  }

  private void createdOutput(PlugInContext context,
      FeatureCollection segs,
      FeatureCollection sizeInd)
  {
    context.getOutputFrame().createNewDocument();
    context.getOutputFrame().addHeader(1,
        i18n.get("qa.CoverageGapPlugIn.coverage-gap"));
    context.getOutputFrame().addField(
        i18n.get("Layer") + ": ", layer.getName() );
    context.getOutputFrame().addField(
        i18n.get("dist-tolerance") + ": ", "" + param.distanceTolerance);
    context.getOutputFrame().addField(
        i18n.get("dist-tolerance") + ": ", "" + param.angleTolerance);
    context.getOutputFrame().addText(" ");

    context.getOutputFrame().addField(
        i18n.get("qa.CoverageGapPlugIn.nb-matched-segments") + ": ", "" + segs.size());
    context.getOutputFrame().addField(
        i18n.get("qa.CoverageGapPlugIn.nb-coverage-gaps") + ": ", "" + sizeInd.size());

    double[] minMax = FeatureStatistics.minMaxValue(sizeInd, "LENGTH");
    context.getOutputFrame().addField(
        i18n.get("qa.CoverageGapPlugIn.min-gap-size"), "" + minMax[0]);
    context.getOutputFrame().addField(
        i18n.get("qa.CoverageGapPlugIn.max-gap-size"), "" + minMax[1]);

  }


  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    dialog.setSideBarImage(new ImageIcon(getClass().getResource("CoverageGap.png")));
    dialog.setSideBarDescription(
        i18n.get("qa.CoverageGapPlugIn.find-gaps-and-slivers"));
    dialog.addLayerComboBox(LAYER, context.getCandidateLayer(0), null, context.getLayerManager());
    dialog.addDoubleField(DIST_TOL, param.distanceTolerance, 8,
        i18n.get("qa.CoverageGapPlugIn.dist-tolerance-tooltip"));
    dialog.addDoubleField(ANGLE_TOL, param.angleTolerance, 8,
        i18n.get("qa.CoverageGapPlugIn.angle-tolerance-tooltip"));
    dialog.addCheckBox(CREATE_NEW_LAYERS, false,
        i18n.get("create-new-layers-for-the-output"));
    dialog.addCheckBox(USE_FENCE, false,
        i18n.get("qa.CoverageGapPlugIn.process-segments-in-fence-only"));
  }

  private void getDialogValues(MultiInputDialog dialog) {
    layer = dialog.getLayer(LAYER);
    param.distanceTolerance = dialog.getDouble(DIST_TOL);
    param.angleTolerance = dialog.getDouble(ANGLE_TOL);
    createNewLayers = dialog.getBoolean(CREATE_NEW_LAYERS);
    useFence = dialog.getBoolean(USE_FENCE);
  }

}
