/*
 * Library name : fr.michaelm.jump.plugin.topology
 * (C) 2011 Michaël Michaud
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


import com.vividsolutions.jump.feature.*;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.workbench.model.Layer;
import com.vividsolutions.jump.workbench.model.StandardCategoryNames;
import com.vividsolutions.jump.workbench.plugin.MultiEnableCheck;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.ThreadedBasePlugIn;
import com.vividsolutions.jump.workbench.ui.AttributeTypeFilter;
import com.vividsolutions.jump.workbench.ui.GUIUtil;
import com.vividsolutions.jump.workbench.ui.MenuNames;
import com.vividsolutions.jump.workbench.ui.MultiTabInputDialog;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.distance.GeometryLocation;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * PlugIn to clean linear features layer(s) with node mismatches, undershoots
 * and overshoots. 
 * @author Michael Michaud
 * @version 0.4.0 (2012-05-20)
 */
// History
// 0.4.0 (2012-05-20) fix a bug in node degree computation
//                    add an option checking attribute equality before snapping
// 0.3.4 (2012-05-17) nodes inserted in wrong segment in reference layer 
//                            clean code (remove comments and system.out)
// 0.3.3 (2012-11-23) compiled with java 5 
// 0.3.2 (2011-11-08)
public class NetworkTopologyCleaningPlugIn extends ThreadedBasePlugIn {
    
    private static String TOPOLOGY;
    private static String NETWORK_TOPOLOGY_CLEANING;
    
    private static String REFERENCE_LAYER;
    private static String REFERENCE_LAYER_TOOLTIP;
    
    private static String LAYER_TO_SNAP;
    private static String LAYER_TO_SNAP_TOOLTIP;
    
    private static String MISMATCHES;
    private static String FIXED;
    
    private static String TOLERANCE;
    private static String TOLERANCE_TOOLTIP;
    
    private static String ADVANCED_OPTIONS;
    private static String DEFAULT_ADVANCED_OPTIONS;

    private static String NODE3_TOL;
    private static String NODE3_TOL_TOOLTIP;
    private static String ANGLE_TOL;
    private static String ANGLE_TOL_TOOLTIP;
    
    private static String DETECTION;
    private static String DETECTION_TOOLTIP;
    
    private static String CORRECTION;
    private static String CORRECTION_TOOLTIP;
    
    private static String REFERENCE_EDITABLE;
    private static String REFERENCE_EDITABLE_TOOLTIP;
    
    private static String SNAP_MODE;
    private static String SNAP_TO_NODE;
    private static String SNAP_TO_VERTEX;
    private static String SNAP_TO_SEGMENT;
    private static String NOT_SNAPPED;
    private static String ROTATION;
    
    private static String ATTRIBUTE_EQUALITY;
    private static String ATTRIBUTE_EQUALITY_TOOLTIP;
    private static String REFERENCE_ATTRIBUTE;
    private static String SNAPPING_ATTRIBUTE;
    
    Layer reference_layer;
    Layer layer_to_snap;
    double tolerance = 10.0;
    double node3_tol = 2.0;
    double angle_tol = 15.0;
    double angle_tol_rad = 15.0*Math.PI/180.0;
    boolean detection, correction, reference_editable;
    
    boolean attribute_equality = false;
    String reference_attribute;
    String snapping_attribute;
    
    GeometryFactory gf = new GeometryFactory();
    
    public String getName() {return "Network topology cleaning PlugIn";}
    
