package fr.michaelm.jump.plugin.topology;


import com.vividsolutions.jump.feature.Feature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * Created by Michaël on 19/05/14.
 */
public class TargetGeometry {

    Feature feature;
    Geometry geometry;
    GeometryWrapper wgeometry;
    double maxDistance = Double.POSITIVE_INFINITY;
    VertexSnapper snapper;


    public TargetGeometry(Feature feature, double maxDistance, VertexSnapper snapper, STRtree index) {
        this.feature = feature;
        this.geometry = feature.getGeometry();
        this.wgeometry = GeometryWrapper.createWrapper(feature, index);
        this.maxDistance = maxDistance;
        this.snapper = snapper;
    }

}
