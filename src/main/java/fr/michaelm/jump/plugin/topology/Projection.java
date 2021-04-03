package fr.michaelm.jump.plugin.topology;

import com.vividsolutions.jump.feature.Feature;
import org.locationtech.jts.geom.Coordinate;

/**
 * A projection from a feature to another feature at a location
 * materialized by a coordinate.
 */
public class Projection implements Comparable<Projection> {

    private final Feature sourceFeature;
    private final GeometryElement targetElement;
    private final Coordinate coord;
    // d2 is the minimum distance between source and the elementit has been projected to
    double d2;

    public Projection(Feature sourceFeature, GeometryElement targetElement, Coordinate coord, double d2) {
        this.sourceFeature = sourceFeature;
        this.targetElement = targetElement;
        this.coord = coord;
        this.d2 = d2;
    }

    public Feature getSourceFeature() {
        return sourceFeature;
    }

    public GeometryElement getTargetElement() {
        return targetElement;
    }

    public Feature getTargetFeature() {
        return targetElement.getFeature();
    }

    public Coordinate getCoord() {
        return coord;
    }

    public double getD2() {
        return d2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        else if (o instanceof Projection) {
            Projection other = (Projection)o;
            return sourceFeature.getID() == other.getSourceFeature().getID() &&
                    getTargetFeature().getID() == other.getTargetFeature().getID() &&
                    coord.equals(other.getCoord());
        }
        else return false;
    }

    @Override
    public int hashCode() {
        return coord.hashCode() + getSourceFeature().getID();
    }

    /**
     * Compare two projections according to the following criteria :
     * if distance of this < distance of o, this is lesser than o
     * if distance are equal and this.featureId is less than o.featureId, this is lesser than o
     * if both distance and feature ID are equal,
     * @param o
     * @return -1 if this is less than o, 0 if this is more than o and 0 if this and o
     * represent a projection to a same coordinate on a same feature.
     */
    @Override
    public int compareTo(Projection o) {
        if (d2 < o.getD2()) return -1;
        else if (d2 > o.getD2()) return 1;
        else {
            int featureComparator = Integer.compare(getTargetFeature().getID(),
                    o.getTargetFeature().getID());
            if (featureComparator < 0) return -1;
            else if (featureComparator > 0) return 1;
            else {
                featureComparator = Integer.compare(sourceFeature.getID(), o.getSourceFeature().getID());
                if (featureComparator < 0) return -1;
                else if (featureComparator > 0) return 1;
                else {
                    return coord.compareTo(o.getCoord());
                }
            }
        }
    }

    public String toString() {
        return sourceFeature.getGeometry().getGeometryType() + "-" + sourceFeature.getID() +
                " to " + getTargetFeature().getGeometry().getGeometryType() + "-" + getTargetFeature().getID() +
                " at " + coord + " (" + Math.sqrt(d2) + ")";
    }

}
