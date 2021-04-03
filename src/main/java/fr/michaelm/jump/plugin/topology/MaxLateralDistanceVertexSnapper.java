package fr.michaelm.jump.plugin.topology;


import com.vividsolutions.jump.feature.Feature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

/**
 * A vertex snapper specifying the snap policy by a maximum distance
 * relative to the distance between the vertex and the segment.
 */
public class MaxLateralDistanceVertexSnapper extends MaxDistanceVertexSnapper {

    // Square horizontal distance
    double hd2max = Double.POSITIVE_INFINITY;

    public MaxLateralDistanceVertexSnapper(double distance, double hDistance) {
        super(distance);
        assert hDistance >= 0 : "Horizontal distance should be >= 0";
        this.hd2max = hDistance*hDistance;
    }

    /**
     * {@inheritDoc}
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
            //Coordinate proj = new LineSegment(segment.p0, segment.p1).project(c);
            double orthogonalDist = d2(c, proj);
            if (orthogonalDist <= d2max) {
                double d2p0 = d2(proj, segment.p0);
                double d2p1 = d2(proj, segment.p1);
                if (d2p0 <= hd2max && d2(c, segment.p0) <= d2max && d2p0 <= d2p1) {
                    return new Projection(source, segment, segment.p0, orthogonalDist);
                }
                else if (d2p1 <= hd2max && d2(c, segment.p1) <= d2max && d2p1 < d2p0) {
                    return new Projection(source, segment, segment.p1, orthogonalDist);
                }
                else {
                    double length2 = d2(segment.p0, segment.p1);
                    // proj must be between p0 and p1
                    if (d2p0 <= length2 && d2p1 <= length2) {
                        return new Projection(source, segment, proj, orthogonalDist);
                    } else {
                        return null;
                    }
                }
            }
            else {
                return null;
            }
        }
    }

}

