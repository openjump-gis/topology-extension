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

package com.vividsolutions.jcs.plugin.clean;

import java.awt.Color;
import javax.swing.*;

import com.vividsolutions.jcs.conflate.coverage.CoverageCleaner;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.util.feature.FeatureStatistics;
import com.vividsolutions.jump.workbench.model.*;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.task.*;
import com.vividsolutions.jump.workbench.ui.*;

import org.locationtech.jts.geom.Geometry;


public class CoverageCleanerPlugIn extends ThreadedBasePlugIn {

    private final static I18N i18n = I18N.getInstance("fr.michaelm.jump.plugin.topology");

    private final static String TOPOLOGY         = i18n.get("Topology");
    private final static String LAYER            = i18n.get("Layer");
    private final static String EXPLODE          = i18n.get("qa.CoverageCleanerPlugIn.explode-first");
    private final static String NORMALIZE        = i18n.get("qa.CoverageCleanerPlugIn.normalize-first");
    private final static String DIST_TOL         = i18n.get("dist-tolerance");
    private final static String ANGLE_TOL        = i18n.get("angle-tolerance");
    private final static String USE_FENCE        = i18n.get("use-fence");
    private final static String INTERPOLATE_Z    = i18n.get("qa.CoverageCleanerPlugIn.interpolate-z");
    private final static String INTERPOLATE_Z_TT = i18n.get("qa.CoverageCleanerPlugIn.interpolate-z-tooltip");
    private final static String Z_PRECISION      = i18n.get("qa.CoverageCleanerPlugIn.z-precision");
    private final static String Z_PRECISION_TT   = i18n.get("qa.CoverageCleanerPlugIn.z-precision-tooltip");

    private Layer layer;
    private final CoverageCleaner.Parameters param = new CoverageCleaner.Parameters();
    private boolean useFence;
    private boolean explode = true;
    private boolean normalize = true;
    private int zPrecision = 1;

    public CoverageCleanerPlugIn() { }

    /**
     * Returns a very brief description of this task.
     * @return the name of this task
     */
    public String getName() {
        return i18n.get("qa.CoverageCleanerPlugIn.coverage-cleaner");
    }

    public void initialize(PlugInContext context) throws Exception {
        context.getFeatureInstaller().addMainMenuPlugin(this,
            new String[]{MenuNames.PLUGINS, TOPOLOGY},
            getName() + "...", false, null,
            new MultiEnableCheck()
                .add(context.getCheckFactory().createTaskWindowMustBeActiveCheck())
                .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
    }

    public boolean execute(PlugInContext context) throws Exception {
        MultiInputDialog dialog = new MultiInputDialog(
            context.getWorkbenchFrame(),
            i18n.get("qa.CoverageCleanerPlugIn.adjust-polygon-boundaries"),
            true);
        setDialogValues(dialog, context);
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (!dialog.wasOKPressed()) { return false; }
        getDialogValues(dialog);
        return true;
    }

    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();
        FeatureCollection inputFC;
        if (explode || normalize) {
            inputFC = explodeOrNormalize(layer.getFeatureCollectionWrapper());
        }
        else inputFC = layer.getFeatureCollectionWrapper();
        CoverageCleaner cleaner = new CoverageCleaner(inputFC, monitor);
        if (useFence) {
            if (context.getLayerViewPanel().getFence() == null) {
              context.getWorkbenchFrame().warnUser(
                  i18n.get("no-fence-defined"));
              return;
            }
            cleaner.setFence(context.getLayerViewPanel().getFence());
        }
        monitor.report(i18n.get("qa.CoverageCleanerPlugIn.adjusting") + "...");
        cleaner.process(param);
        if (monitor.isCancelRequested()) return;
        createLayers(context, cleaner);
    }

    private void createLayers(PlugInContext context, CoverageCleaner cleaner) {
        context.addLayer(
            StandardCategoryNames.RESULT_SUBJECT,
            layer.getName(),
            cleaner.getUpdatedFeatures());

        FeatureCollection adjustedFC = cleaner.getAdjustedFeatures();
        Layer lyr = context.addLayer(
            StandardCategoryNames.QA,
            i18n.get("qa.CoverageCleanerPlugIn.adjusted") + "-" + layer.getName(),
            adjustedFC);
        lyr.setDescription(
            i18n.get("qa.CoverageCleanerPlugIn.adjusted-features-for") +
            " " + layer.getName() + " (" +
                i18n.get("dist-tolerance") + " = " + param.distanceTolerance + ")");

        FeatureCollection adjustmentIndFC = cleaner.getAdjustmentIndicators();
        Layer lyr2 = context.addLayer(
            StandardCategoryNames.QA,
            i18n.get("qa.CoverageCleanerPlugIn.adjustements") + "-" + layer.getName(),
            adjustmentIndFC);
        LayerStyleUtil.setLinearStyle(lyr2, Color.blue, 2, 4);
        lyr2.fireAppearanceChanged();
        lyr2.setDescription(
            i18n.get("qa.CoverageCleanerPlugIn.adjustement-size-indicator-for") + " " +
            layer.getName() + " (" +
                i18n.get("dist-tolerance") + " = " + param.distanceTolerance + ")");

        createOutput(context, adjustedFC, adjustmentIndFC);

    }

