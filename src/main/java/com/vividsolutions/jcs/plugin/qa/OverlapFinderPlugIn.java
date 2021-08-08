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

package com.vividsolutions.jcs.plugin.qa;

import com.vividsolutions.jcs.qa.OverlapFinder;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.ColorUtil;
import com.vividsolutions.jump.util.feature.FeatureStatistics;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.LayerStyleUtil;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;

import java.awt.*;

public class OverlapFinderPlugIn extends ThreadedBasePlugIn {

  private final static I18N i18n = I18N.getInstance("fr.michaelm.jump.plugin.topology");

  private final static String TOPOLOGY = i18n.get("Topology");
  private final static String LAYER1 = i18n.get("Layer") + " 1";
  private final static String LAYER2 = i18n.get("Layer") + " 2";
  private final static String CREATE_NEW_LAYERS = i18n.get("create-new-layers");

  private Layer layer1, layer2;
  private boolean createNewLayers;

  public OverlapFinderPlugIn() { }

  public void initialize(PlugInContext context) throws Exception {
    context.getFeatureInstaller().addMainMenuPlugin(this,
        new String[]{MenuNames.PLUGINS, TOPOLOGY},
        getName() + "...", false, null,
        new MultiEnableCheck()
            .add(context.getCheckFactory().createTaskWindowMustBeActiveCheck())
            .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(2)));
  }

  public String getName() {
    return i18n.get("qa.OverlapFinderPlugIn.find-overlaps");
  }

  public boolean execute(PlugInContext context) {
    MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(),
        i18n.get("qa.OverlapFinderPlugIn.find-overlaps"),
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
    computeOverlaps(monitor, context);
  }

  private void computeOverlaps(TaskMonitor monitor, PlugInContext context) {
    OverlapFinder ovf = new OverlapFinder(
        layer1.getFeatureCollectionWrapper(),
        layer2.getFeatureCollectionWrapper()
        );
    ovf.computeOverlaps(monitor);
    FeatureCollection overlaps1 = ovf.getOverlappingFeatures(0);
    FeatureCollection overlaps2 = ovf.getOverlappingFeatures(1);
    FeatureCollection overlapInd = ovf.getOverlapIndicators();
    FeatureCollection overlapSizeInd = ovf.getOverlapSizeIndicators();

    if (monitor.isCancelRequested()) return;
    if (overlaps1.size() > 0) {
      createLayers(context, overlaps1, overlaps2, overlapInd, overlapSizeInd);
    }
    createOutput(context, overlaps1, overlaps2, overlapSizeInd);
  }

  private void createOverlapLayer(PlugInContext context,
                                  Layer inputLayer,
                                  FeatureCollection overlaps, Color fillColor)
  {
    String overlapLayerName = i18n.get("qa.OverlapFinderPlugIn.overlaps") +
        " " + inputLayer.getName();
    Layer lyr;
    if (createNewLayers) {
      lyr = context.addLayer(StandardCategoryNames.QA,
                             overlapLayerName + " - " + inputLayer.getName(), overlaps);
    }
    else {
      lyr = context.getLayerManager().addOrReplaceLayer(StandardCategoryNames.QA,
          overlapLayerName, overlaps);
    }
    lyr.getBasicStyle().setFillColor(fillColor);
    lyr.getBasicStyle().setLineColor(fillColor.darker());
    lyr.fireAppearanceChanged();
    lyr.setDescription(i18n.get("qa.OverlapFinderPlugIn.overlaps-for") +
        inputLayer.getName());
  }

  private void createLayers(PlugInContext context,
                            FeatureCollection overlaps1,
                            FeatureCollection overlaps2,
                            FeatureCollection overlapInd,
                            FeatureCollection overlapSizeInd)
  {

      // need to sort out what happens if overlap layers exist already before creating them
      createOverlapLayer(context, layer1, overlaps1, ColorUtil.GOLD);
      createOverlapLayer(context, layer2, overlaps2, ColorUtil.PALE_BLUE);

      String overlapSegLayerName =
          i18n.get("qa.OverlapFinderPlugIn.overlap-segments");
      Layer lyr2;
      if (createNewLayers) {
        lyr2 = context.addLayer(StandardCategoryNames.QA,
                                overlapSegLayerName, overlapInd);
      }
      else {
        lyr2 = context.getLayerManager().addOrReplaceLayer(StandardCategoryNames.QA,
            overlapSegLayerName, overlapInd);
      }
      LayerStyleUtil.setLinearStyle(lyr2, Color.red, 2, 4);
      lyr2.fireAppearanceChanged();
      lyr2.setDescription(i18n.get("qa.OverlapFinderPlugIn.overlap-segments"));

      String overlapSizeLayerName =
          i18n.get("qa.OverlapFinderPlugIn.overlap-size");
      Layer lyr3;
      if (createNewLayers) {
        lyr3 = context.addLayer(StandardCategoryNames.QA,
                                overlapSizeLayerName, overlapSizeInd);
      }
      else {
        lyr3 = context.getLayerManager().addOrReplaceLayer(StandardCategoryNames.QA,
            overlapSizeLayerName, overlapSizeInd);
      }
      LayerStyleUtil.setLinearStyle(lyr3, Color.blue, 2, 4);
      lyr3.fireAppearanceChanged();
      lyr3.setDescription(
          i18n.get("qa.OverlapFinderPlugIn.overlap-size-indicators"));

  }

  private void createOutput(PlugInContext context,
                            FeatureCollection overlaps1,
                            FeatureCollection overlaps2,
                            FeatureCollection overlapSizeInd)
    {
    context.getOutputFrame().createNewDocument();
    context.getOutputFrame().addHeader(1,
        i18n.get("qa.OverlapFinderPlugIn.overlaps"));
    context.getOutputFrame().addField(LAYER1 + ": ", layer1.getName() );
    context.getOutputFrame().addField(LAYER2 + ": ", layer2.getName() );
    context.getOutputFrame().addText(" ");

    context.getOutputFrame().addField(
        i18n.get("qa.OverlapFinderPlugIn.nb-overlapping-features-in") +
        layer1.getName() + ": ", "" + overlaps1.size());
    context.getOutputFrame().addField(
        i18n.get("qa.OverlapFinderPlugIn.nb-overlapping-features-in") +
        layer2.getName() + ": ", "" + overlaps2.size());

    double[] minMax = FeatureStatistics.minMaxValue(overlapSizeInd, "LENGTH");
    context.getOutputFrame().addField(
        i18n.get("qa.OverlapFinderPlugIn.min-overlap-size"), "" + minMax[0]);
    context.getOutputFrame().addField(
        i18n.get("qa.OverlapFinderPlugIn.max-overlap-size"), "" + minMax[1]);

  }

  private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
    //dialog.setSideBarImage(new ImageIcon(getClass().getResource("CoverageOverlap.png")));
    dialog.setSideBarDescription(
        i18n.get("qa.OverlapFinderPlugIn.find-overlaps-in-two-datasets"));
    dialog.addLayerComboBox(LAYER1,
        context.getLayerManager().getLayer(0), null, context.getLayerManager());
    dialog.addLayerComboBox(LAYER2,
        context.getLayerManager().getLayer(1), null, context.getLayerManager());
    dialog.addCheckBox(CREATE_NEW_LAYERS, false,
        i18n.get("create-new-layers-for-the-output"));
  }

  private void getDialogValues(MultiInputDialog dialog) {
    layer1 = dialog.getLayer(LAYER1);
    layer2 = dialog.getLayer(LAYER2);
    createNewLayers = dialog.getBoolean(CREATE_NEW_LAYERS);
  }

}
