package fr.michaelm.jump.plugin.topology;

import com.vividsolutions.jump.feature.Feature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;

import java.util.*;

/**
 * The most basic geometry element of a Geometry to deserve the purpose
 * of coordinate projection.
 * A 0-dimensional geometry element is a PointElement (single coordinate)
 * A 1 or 2-dimensional geometry element is a SegmentElement (two coordinates)
 */
public abstract class GeometryElement implements Comparator<Projection> {

    GeometryWrapper wGeometry;
    List<Projection> projections;

    public GeometryElement(GeometryWrapper wGeometry) {
        this.wGeometry = wGeometry;
    }

    /**
     * Projects a Feature point to this GeometryElement.
     * The projection is not automatically added to the projections List
     * as we may want to choose some projections only (projection with
     * the minimum distance for example).
     * @param point Feature to project to this GeometryElement
     * @param snapper snapper specifying the snapping policy
     * @return a Projection from point to this GeometryElement or null is
     * the point is out of the snapper tolerance.
     */
    public Projection project(Feature point, VertexSnapper snapper) {
        return snapper.snap(point, this);
    }

    public Feature getFeature() {
        return wGeometry.getFeature();
    }

    public GeometryWrapper getGeometryWrapper() {
        return wGeometry;
    }

    /**
     * Try to project point to all candidate elements and return the best Projection
     * according to to the snapper policy or null if snapper cannot snap point to any
     * of the candidates.
     * Note that candidate elements may belong to distinct features.
     * @param point the point to project
     * @param snapper the snapper to use to project and snap
     * @param elements candidate GeometryElements
     * @param add if true, the best projection found is added to the target GeometryElement
     * @return the best Projection found or null
     */
    public static Projection projectSingle(Feature point, VertexSnapper snapper,
                                           List<GeometryElement> elements, boolean add) {
        double min2 = Double.POSITIVE_INFINITY;
        Projection bestProjection = null;
        for (GeometryElement element : elements) {
            Projection projection = element.project(point, snapper);
            if (projection == null) continue;
            else if (projection.getD2() < min2) {
                bestProjection = projection;
                min2 = projection.getD2();
            }
        }
        if (add && bestProjection != null) {
            bestProjection.getTargetElement().add(bestProjection);
        }
        return bestProjection;
    }

    /**
     * Try to project point to all candidate elements and return the best Projection
     * for each feature according to to the snapper policy.
     * Note that elements may belong to several distinct features. In this case, the
     * returned map keep only the best projection for each distinct Feature.
     * @param point the point to project
     * @param snapper the snapper to use to project and snap
     * @param elements candidate GeometryElements
     * @param add if true, the best projections found for each Feature are added to
     *            the target GeometryElement
     * @return a Map containing a maximum of one Projection per Feature, choosen
     * according to the snapper policy.
     */
    public static Map<Feature,Projection> projectMultiple(Feature point, VertexSnapper snapper,
                                                          List<GeometryElement> elements, boolean add) {
        Map<Feature,Projection> projections = new HashMap<>();
        for (GeometryElement element : elements) {
            Projection projection = element.project(point, snapper);
            if (projection == null) continue;
            else {
                Feature target = projection.getTargetFeature();
                Projection bestProj = projections.get(target);
                if (bestProj == null) {
                    projections.put(target, projection);
                } else {
                    double d2 = bestProj.getD2();
                    if (projection.getD2() < d2) {
                        projections.put(target, projection);
                    }
                }
            }
        }
        if (add && projections.size() > 0) {
            for (Projection proj : projections.values()) {
                proj.getTargetElement().add(proj);
            }
        }
        return projections;
    }

    /**
     * Adds a {@link Projection} in a safe way (initialize projections
     * if it is not already initialized, and do nothing if projection
     * is null;
     * @param projection Projection to be added (may be null)
     * @return true if the projection has been added
     */
    public boolean add(Projection projection) {
        if (projection != null) {
            if (projections == null) {
                projections = new ArrayList<>(1);
            }
            projections.add(projection);
            return true;
        } else {
            return false;
        }
    }

    abstract public void insert(List<CoordinateList> list);

    abstract public void split(List<CoordinateList> list);

    static double d2(Coordinate c1, Coordinate c2) {
        return (c1.x-c2.x)*(c1.x-c2.x) + (c1.y-c2.y)*(c1.y-c2.y);
    }

}
