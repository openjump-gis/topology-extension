/*
 * Library name : fr.michaelm.jump.plugin.topology
 * (C) 2012 Michaël Michaud
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
 * michael.michaud@free.fr
 *
 */

package fr.michaelm.jump.plugin.topology;


import java.awt.Component;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.*;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiInputDialog;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * PlugIn to project points features on linear network.
 * // TODO make the insert and split options undoable.
 * @author Michael Michaud
 * @version 0.8.0 (2014-06-25)
 */
// History
// 0.8.1 (2014-05-27) Fix a bug affecting multiple target option
// 0.8.0 (2014-05-24) Plugin has been completely re-coded from scratch
// 0.7.1 (2014-05-17) Fix a bug by inserting new points inside the main loop
// 0.6   (2012-09-17) complete rewrite to be able to do multi-projections
// 0.1.0 (2012-12-25) initial version
public class ProjectPointsOnLinesPlugIn extends ThreadedBasePlugIn {

    private final static I18N i18n = I18N.getInstance("fr.michaelm.jump.plugin.topology");

    private static String TOPOLOGY;
    private static String PROJECT_POINTS_ON_LINES;
    
    private static String POINT_LAYER;
    private static String POINT_LAYER_TOOLTIP;
    
    private static String TARGET_LAYER;
    private static String TARGET_LAYER_TOOLTIP;
        
    private static String TOLERANCE;
    private static String TOLERANCE_TOOLTIP;
    
    private static String SNAP;
    private static String SNAP_TOOLTIP;
    private static String SNAP_TOLERANCE;
    private static String SNAP_TOLERANCE_TOOLTIP;
    
    private static String PROJECT;
    private static String PROJECT_TOOLTIP;
    
    private static String MODIFY_TARGET_LAYER;
    private static String MODIFY_TARGET_LAYER_TOOLTIP;
    private static String NO_OPERATION;
    private static String INSERT;
    private static String SPLIT;
    
    private static String NEAREST_PROJ_ONLY;
    private static String ALL_PROJ_WITHIN_TOLERANCE;  
        
    private static String CREATE_LINK_LAYER;
    private static String CREATE_LINK_LAYER_TOOLTIP;
    private static String PROJECTED_DISTANCE;
    private static String PROJECTED;
    private static String LINKS;
    
    private static String POINTS_PROCESSED;
    private static String NO_POINT_IN_POINT_LAYER;
    private static String NO_FEATURE_IN_TARGET_LAYER;
    
    Layer point_layer;
    Layer target_layer;
    double tolerance = 10.0;
    double snap_tolerance = 0.0;
    
    boolean project = true;
    String operation;
    boolean insert = false;
    boolean split = false;
    boolean nearest_proj_only = true;
    boolean all_proj_within_tolerance = false;   
    
    boolean create_link_layer = false;
    boolean add_dist_attribute = false;

    public String getName() {
        return i18n.get("ProjectPointsOnLinesPlugIn.project-points-on-lines");
    }