    public void initialize(final PlugInContext context) throws Exception {
        
        TOPOLOGY                   = I18NPlug.getI18N("Topology");
        NETWORK_TOPOLOGY_CLEANING  = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.network-topology-cleaning");
                                   
        REFERENCE_LAYER            = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.reference-layer");
        REFERENCE_LAYER_TOOLTIP    = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.reference-layer-tooltip");
        LAYER_TO_SNAP              = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.layer-to-snap");
        LAYER_TO_SNAP_TOOLTIP      = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.layer-to-snap-tooltip");
        MISMATCHES                 = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.mismatches");
        FIXED                      = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.fixed");
                                   
        TOLERANCE                  = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.tolerance");
        TOLERANCE_TOOLTIP          = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.tolerance-tooltip");
                                   
        ADVANCED_OPTIONS           = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.advanced-options");
        DEFAULT_ADVANCED_OPTIONS   = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.default-advanced-options");
        
        NODE3_TOL                  = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.node3-tol");
        NODE3_TOL_TOOLTIP          = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.node3-tol-tooltip");

        ANGLE_TOL                  = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.angle-tol");
        ANGLE_TOL_TOOLTIP          = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.angle-tol-tooltip");
                                   
        DETECTION                  = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.detection");
        DETECTION_TOOLTIP          = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.detection-tooltip");
        CORRECTION                 = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.layer1-nodes-moveable");
        CORRECTION_TOOLTIP         = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.layer1-nodes-moveable-tooltip");
        REFERENCE_EDITABLE         = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.layer2-geom-editable");
        REFERENCE_EDITABLE_TOOLTIP = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.layer2-geom-editable-tooltip");
        
        SNAP_MODE                  = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.snap-mode");
        SNAP_TO_NODE               = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.snap-to-node");
        SNAP_TO_VERTEX             = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.snap-to-vertex");
        SNAP_TO_SEGMENT            = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.snap-to-segment");
        NOT_SNAPPED                = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.not-snapped");
        ROTATION                   = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.rotation");
        
        ATTRIBUTE_EQUALITY         = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.attribute-equality-option");
        ATTRIBUTE_EQUALITY_TOOLTIP = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.attribute-equality-tooltip");
        REFERENCE_ATTRIBUTE        = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.reference-layer-attribute");
        SNAPPING_ATTRIBUTE         = I18NPlug.getI18N("NetworkTopologyCleaningPlugIn.snapping-layer-attribute");
        
        context.getFeatureInstaller().addMainMenuPlugin(
          this, new String[]{MenuNames.PLUGINS, TOPOLOGY},
          NETWORK_TOPOLOGY_CLEANING + "...",
          false, null, new MultiEnableCheck()
          .add(context.getCheckFactory().createTaskWindowMustBeActiveCheck())
          .add(context.getCheckFactory().createAtLeastNLayersMustExistCheck(1)));
    }
    
