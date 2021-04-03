package fr.michaelm.jump.plugin.topology;


import com.vividsolutions.jump.feature.Feature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

/**
 * A vertex snapper specifying the snap policy by a maximum distance
 * between the original coord and the coord it tries to snap to.
 */
public class MaxDistanceVertexSnapper implements VertexSnapper {

    // Square distance
    double d2max = Double.POSITIVE_INFINITY;

    public MaxDistanceVertexSnapper(double distance) {
        assert distance >= 0 : "Distance should be >= 0";
        this.d2max = distance*distance;
    }

    /**
     * {@inheritDoc}
     */
    public Projection snap(Feature source, GeometryElement element) throws IllegalArgumentException {
        if (element instanceof PointElement) {
            return snap(source, (PointElement)element);
        } else if (element instanceof SegmentElement) {
            return snap(source, (SegmentElement)element);
        } else {
            throw new IllegalArgumentException("GeometryElement must be a PointElement or a SegmentElement");
        }
    }

    /**
     * Return a {@link Projection} if source has been projected to the
     * PointElement, and null otherwise.
     * @param source the source feature to be projected
     * @param point a PointElement
     * @return a Projection or null
     */
    protected Projection snap(Feature source, PointElement point) {
        Coordinate c = source.getGeometry().getCoordinate();
        double d2;
        if (c.equals(point.p)) {
            return new Projection(source, point, point.p, d2(c, point.p));
        }
        else if ((d2 = d2(c, point.p)) <= d2max) {
            return new Projection(source, point,point.p, d2);
        }
        else return null;
    }

    /**
     * Return a {@link Projection} if source has been projected to the
     * SegmentElement, and null otherwise.
     * @param source the source feature to be projected
     * @param segment a SegmentElement
     * @return a Projection or null
     */
    protected Projection snap(Feature source, SegmentElement segment) {
        Coordinate c = source.getGeometry().getCoordinate();
        if (c.equals(segment.p0)) {
            return new Projection(source, segment, segment.p0, 0);
        }
        else if (c.equals(segment.p1)) {
            return new Projection(source, segment, segment.p1, 0);
        }
        else {
            Coordinate proj = new LineSegment(segment.p0, segment.p1).closestPoint(c);
            double d2_0 = d2(c, segment.p0);
            double d2_1 = d2(c, segment.p1);
            if (d2_0 <= d2max && d2_0 <= d2_1) {
                return new Projection(source, segment, segment.p0, d2(c, proj));
            }
            else if (d2_1 <= d2max && d2_1 < d2_0) {
                return new Projection(source, segment, segment.p1, d2(c, proj));
            }
            else {
                if (d2(c, proj) <= d2max) return new Projection(source, segment, proj, d2(c, proj));
                else return null;
            }
        }
    }

    protected static double d2(Coordinate c1, Coordinate c2) {
        return (c1.x-c2.x)*(c1.x-c2.x) + (c1.y-c2.y)*(c1.y-c2.y);
    }
}