    public void initialize(final PlugInContext context) throws Exception {
        
        TOPOLOGY                    = i18n.get("Topology");
        PROJECT_POINTS_ON_LINES     = i18n.get("ProjectPointsOnLinesPlugIn.project-points-on-lines");
                                   
        POINT_LAYER                 = i18n.get("ProjectPointsOnLinesPlugIn.point-layer");
        POINT_LAYER_TOOLTIP         = i18n.get("ProjectPointsOnLinesPlugIn.point-layer-tooltip");
        TARGET_LAYER                = i18n.get("ProjectPointsOnLinesPlugIn.target-layer");
        TARGET_LAYER_TOOLTIP        = i18n.get("ProjectPointsOnLinesPlugIn.target-layer-tooltip");
                                   
        TOLERANCE                   = i18n.get("ProjectPointsOnLinesPlugIn.distance-tolerance");
        TOLERANCE_TOOLTIP           = i18n.get("ProjectPointsOnLinesPlugIn.distance-tolerance-tooltip");
        SNAP                        = i18n.get("ProjectPointsOnLinesPlugIn.vertex-snapping");
        SNAP_TOOLTIP                = i18n.get("ProjectPointsOnLinesPlugIn.vertex-snapping-tooltip");
        SNAP_TOLERANCE              = i18n.get("ProjectPointsOnLinesPlugIn.snap-tolerance");
        SNAP_TOLERANCE_TOOLTIP      = i18n.get("ProjectPointsOnLinesPlugIn.snap-tolerance-tooltip");
        
        PROJECT                     = i18n.get("ProjectPointsOnLinesPlugIn.project");
        PROJECT_TOOLTIP             = i18n.get("ProjectPointsOnLinesPlugIn.project-tooltip");
        
        MODIFY_TARGET_LAYER         = i18n.get("ProjectPointsOnLinesPlugIn.modify-target-layer");
        MODIFY_TARGET_LAYER_TOOLTIP = i18n.get("ProjectPointsOnLinesPlugIn.modify-target-layer-tooltip");
        NO_OPERATION                = i18n.get("ProjectPointsOnLinesPlugIn.no-operation");
        INSERT                      = i18n.get("ProjectPointsOnLinesPlugIn.insert");
        SPLIT                       = i18n.get("ProjectPointsOnLinesPlugIn.split");
        
        NEAREST_PROJ_ONLY           = i18n.get("ProjectPointsOnLinesPlugIn.nearest-proj-only");
        ALL_PROJ_WITHIN_TOLERANCE   = i18n.get("ProjectPointsOnLinesPlugIn.all-proj-within-tolerance");
        
        CREATE_LINK_LAYER           = i18n.get("ProjectPointsOnLinesPlugIn.create-link-layer");
        CREATE_LINK_LAYER_TOOLTIP   = i18n.get("ProjectPointsOnLinesPlugIn.create-link-layer-tooltip");
        PROJECTED_DISTANCE          = i18n.get("ProjectPointsOnLinesPlugIn.projected-distance");
        PROJECTED                   = i18n.get("ProjectPointsOnLinesPlugIn.projected");
        LINKS                       = i18n.get("ProjectPointsOnLinesPlugIn.links");
        
        POINTS_PROCESSED            = i18n.get("ProjectPointsOnLinesPlugIn.points-processed");
        NO_POINT_IN_POINT_LAYER     = i18n.get("ProjectPointsOnLinesPlugIn.no-point-in-point-layer");
        NO_FEATURE_IN_TARGET_LAYER  = i18n.get("ProjectPointsOnLinesPlugIn.no-feature-in-target-layer");
        
        context.getFeatureInstaller().addMainMenuPlugin(this,
            new String[]{MenuNames.PLUGINS, TOPOLOGY},
            getName() + "...", false, null,
            new MultiEnableCheck()
                .add(context.getCheckFactory().createTaskWindowMustBeActiveCheck())
                .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(2)));
    }
    
    public boolean execute(PlugInContext context) {
        
        final MultiInputDialog dialog = new MultiInputDialog(
        context.getWorkbenchFrame(), PROJECT_POINTS_ON_LINES, true);
        // Point layer
        if (point_layer == null || !context.getLayerManager().getLayers().contains(point_layer)) {
            point_layer = context.getCandidateLayer(0);
        }
        final JComboBox jcb_point_layer = dialog.addLayerComboBox(
            POINT_LAYER, point_layer, null, context.getLayerManager());
        
        // Line layer
        if (target_layer == null || !context.getLayerManager().getLayers().contains(target_layer)) {
            int number_of_layers = context.getLayerManager().getLayers().size();
            if (number_of_layers > 1) target_layer = context.getCandidateLayer(1);
            else target_layer = context.getCandidateLayer(0); // should never be the case
        }
        final JComboBox jcb_line_layer = dialog
                .addLayerComboBox(TARGET_LAYER, target_layer, null, context.getLayerManager());
        
        // Distance options
        final JTextField jtf_dist_tolerance = dialog
                .addDoubleField(TOLERANCE, tolerance, 12, TOLERANCE_TOOLTIP);
        //final JCheckBox jcb_snap = dialog.addCheckBox(SNAP, snap, SNAP_TOOLTIP);
        final JTextField jtf_snap_tolerance = dialog
                .addDoubleField(SNAP_TOLERANCE, snap_tolerance, 12, SNAP_TOLERANCE_TOOLTIP);
        
        // Output options
        dialog.addSeparator();
        final JCheckBox jcb_project = dialog.addCheckBox(PROJECT, project, PROJECT_TOOLTIP);
        
        final JRadioButton jrb_nearest_proj_only = dialog
                .addRadioButton(NEAREST_PROJ_ONLY, "MULTI", nearest_proj_only, null);
        final JRadioButton jrb_all_proj_within_tolerance = dialog
                .addRadioButton(ALL_PROJ_WITHIN_TOLERANCE, "MULTI", all_proj_within_tolerance, null);
        final JComboBox jcb_line_operation = dialog
                .addComboBox(MODIFY_TARGET_LAYER, NO_OPERATION,
                             Arrays.asList(NO_OPERATION, INSERT, SPLIT),
                             MODIFY_TARGET_LAYER_TOOLTIP);
        jcb_line_operation.setEnabled(target_layer.isEditable());
        
        final JCheckBox jcb_create_link_layer = dialog
                .addCheckBox(CREATE_LINK_LAYER, create_link_layer, CREATE_LINK_LAYER_TOOLTIP);

        // Listeners
        jcb_line_layer.addActionListener(e -> {
            target_layer = dialog.getLayer(TARGET_LAYER);
            jcb_line_operation.setEnabled(target_layer.isEditable());
        });
        
        // get parameters
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (dialog.wasOKPressed()) {
            point_layer       = dialog.getLayer(POINT_LAYER);
            target_layer      = dialog.getLayer(TARGET_LAYER);
            tolerance         = dialog.getDouble(TOLERANCE);
            //snap              = dialog.getBoolean(SNAP);
            snap_tolerance    = dialog.getDouble(SNAP_TOLERANCE);
            project           = dialog.getBoolean(PROJECT);
            insert            = jcb_line_operation.getSelectedItem().equals(INSERT);
            split             = jcb_line_operation.getSelectedItem().equals(SPLIT);
            nearest_proj_only = dialog.getBoolean(NEAREST_PROJ_ONLY);
            all_proj_within_tolerance = dialog.getBoolean(ALL_PROJ_WITHIN_TOLERANCE);
            create_link_layer = dialog.getBoolean(CREATE_LINK_LAYER);
            return true;
        }
        else return false;
        
    }
    
    public void run(TaskMonitor monitor, PlugInContext context) throws Exception {
        monitor.allowCancellationRequests();
        monitor.report(PROJECT_POINTS_ON_LINES + "...");
        
        FeatureCollection fc_points = point_layer.getFeatureCollectionWrapper();
        if (!validPointFC(context.getWorkbenchFrame(), fc_points)) return;
        FeatureCollection fc_lines = target_layer.getFeatureCollectionWrapper();
        if (!validLineFC(context.getWorkbenchFrame(), fc_lines)) return;
        
        // Create a spatial index containing all the linear components of fc_lines
        STRtree index = new STRtree();
        for (Feature feature : fc_lines.getFeatures()) {
            GeometryWrapper.createWrapper(feature, index);
        }
        
        FeatureSchema fs = fc_points.getFeatureSchema().clone();
        fs.addAttribute(PROJECTED_DISTANCE, AttributeType.DOUBLE);
        FeatureCollection projected_points = new FeatureDataset(fs);
        FeatureCollection projection_links = new FeatureDataset(fs);

        int count = 0;
        int tot = fc_points.size();
        VertexSnapper snapper = new MaxLateralDistanceVertexSnapper(tolerance, snap_tolerance);
        //Set<LinearComponent> modifiedComponents = new HashSet<LinearComponent>();
        // Main loop processing each point feature one after the other and
        Map<Integer,GeometryWrapper> targets = new HashMap<>();
        for (Feature f : fc_points.getFeatures()) {
            if (count++%100==0 || count>=tot) {
                monitor.report(count, tot, POINTS_PROCESSED);
            }
            Envelope env = f.getGeometry().getEnvelopeInternal();
            env.expandBy(tolerance);
            List<GeometryElement> candidates = index.query(env);
            if (nearest_proj_only) {
                Projection proj = GeometryElement.projectSingle(f, snapper, candidates, true);
                if (proj != null) {
                    if (project) addProjectedFeature(proj, fs, projected_points);
                    if (create_link_layer) addLink(proj, fs, projection_links);
                    targets.put(proj.getTargetFeature().getID(), proj.getTargetElement().getGeometryWrapper());
                }
            } else {
                Map<Feature,Projection> map = GeometryElement.projectMultiple(f, snapper, candidates, true);
                for (Projection proj : map.values()) {
                    if (project) addProjectedFeature(proj, fs, projected_points);
                    if (create_link_layer) addLink(proj, fs, projection_links);
                    targets.put(proj.getTargetFeature().getID(), proj.getTargetElement().getGeometryWrapper());
                }
            }
        }
        if (insert) {
            for (GeometryWrapper gw : targets.values()) {
                gw.insert();
            }
        } else if (split) {
            for (GeometryWrapper gw : targets.values()) {
                gw.split();
            }
        }

        if (project) {
            context.getLayerManager().addLayer(StandardCategoryNames.RESULT, 
                point_layer.getName() + " - " + PROJECTED, projected_points);
        }
        if (create_link_layer) {
            context.getLayerManager().addLayer(StandardCategoryNames.RESULT, 
                point_layer.getName() + " - " + LINKS, projection_links);
        }
    
    }

    private void addProjectedFeature(Projection proj, FeatureSchema fs, FeatureCollection fc) {
        Feature feature = copyFeature(proj.getSourceFeature(), fs, false);
        feature.setGeometry(proj.getSourceFeature().getGeometry().getFactory().createPoint(proj.getCoord()));
        feature.setAttribute(PROJECTED_DISTANCE, Math.sqrt(proj.getD2()));
        fc.add(feature);
    }

    private void addLink(Projection proj, FeatureSchema fs, FeatureCollection fc) {
        Feature feature = copyFeature(proj.getSourceFeature(), fs, false);
        feature.setGeometry(proj.getSourceFeature().getGeometry().getFactory().createLineString(
                new Coordinate[]{proj.getSourceFeature().getGeometry().getCoordinate(), proj.getCoord()}));
        feature.setAttribute(PROJECTED_DISTANCE, Math.sqrt(proj.getD2()));
        fc.add(feature);
    }
    
    // Helper method to copy a Feature to a new Feature with a different schema
    private Feature copyFeature(Feature f, FeatureSchema newfs, boolean deep) {
        Feature feature = new BasicFeature(newfs);
        FeatureSchema oldfs = f.getSchema();
        for (int i = 0 ; i < oldfs.getAttributeCount() ; i++) {
            if (i == oldfs.getGeometryIndex() && !deep) continue;
            String attName = oldfs.getAttributeName(i);
            if (newfs.hasAttribute(attName)) {
                feature.setAttribute(attName, f.getAttribute(i));
            }
        }
        return feature;
    }
    
    private boolean validPointFC(Component comp, FeatureCollection points) {
        //boolean valid = false;
        for (Feature f : points.getFeatures()) {
            Geometry g = f.getGeometry();
            if (g instanceof Point) return true;
        }
        JOptionPane.showMessageDialog(comp, NO_POINT_IN_POINT_LAYER);
        return false;
    }
    
    private boolean validLineFC(Component comp, FeatureCollection fc) {
        //boolean valid = true;
        if (fc.size() > 0) return true;
        JOptionPane.showMessageDialog(comp, NO_FEATURE_IN_TARGET_LAYER);
        return false;
    }

}