    public boolean execute(PlugInContext context) {
        
        final MultiTabInputDialog dialog = new MultiTabInputDialog(
        context.getWorkbenchFrame(), NETWORK_TOPOLOGY_CLEANING, NETWORK_TOPOLOGY_CLEANING, true);
        
        if (reference_layer == null || !context.getLayerManager().getLayers().contains(reference_layer)) {
            reference_layer = context.getCandidateLayer(0);
        }
        final JComboBox jcb_reference_layer = dialog.addLayerComboBox(
            REFERENCE_LAYER, reference_layer, null, context.getLayerManager());
        
        if (layer_to_snap == null || !context.getLayerManager().getLayers().contains(layer_to_snap)) {
            int number_of_layers = context.getLayerManager().getLayers().size();
            if (number_of_layers > 1) layer_to_snap = context.getCandidateLayer(1);
            else layer_to_snap = context.getCandidateLayer(0);
        }
        final JComboBox jcb_layer_to_snap = dialog.addLayerComboBox(
            LAYER_TO_SNAP, layer_to_snap, null, context.getLayerManager());
        
        final JTextField jtf_tolerance = dialog.addDoubleField(TOLERANCE, tolerance, 12, TOLERANCE_TOOLTIP);
        
        dialog.addSeparator();
        
        final JCheckBox jcb_detection = dialog.addCheckBox(DETECTION,  true, DETECTION_TOOLTIP);
        final JCheckBox jcb_correction = dialog.addCheckBox(CORRECTION,  correction,  CORRECTION_TOOLTIP);
        final JCheckBox jcb_reference_editable = dialog.addCheckBox(REFERENCE_EDITABLE,  false, REFERENCE_EDITABLE_TOOLTIP);
        jcb_reference_editable.setEnabled(reference_layer.isEditable());
        
        dialog.addPane(ADVANCED_OPTIONS);
        final JButton jb_default_options     = dialog.addButton(DEFAULT_ADVANCED_OPTIONS);
        final JTextField jtf_node3_tol       = dialog.addDoubleField(NODE3_TOL, node3_tol, 12, NODE3_TOL_TOOLTIP);
        final JTextField jtf_angle_tol       = dialog.addDoubleField(ANGLE_TOL, angle_tol, 12, ANGLE_TOL_TOOLTIP);
        
        dialog.addSeparator();
        final JCheckBox jcb_attribute_equality = dialog.addCheckBox(ATTRIBUTE_EQUALITY,  attribute_equality, ATTRIBUTE_EQUALITY_TOOLTIP);
        final JComboBox jcb_reference_attribute = dialog.addAttributeComboBox(REFERENCE_ATTRIBUTE, REFERENCE_LAYER, AttributeTypeFilter.NO_GEOMETRY_FILTER, null);
        final JComboBox jcb_snapping_attribute = dialog.addAttributeComboBox(SNAPPING_ATTRIBUTE, LAYER_TO_SNAP, AttributeTypeFilter.NO_GEOMETRY_FILTER, null);
        boolean attributes_available = 
            reference_layer.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()>1
            &&
            layer_to_snap.getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()>1;
        jcb_attribute_equality.setEnabled(attributes_available);
        jcb_reference_attribute.setEnabled(attributes_available);
        jcb_snapping_attribute.setEnabled(attributes_available);
        //if (!attributes_available) jcb_attribute_equality.setSelected(false);
        
        // Set default advanced options if jb_default_options is pressed
        jb_default_options.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jtf_node3_tol.setText(""+dialog.getDouble(TOLERANCE)/5.0);
                jtf_angle_tol.setText(""+15.0);
                
            }
        });
        
        // changing reference layer may change options availability
        jcb_reference_layer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                reference_layer = dialog.getLayer(REFERENCE_LAYER);
                if (!reference_layer.isEditable()) jcb_reference_editable.setSelected(false);
                jcb_reference_editable.setEnabled(reference_layer.isEditable());
                boolean attributes_available = 
                    dialog.getLayer(REFERENCE_LAYER).getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()>1 &&
                    dialog.getLayer(LAYER_TO_SNAP).getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()>1;
                jcb_attribute_equality.setEnabled(attributes_available);
                jcb_reference_attribute.setEnabled(attributes_available);
                jcb_snapping_attribute.setEnabled(attributes_available);
                jcb_attribute_equality.setSelected(false);
            }
        });
        
        // changing layer_to_snap may change options availability
        jcb_layer_to_snap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                boolean attributes_available = 
                    dialog.getLayer(REFERENCE_LAYER).getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()>1 &&
                    dialog.getLayer(LAYER_TO_SNAP).getFeatureCollectionWrapper().getFeatureSchema().getAttributeCount()>1;
                jcb_attribute_equality.setEnabled(attributes_available);
                jcb_reference_attribute.setEnabled(attributes_available);
                jcb_snapping_attribute.setEnabled(attributes_available);
                jcb_attribute_equality.setSelected(false);
            }
        });
        
        GUIUtil.centreOnWindow(dialog);
        dialog.setVisible(true);
        if (dialog.wasOKPressed()) {
            reference_layer     = dialog.getLayer(REFERENCE_LAYER);
            layer_to_snap       = dialog.getLayer(LAYER_TO_SNAP);
            tolerance           = dialog.getDouble(TOLERANCE);
            node3_tol           = dialog.getDouble(NODE3_TOL);
            angle_tol           = dialog.getDouble(ANGLE_TOL);
            angle_tol_rad       = dialog.getDouble(ANGLE_TOL)*Math.PI/180.0;
            detection           = dialog.getBoolean(DETECTION);
            correction          = dialog.getBoolean(CORRECTION);
            reference_editable  = dialog.getBoolean(REFERENCE_EDITABLE);
            attribute_equality  = dialog.getBoolean(ATTRIBUTE_EQUALITY);
            reference_attribute = dialog.getText(REFERENCE_ATTRIBUTE);
            snapping_attribute  = dialog.getText(SNAPPING_ATTRIBUTE);
            return true;
        }
        else return false;
        
    }
    
    public void run(TaskMonitor monitor, PlugInContext context) {
        monitor.allowCancellationRequests();
        monitor.report(NETWORK_TOPOLOGY_CLEANING + "...");
        
        FeatureCollection fc_ref = reference_layer.getFeatureCollectionWrapper();
        FeatureCollection fc = layer_to_snap.getFeatureCollectionWrapper();
        
        // Layer showing correction vectors
        FeatureSchema fs = new FeatureSchema();
        fs.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        fs.addAttribute(SNAP_MODE, AttributeType.STRING);
        fs.addAttribute(ROTATION, AttributeType.DOUBLE);
        FeatureCollection displacements = new FeatureDataset(fs);
        
        // New layer with snapped features
        FeatureSchema result_fs = fc.getFeatureSchema().clone();
        FeatureCollection result = new FeatureDataset(result_fs);
        for (Feature feature : fc.getFeatures()) {
            result.add((feature).clone(true));
        }
        
        // Indexed reference feature collection
        if (reference_layer == layer_to_snap) fc_ref = result;
        IndexedFeatureCollection ifc_ref = new IndexedFeatureCollection(fc_ref, new STRtree());
        
        // Indexed feature collection to process
        IndexedFeatureCollection ifc = reference_layer == layer_to_snap ?
            ifc_ref :
            new IndexedFeatureCollection(fc, new STRtree());
        
        // List of nodes which are not snapped on a reference vertex
        List<Node> nodes_to_snap = new ArrayList<>();
        for (Feature feature : result.getFeatures()) {
            Coordinate[] cc = (feature).getGeometry().getCoordinates();
            Node node_ini = new Node(cc[0], 0, feature);
            if (!node_ini.isSnapped(ifc_ref)) nodes_to_snap.add(node_ini);
            Node node_end = new Node(cc[cc.length-1], cc.length-1, feature);
            if (!node_end.isSnapped(ifc_ref)) nodes_to_snap.add(node_end);
        }
        
        // [mmichaud 2012-05-17] process nodes and add insertion points as they 
        // are found, otherwise, segment indexes in GeometryLocation are wrong
        for (Node node : nodes_to_snap) {
            if (node.findFeatureToSnapTo(tolerance, ifc_ref, attribute_equality, reference_attribute, snapping_attribute)) {
                node.computeDegree(ifc);
                int degree = node.getDegree();
                double tol = degree>2 ? node3_tol : tolerance;
                boolean snap = node.snapToNode(tol, angle_tol_rad);
                if (!snap) snap = node.snapToSegment(tol, angle_tol_rad);
                if (reference_editable && node.getSnapMode()==SNAP_TO_SEGMENT) {
                    node.insertCoordinateInReference();
                }
                // Compute the vector representing the node displacement
                BasicFeature bf = new BasicFeature(fs);
                bf.setGeometry(node.getFeature().getGeometry().getFactory().createLineString(
                    new Coordinate[]{node.getLocations()[0].getCoordinate(),
                                     node.getLocations()[1].getCoordinate()}));
                bf.setAttribute(SNAP_MODE, node.getSnapMode());
                bf.setAttribute(ROTATION, Math.rint(node.getRotation()*1800.0/Math.PI)/10.0);
                displacements.add(bf);
            }
        }
        
        context.getLayerManager().addCategory(StandardCategoryNames.RESULT);
        if (displacements.size()>0) {
            if (detection) {
                Layer lyr = context.addLayer(StandardCategoryNames.RESULT, MISMATCHES, displacements);
                lyr.addStyle(new com.vividsolutions.jump.workbench.ui.renderer.style.ArrowLineStringEndpointStyle.SolidEnd());
            }
            if (correction) {
                context.addLayer(StandardCategoryNames.RESULT, layer_to_snap.getName() + "-" + FIXED, result);
            }
        }
    }
        
    
   /**
    * Internal class representing a Node with a reference to its Feature
    * and a reference to the nearest features.
    */
    private static class Node {
        // Coordinate of this node
        Coordinate coord;
        // Index of this node in its geometry
        int index;
        // Feature this node comes from
        Feature feature;
        // degree of this node (computed from a FeatureCollection context)
        int degree = 0;
        // Nearest feature this node could snap to 
        Feature snappedFeature;
        // GeometryLocation of the node and
        // GeometryLocation of the nearest coordinate on the snapped feature
        GeometryLocation[] locations;
        // How this node has snapped feature
        String snap_mode;
        // Rotation of the segment ending with this node between its orientation
        // before the snap and after the snap
        double rotation; // in radians
        
        // Create a node
        public Node(Coordinate coord, int index, Feature feature) {
            this.coord = coord;
            this.index = index;
            this.feature = feature;
        }
        
        public Coordinate getCoordinate() {return coord;}
        
        public int getIndex() {return index;}
        
        public int getDegree() {return degree;}
        
        public Feature getFeature() {return feature;}
        
        public Feature getSnappedFeature() {return snappedFeature;}
        
        public GeometryLocation[] getLocations() {return locations;}
        
        public boolean isSnapped() {return snappedFeature!=null;}
        
        public String getSnapMode() {return snap_mode;}
        
        public double getRotation() {return rotation;}
        
        // Returns degree of the node for this feature collection
        public int computeDegree(FeatureCollection fc) {
            degree = 0;
            List<Feature> candidates = fc.query(new Envelope(coord));
            for (Feature feature : candidates) {
                Geometry geom = feature.getGeometry();
                if (!(geom instanceof LineString)) continue;
                Coordinate[] cc = geom.getCoordinates();
                Coordinate ini = cc[0];
                Coordinate end = cc[cc.length-1];
                if (coord.equals(ini)) degree++;
                if (coord.equals(end)) degree++;
            }
            return degree;
        }
        
        // Returns true if this node is already snapped on a feature of fc 
        public boolean isSnapped(FeatureCollection fc) {
            List<Feature> candidates = fc.query(new Envelope(coord));
            for (Feature feature : candidates) {
                Geometry geom = feature.getGeometry();
                // if this node comes from fc, you don't want to snapped the
                // node on the feature it comes from : skip it 
                if (geom.equals(feature.getGeometry())) continue;
                for (Coordinate c : geom.getCoordinates()) {
                    if (coord.equals(c)) return true;
                }
            }
            return false;
        }
        
        // Find the feature to snap to and the "locations" (nearest points)
        public boolean findFeatureToSnapTo(double tol, FeatureCollection fc,
                boolean useAtt, String refAtt, String snapAtt) {
            Envelope env = new Envelope(coord);
            env.expandBy(tol);
            List<Feature> candidates = fc.query(env);
            double dmin = tol;
            for (Feature f : candidates) {
                if (f.getGeometry().getDimension() != 1) continue;
                if (feature.getGeometry().equals(f.getGeometry())) continue;
                if (useAtt) {
                    Object refVal = f.getAttribute(refAtt);
                    Object snapVal = feature.getAttribute(snapAtt);
                    if (refVal == null && snapVal != null) continue;
                    if (refVal != null && !refVal.equals(snapVal)) continue;
                }
                Point p = feature.getGeometry().getFactory().createPoint(coord);
                DistanceOp dop = new DistanceOp(p, f.getGeometry());
                double dist = dop.distance();
                if (dist > dmin) continue;
                if (dist <= tol && dist <= dmin) {
                    snappedFeature = f;
                    locations = dop.nearestLocations();
                    dmin = dist;
                }
            }
            return snappedFeature != null;
        }
        
        // Try to snap to one of the candidate feature end point.
        // If one of the candidate feature endpoint is within the tolerance
        // we want to snap to this node instead of to the nearest vertex.
        public boolean snapToNode(double tol, double angleTol) {
            Coordinate snapCoord;
            
            // Compute distances
            Coordinate[] cc = snappedFeature.getGeometry().getCoordinates();
            double d_ini = coord.distance(cc[0]);
            double d_fin = coord.distance(cc[cc.length-1]);
            // case 1, end nodes are too far
            if (d_ini > tol && d_fin > tol) {
                snap_mode = NOT_SNAPPED + " (D > " + tol + ")";
                return false;
            }
            
            // Compute angles
            Coordinate coordBeforeNode = index==0 ?
                feature.getGeometry().getCoordinates()[1] :
                feature.getGeometry().getCoordinates()[feature.getGeometry().getCoordinates().length-2] ;
            double a_ini = Angle.angleBetween(cc[0], coordBeforeNode, coord);
            double a_fin = Angle.angleBetween(cc[cc.length-1], coordBeforeNode, coord);
            
            // if distance to one of tehe node is > tol
            if (d_ini <= tol && d_fin > tol) {
                snapCoord = cc[0];
                rotation = a_ini;
            }
            else if (d_fin <= tol && d_ini > tol) {
                snapCoord = cc[cc.length-1];
                    rotation = a_fin;
            }
            
            // For nodes of degree 1, use angle tolerance
            else if (degree == 1) {
                if (d_ini<=d_fin && a_ini<=angleTol) {
                    snapCoord = cc[0];
                    rotation = a_ini;
                }
                else if (d_fin<=d_ini && a_fin<=angleTol) {
                    snapCoord = cc[cc.length-1];
                    rotation = a_fin;
                }
                else if (d_ini<=tol && a_ini<=angleTol) {
                    snapCoord = cc[0];
                    rotation = a_ini;
                }
                else if (d_fin<=tol && a_fin<=angleTol) {
                    snapCoord = cc[cc.length-1];
                    rotation = a_fin;
                }
                else {
                    if (d_ini>tol && d_fin>tol) snap_mode = NOT_SNAPPED + " (D > " + tol + ")";
                    else if (a_ini>angleTol && a_fin>angleTol) snap_mode = NOT_SNAPPED + " (A > " + angleTol + ")";
                    else snap_mode = NOT_SNAPPED;
                    return false;
                }
            }
            // For nodes of degree > 1, snap to the nearest node, even if angle 
            // is larger than tolerance
            else {
                if (d_ini<=d_fin) {
                    snapCoord = cc[0];
                    rotation = a_ini;
                }
                else {
                    snapCoord = cc[cc.length-1];
                    rotation = a_fin;
                }
            }
            // Compute new geometry
            CoordinateList coordlist = new CoordinateList(feature.getGeometry().getCoordinates());
            coordlist.set(index, snapCoord);
            Geometry newGeom = feature.getGeometry().getFactory().createLineString(coordlist.toCoordinateArray());
            // case 2a : snap creates an intersection between two features
            if (newGeom.crosses(snappedFeature.getGeometry())) {
                snap_mode = NOT_SNAPPED + " (crossing geometries)";
                return false;
            }
            // case 2b : features can be snapped properly
            locations[1] = new GeometryLocation(snappedFeature.getGeometry(), snapCoord);
            newGeom.geometryChanged();
            feature.setGeometry(newGeom);
            snap_mode = SNAP_TO_NODE;
            return true;
        }
        
        public boolean snapToSegment(double tol, double angleTol) {
            Coordinate snapCoord;
            Coordinate[] cc = snappedFeature.getGeometry().getCoordinates();
            // Computes distances with segment endpoints and segment nearest point
            Coordinate c_befor =  cc[locations[1].getSegmentIndex()];
            Coordinate c_ortho =  locations[1].getCoordinate();
            Coordinate c_after =  cc[locations[1].getSegmentIndex()+1];
            double d_befor = coord.distance(c_befor);
            double d_ortho = coord.distance(c_ortho);
            double d_after = coord.distance(c_after);
            
            // Computes angles
            Coordinate coordBeforeNode = index==0 ?
                feature.getGeometry().getCoordinates()[1] :
                feature.getGeometry().getCoordinates()[feature.getGeometry().getCoordinates().length-2] ;
            double a_befor = Angle.angleBetween(c_befor, coordBeforeNode, coord);
            double a_ortho = Angle.angleBetween(c_ortho, coordBeforeNode, coord);
            double a_after = Angle.angleBetween(c_after, coordBeforeNode, coord);
            
            // for degree 1 nodes, use angle tolerance to choose the best snap point
            if (degree == 1) {
                // c_after is out of tolerance, choose c_befor
                //System.out.println("degree = 1");
                if (d_befor<=tol && a_befor<=angleTol &&
                    (d_after>tol || a_after>angleTol)) {
                    snapCoord = c_befor;
                    rotation = a_befor;
                }
                // c_befor is out of tolerance, choose c_after
                else if (d_after<=tol && a_after<=angleTol &&
                         (d_befor>tol || a_befor>angleTol)) {
                    //System.out.println("d_after<=tol && a_after<=angleTol && (d_befor>tol || a_befor>angleTol");
                    snapCoord = c_after;
                    rotation = a_after;
                }
                // c_befor and c_after are in the tolerance, choose the best
                else if (d_after<=tol && a_after<=angleTol &&
                         d_befor<=tol && a_befor<=angleTol) {
                    //System.out.println("d_after<=tol && a_after<=angleTol && d_befor<=tol && a_befor<=angleTol");
                    if (a_befor<=a_after) {
                        //System.out.println("a_befor<=a_after");
                        snapCoord = c_befor;
                        rotation = a_befor;
                    }
                    else {
                        //System.out.println("else");
                        snapCoord = c_after;
                        rotation = a_after;
                    }
                }
                // c_befor and c_after are out of tolerance
                else {
                    if (d_ortho<=tol && a_ortho<=angleTol) {
                        //System.out.println("else : d_ortho<=tol && a_ortho<=angleTol");
                        snapCoord = c_ortho;
                        rotation = a_ortho;
                    }
                    else {
                        //System.out.println("else : else");
                        if (d_ortho>tol) snap_mode = NOT_SNAPPED + " (D > "+tol+")";
                        else if (a_ortho>angleTol) snap_mode = NOT_SNAPPED +
                            " (A > " + Math.rint(angleTol*1800.0/Math.PI)/10.0 + ")";
                        else snap_mode = NOT_SNAPPED;
                        return false;
                    }
                }
            }
            // For degree = 2+ nodes, do no use angle tolerance
            else if (d_befor<=tol && d_after<=tol) {
                //System.out.println("degree 2+ : d_befor<=tol && d_after<=tol");
                if (d_befor<=d_after) {
                    snapCoord = c_befor;
                    rotation = a_befor;
                }
                else {
                    snapCoord = c_after;
                    rotation = a_after;
                }
            }
            else if (d_befor<=tol) {
                snapCoord = c_befor;
                rotation = a_befor;
            }
            else if (d_after<=tol) {
                snapCoord = c_after;
                rotation = a_after;
            }
            else if (d_ortho<=tol) {
                snapCoord = c_ortho;
                rotation = a_ortho;
            }
            else {
                if (d_ortho>tol) snap_mode = NOT_SNAPPED + " (D > "+tol+")";
                else if (a_ortho>angleTol) snap_mode = NOT_SNAPPED +
                    " (A > " + Math.rint(angleTol*1800.0/Math.PI)/10.0 + ")";
                else snap_mode = NOT_SNAPPED;
                return false;
            }
            
            CoordinateList coordlist = new CoordinateList(feature.getGeometry().getCoordinates());
            coordlist.set(index, snapCoord);
            Geometry newGeom = feature.getGeometry().getFactory().createLineString(coordlist.toCoordinateArray());
            newGeom.geometryChanged();

            int newindex = snapCoord == c_befor?
                           locations[1].getSegmentIndex():
                           locations[1].getSegmentIndex()+1;
            locations[1] = new GeometryLocation(snappedFeature.getGeometry(), newindex, snapCoord);
            feature.setGeometry(newGeom);
            if (snapCoord == c_ortho) snap_mode = SNAP_TO_SEGMENT;
            else snap_mode = SNAP_TO_VERTEX;
            return true;
        }
        
        public void insertCoordinateInReference() {
            CoordinateList coordlist = new CoordinateList(snappedFeature.getGeometry().getCoordinates());
            // [mmichaud 2012-05-17] fix a severe bug about segment index
            coordlist.add(locations[1].getSegmentIndex(), locations[1].getCoordinate(), false);
            Geometry newGeom = feature.getGeometry().getFactory().createLineString(coordlist.toCoordinateArray());
            newGeom.geometryChanged();
            snappedFeature.setGeometry(newGeom);
            Point p = feature.getGeometry().getFactory().createPoint(coord);
            DistanceOp dop = new DistanceOp(p, snappedFeature.getGeometry());
            locations = dop.nearestLocations();
        }
        
    }

}