    private void createOutput(PlugInContext context,
                              FeatureCollection adjustedFC,
                              FeatureCollection adjustmentIndFC) {
        context.getOutputFrame().createNewDocument();
        context.getOutputFrame().addHeader(1,
            i18n.get("qa.CoverageCleanerPlugIn.coverage-cleaner"));
        context.getOutputFrame().addField(i18n.get("Layer") + ": ", layer.getName() );
        context.getOutputFrame().addField(i18n.get("dist-tolerance") + ": ", "" + param.distanceTolerance);
        context.getOutputFrame().addField(i18n.get("angle-tolerance") + ": ", "" + param.angleTolerance);
        
        context.getOutputFrame().addHeader(2,
            i18n.get("qa.CoverageCleanerPlugIn.adjustements"));
        context.getOutputFrame().addField(
            i18n.get("qa.CoverageCleanerPlugIn.number-of-features-adjusted"), "" + adjustedFC.size());
        context.getOutputFrame().addField(
            i18n.get("qa.CoverageCleanerPlugIn.number-of-vertices-adjusted"), "" + adjustmentIndFC.size());
        
        double[] minMax = FeatureStatistics.minMaxValue(adjustmentIndFC, "LENGTH");
        context.getOutputFrame().addField(
            i18n.get("qa.CoverageCleanerPlugIn.min-adjustment-size"), "" + minMax[0]);
        context.getOutputFrame().addField(
            i18n.get("qa.CoverageCleanerPlugIn.max-adjustment-size"), "" + minMax[1]);
    }

    private void setDialogValues(MultiInputDialog dialog, PlugInContext context) {
      dialog.setSideBarDescription(
          i18n.get("qa.CoverageCleanerPlugIn.description")
      );
      String fieldName = LAYER;
      JComboBox addLayerComboBox = dialog.addLayerComboBox(fieldName,
          context.getCandidateLayer(0), null, context.getLayerManager());
      dialog.addCheckBox(EXPLODE, explode,
          i18n.get("qa.CoverageCleanerPlugIn.explode-first-definition"));
      dialog.addCheckBox(NORMALIZE, normalize,
          i18n.get("qa.CoverageCleanerPlugIn.normalize-first-definition"));
      dialog.addDoubleField(DIST_TOL, param.distanceTolerance, 8,
          i18n.get("qa.CoverageCleanerPlugIn.dist-tolerance-definition"));
      dialog.addDoubleField(ANGLE_TOL, param.angleTolerance, 4,
          i18n.get("qa.CoverageCleanerPlugIn.angle-tolerance-definition"));

      dialog.addSeparator();

      final JCheckBox interpolateZCB = dialog.addCheckBox(INTERPOLATE_Z, param.interpolateZ, INTERPOLATE_Z_TT);
      final JTextField zPrecisionTF = dialog.addIntegerField(Z_PRECISION, zPrecision, 4, Z_PRECISION_TT);

      dialog.addSeparator();

      dialog.addCheckBox(USE_FENCE, useFence,
          i18n.get("qa.CoverageCleanerPlugIn.process-segments-in-fence-only"));

      zPrecisionTF.setEnabled(interpolateZCB.isSelected());
      interpolateZCB.addActionListener(e -> zPrecisionTF.setEnabled(interpolateZCB.isSelected()));
    }

    private void getDialogValues(MultiInputDialog dialog) {
        layer = dialog.getLayer(LAYER);
        param.distanceTolerance = dialog.getDouble(DIST_TOL);
        param.angleTolerance = dialog.getDouble(ANGLE_TOL);
        useFence = dialog.getBoolean(USE_FENCE);
        param.interpolateZ = dialog.getBoolean(INTERPOLATE_Z);
        zPrecision = dialog.getInteger(Z_PRECISION);
        param.zScale = Math.pow(10, zPrecision);
    }
    
    private FeatureCollection explodeOrNormalize(FeatureCollection fc) {
        FeatureCollection result = new FeatureDataset(fc.getFeatureSchema());
        for (Feature feature : fc.getFeatures()) {
            Geometry geometry = feature.getGeometry();
            if (explode) {
                for (int i = 0 ; i < geometry.getNumGeometries() ; i++) {
                    Feature newFeature = feature.clone(false);
                    if (normalize) newFeature.setGeometry(geometry.getGeometryN(i).norm());
                    else newFeature.setGeometry(geometry.getGeometryN(i).copy());
                    result.add(newFeature);
                }
            }
            else {
                Feature newFeature = feature.clone(false);
                if (normalize) newFeature.setGeometry(geometry.norm());
                else newFeature.setGeometry(geometry.copy());
                result.add(newFeature);
            }
        }
        return result;
    }

}
